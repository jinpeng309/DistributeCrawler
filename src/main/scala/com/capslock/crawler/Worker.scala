package com.capslock.crawler

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}

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

    case class TaskResponse(url: String, body: String, headers: immutable.Seq[HttpHeader], sender: ActorRef)

    override def receive: Receive = {
        case TaskResponse(url, body, headers, taskSender) =>
            processResponse(url, body, headers)
            taskSender ! TaskResult(Some("worker finish"))
        case Task(_, url) =>
            val taskSender = sender()
            val requestHeaders = immutable.Seq(Worker.cookie)
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

    def processUserProfileResponse(username: String, url: String, body: String, headers: immutable.Seq[HttpHeader]): Unit = {
        for (profile <- UserProfileExtractor(username, url, body).extract()) {
            val a_t = headers.find(header => header.name().equals("Set-Cookie") && header.value().startsWith("a_t"))
                .map(header => header.value()).map(cookie => {
                cookie.substring(4, cookie.indexOf("; "))
            })
            val z_c0 = headers.find(header => header.name().equals("Set-Cookie") && header.value().startsWith("z_c0"))
                .map(header => header.value()).map(cookie => {
                cookie.substring(5, cookie.indexOf("; "))
            })
            println(profile.hash)
        }
    }

    def processResponse(url: String, body: String, headers: immutable.Seq[HttpHeader]): Unit = {
        url match {
            case Worker.profileUrlPattern(username) =>
                processUserProfileResponse(username, url, body, headers)
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
        val offset = 0
        val hash = "0970f947b898ecc0ec035f9126dd4e08"
        val formData = FormData(("method", "next"), ("params", "{\"offset\":20,\"order_by\":\"created\",\"hash_id\":\"0970f947b898ecc0ec035f9126dd4e08\"}")).toEntity
        val httpRequest = HttpRequest(HttpMethods.GET, "https://www.zhihu.com/node/ProfileFolloweesListV2", headers = requestHeaders, entity = formData)
        http.singleRequest(httpRequest)
            .foreach(response => println(response.headers.foreach(httpHeader => println(httpHeader))))
    }
}
