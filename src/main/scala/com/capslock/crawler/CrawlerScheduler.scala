package com.capslock.crawler

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.stream.Materializer

/**
 * Created by capslock1874.
 */

object CrawlerScheduler {
    def props(topic: String, initUrl: String)(implicit materializer: Materializer): Props =
        Props(classOf[CrawlerScheduler], topic, initUrl, materializer)

    def name(topic: String) = s"crawler-$topic"
}

class CrawlerScheduler(topic: String, initUrl: String)(implicit materializer: Materializer) extends Actor with ActorLogging {
    val cluster = Cluster(context.system)
    var tasks = List[Task](Task(topic, initUrl))
    var nodes: List[ActorRef] = List()

    override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

    override def postStop(): Unit = cluster.unsubscribe(self)

    override def receive: Receive = {
        case TaskResult(Some(nextUrl)) =>
            log.info(s"next url $nextUrl")
            dispatchTask(Task(topic, "https://www.zhihu.com/people/excited-vczh"))
        case MasterRegistration =>
            log.info(s"MasterRegister ${sender().path}")
            println("MasterRegister")
            context watch sender()
            nodes = sender() :: nodes
            dispatchTasks()
        case Terminated(a) =>
            nodes = nodes.filterNot(node => node.eq(a))
            println(s"terminated current node $nodes")
        case x =>
            println(x.toString)
    }

    def dispatchTask(task: Task): Unit = {
        if (nodes.nonEmpty) {
            println("send task")
            nodes(Math.abs(task.hashCode() % nodes.size)) ! task
        } else {
            tasks = task :: tasks
        }
    }

    def dispatchTasks() = {
        tasks.foreach(dispatchTask)
    }
}
