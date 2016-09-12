package com.capslock.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, Materializer}

import scala.collection.immutable
import scala.concurrent.duration._

/**
 * Created by capslock1874.
 */
class Worker(implicit materializer: ActorMaterializer) extends Actor with ActorLogging {

    import akka.pattern.pipe
    import context.dispatcher

    val http = Http(context.system)

    case class TaskResponse(url: String, body: String, headers: immutable.Seq[HttpHeader], sender: ActorRef)

    override def receive: Receive = {
        case TaskResponse(url, body, headers, taskSender) =>
            processResponse(url, body)
            taskSender ! TaskResult(Some("worker finish"))
        case Task(_, url) =>
            val taskSender = sender()
            val requestHeaders = immutable.Seq(Worker.cookie)
            //            http.singleRequest(HttpRequest(uri = "https://www.zhihu.com/node/ProfileFolloweesListV2?meethod=next&params=%7B%22offset%22%3A60%2C%22order_by%22%3A%22created%22%2C%22hash_id%22%3A%220970f947b898ecc0ec035f9126dd4e08%22%7D", headers = headers)).map {
            //                case t =>
            //                    println(t)
            //            }
            http.singleRequest(HttpRequest(uri = url, headers = requestHeaders)).map {
                case HttpResponse(StatusCodes.OK, headers, entity, _) =>
                    entity
                        .toStrict(10 seconds)
                        .map(_.data.decodeString("UTF-8"))
                        .map(body => TaskResponse(url, body, headers, taskSender))
                        .pipeTo(self)
                case resp@HttpResponse(code, _, _, _) =>
                    resp.discardEntityBytes()
            }
    }

    def processResponse(url: String, body: String): Unit = {
        url match {
            case Worker.profileUrlPattern(username) =>
                println(username)
        }
    }

    def extractNextTask(response: TaskResponse): Option[Task] = {
        val body = response.body
        val url = response.url
        Option.empty
    }
}

object Worker {
    val cookie = Cookie(
    )
    val profileUrlPattern = "https://www.zhihu.com/people/(.*)".r

    def props(implicit materializer: Materializer): Props = {
        Props(classOf[Worker], materializer)
    }
}
