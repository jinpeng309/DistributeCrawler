package com.capslock.crawler

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.Materializer
import com.capslock.crawler.CrawlerScheduler.{HEAVY, LIGHT, Node}

/**
 * Created by capslock1874.
 */

object CrawlerScheduler {
    def props(topic: String, initUrl: String)(implicit materializer: Materializer): Props =
        Props(classOf[CrawlerScheduler], topic, initUrl, materializer)

    def name(topic: String) = s"crawler-$topic"

    sealed trait LOAD

    case object HEAVY extends LOAD

    case object LIGHT extends LOAD

    case class Node(actor: ActorRef, var load: LOAD)

}

class CrawlerScheduler(topic: String, initUrl: String)(implicit materializer: Materializer) extends Actor with ActorLogging {
    val cluster = Cluster(context.system)
    var tasks = List[Task](Task(topic, wrapHttpRequest(initUrl)))
    var nodes: List[CrawlerScheduler.Node] = List()

    def wrapHttpRequest(url: String): HttpRequest = {
        HttpRequest(HttpMethods.GET, url)
    }

    override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

    override def postStop(): Unit = cluster.unsubscribe(self)

    override def receive: Receive = {
        case TaskResult(Some(nextUrl)) =>
            log.info(s"next url $nextUrl")
            dispatchTask(Task(topic, wrapHttpRequest("https://www.zhihu.com/people/excited-vczh")))
        case MasterRegistration =>
            log.info(s"MasterRegister ${sender().path}")
            context watch sender()
            nodes = Node(sender(), LIGHT) :: nodes
            dispatchTasks()
        case Terminated(a) =>
            nodes = nodes.filterNot(node => node.actor.eq(a))
        case StopPushTaskFeedback =>
            nodes.find(node => node.actor.eq(sender())).foreach(node => node.load = HEAVY)
        case PullTask =>
            dispatchTask(sender())
        case x =>
            println(x.toString)
    }

    def dispatchTask(node: ActorRef): Unit = {
        if (tasks.nonEmpty) {
            node ! tasks.head
            tasks = tasks.tail
        }
    }

    def dispatchTask(task: Task): Unit = {
        if (nodes.nonEmpty) {
            nodes.filterNot(node => node.load == HEAVY)(Math.abs(task.hashCode() % nodes.size)).actor ! task
        } else {
            tasks = task :: tasks
        }
    }

    def dispatchTasks() = {
        tasks.foreach(dispatchTask)
    }
}
