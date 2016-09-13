package com.capslock.crawler

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import play.api.libs.json.Json

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Created by capslock1874.
 */
class Worker(master: ActorRef)(implicit materializer: ActorMaterializer) extends Actor with ActorLogging {

    import akka.pattern.pipe
    import context.dispatcher

    val http = Http(context.system)

    case class TaskResponse(topic: String, url: String, body: String, headers: immutable.Seq[HttpHeader], sender: ActorRef)

    override def receive: Receive = {
        case TaskResponse(topic, url, body, headers, taskSender) =>
            processResponse(topic, url, body, headers)
            taskSender ! TaskResult(Some("worker finish"))
        case Task(topic, request) =>
            val taskSender = sender()
            val requestHeaders = immutable.Seq(Worker.cookie)
            http.singleRequest(request).map {
                case HttpResponse(StatusCodes.OK, headers, entity, _) =>
                    entity
                        .toStrict(10 seconds)
                        .map(_.data.decodeString("UTF-8"))
                        .map(body => TaskResponse(topic, request._2.path.toString(), body, headers, taskSender))
                        .pipeTo(self)
                case resp@HttpResponse(code, _, _, _) =>
                    resp.discardEntityBytes()
            }
    }

    def processUserProfileResponse(topic: String, username: String, url: String, body: String, headers: immutable.Seq[HttpHeader]): List[Task] = {
        for (userProfile <- UserProfileExtractor(username, url, body).extract()) {
            val a_t = headers.find(header => header.name().equals("Set-Cookie") && header.value().startsWith("a_t"))
                .map(header => header.value()).map(cookie => {
                cookie.substring(4, cookie.indexOf("; "))
            })
            val z_c0 = headers.find(header => header.name().equals("Set-Cookie") && header.value().startsWith("z_c0"))
                .map(header => header.value()).map(cookie => {
                cookie.substring(5, cookie.indexOf("; "))
            })
            for (aTCookie <- a_t; zC0Cookie <- z_c0) {
                val cookie = Cookie(
                    ("a_t", aTCookie),
                    ("z_c0", zC0Cookie)
                )
                val requestHeaders = immutable.Seq(cookie)
                val offset = 0
                val hash = userProfile.hash
                val params = Json.obj(
                    "method" -> "next",
                    "params" -> Json.obj(
                        "offset" -> offset,
                        "order_by" -> "created",
                        "hash_id" -> hash
                    ))
                val formData = FormData(("method", "next"), ("params", params.toString())).toEntity
                val httpRequest = HttpRequest(HttpMethods.GET, Worker.profileFolloweesList, headers = requestHeaders, entity = formData)
                List(Task(topic, httpRequest))
            }
        }
        List()
    }

    def processResponse(topic: String, url: String, body: String, headers: immutable.Seq[HttpHeader]): Unit = {
        url match {
            case Worker.profileUrlPattern(username) =>
                processUserProfileResponse(topic, username, url, body, headers)
            case `url` if url.startsWith(Worker.profileFolloweesList) =>

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
        ("a_t", "2.0AABALMEZAAAXAAAAF2b7VwAAQCzBGQAAAFDAl3xCoQkXAAAAYQJVTR061VcAPL0fuNcxkZIqUEH1aZN-jc1ztVuCuFIE1hL8SAOdlMihPr8nICgomA=="),
        ("z_c0", "Mi4wQUFCQUxNRVpBQUFBSU1BTy1RaUZDaGNBQUFCaEFsVk5WWHI3VndBczJ5TlF3RDMwXzBpcU95UWJRdG9HWm5lU2dR|1473689929|ef67a4899d6c5ae5a6d4fd7a4e1f42a3f9fddcdb")
    )
    val profileUrlPattern = "https://www.zhihu.com/people/(.*)".r
    val profileFolloweesList = "https://www.zhihu.com/node/ProfileFolloweesListV2"

    def props(master: ActorRef)(implicit materializer: Materializer): Props = {
        Props(classOf[Worker], master, materializer)
    }

    def main(args: Array[String]) {
        implicit val system = ActorSystem("ClusterSystem")
        implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
        import ExecutionContext.Implicits.global
        val http = Http(system)
        val requestHeaders = immutable.Seq(Worker.cookie)
        val offset = 20
        val hash = "0970f947b898ecc0ec035f9126dd4e08"
        val params = Json.obj(
            "offset" -> offset,
            "order_by" -> "created",
            "hash_id" -> hash
        )
        val formData = FormData(("method", "next"), ("params", params.toString())).toEntity
        val httpRequest = HttpRequest(HttpMethods.GET, "https://www.zhihu.com/node/ProfileFolloweesListV2", headers = requestHeaders, entity = formData)
        http.singleRequest(httpRequest).map {
            case HttpResponse(StatusCodes.OK, headers, entity, _) =>
                entity
                    .toStrict(10 seconds)
                    .map(_.data.decodeString("UTF-8"))
                    .map(body => println(body))
            case x =>
                println(x)
        }
    }
}
