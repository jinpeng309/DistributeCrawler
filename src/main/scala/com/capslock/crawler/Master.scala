package com.capslock.crawler

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.routing.{ActorRefRoutee, Router, SmallestMailboxRoutingLogic}
import akka.stream.Materializer

/**
 * Created by capslock1874.
 */
class Master(topic: String)(implicit materializer: Materializer) extends Actor with ActorLogging {
    val cluster = Cluster(context.system)
    var workingCount = 0
    val maxBufferSize = (Runtime.getRuntime.availableProcessors() * 4 + 1) * 5
    var waitingTasks = List[Task]()
    var schedulers = List[ActorSelection]()
    var router = {
        val routees = Vector.fill(Runtime.getRuntime.availableProcessors() * 4 + 1) {
            val r = context.actorOf(Worker.props(self)(materializer))
            context watch r
            ActorRefRoutee(r)
        }
        Router(SmallestMailboxRoutingLogic(), routees)
    }

    override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

    override def postStop(): Unit = cluster.unsubscribe(self)

    override def receive: Receive = {
        case MemberUp(member) =>
            if (member.hasRole("scheduler")) {
                val scheduler = context.actorSelection(RootActorPath(member.address) / "user" / CrawlerScheduler.name(topic))
                scheduler ! MasterRegistration
                schedulers = scheduler :: schedulers
            }
        case task: Task =>
            if (workingCount <= maxBufferSize) {
                workingCount += 1
                router.route(task, sender())
            } else {
                workingCount += 1
                waitingTasks = waitingTasks ::: List(task)
                sender() ! StopPushTaskFeedback
            }

        case TaskComplete =>
            workingCount -= 1
            if (waitingTasks.nonEmpty) {
                sender() ! waitingTasks.head
                waitingTasks = waitingTasks.tail
                workingCount += 1
            }
            if (workingCount < maxBufferSize) {
                schedulers.foreach(scheduler => scheduler ! PullTask)
            }
        case Terminated(a) =>
            router.removeRoutee(a)
            val r = context.actorOf(Worker.props(self)(materializer))
            context watch r
            router = router.addRoutee(r)
    }
}

object Master {
    def props(topic: String)(implicit materializer: Materializer): Props =
        Props(classOf[Master], topic, materializer)

    def name(topic: String) = s"master-$topic"
}

