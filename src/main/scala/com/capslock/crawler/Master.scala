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

    var router = {
        val routees = Vector.fill(Runtime.getRuntime.availableProcessors() * 4 + 1) {
            val r = context.actorOf(Worker.props(materializer))
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
                context.actorSelection(RootActorPath(member.address) / "user" / CrawlerScheduler.name(topic)) ! MasterRegistration
            }
        case task: Task =>
            router.route(task, sender())
        case Terminated(a) =>
            router.removeRoutee(a)
            val r = context.actorOf(Worker.props(materializer))
            context watch r
            router = router.addRoutee(r)
    }
}

object Master {
    def props(topic: String)(implicit materializer: Materializer): Props =
        Props(classOf[Master], topic, materializer)

    def name(topic: String) = s"master-$topic"
}

