package com.capslock.crawler

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

/**
 * Created by capslock1874.
 */
object Boot extends App {
    implicit val system = ActorSystem("ClusterSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    val topic = "zhihu"
    system.actorOf(CrawlerScheduler.props(topic, "https://www.zhihu.com/people/excited-vczh"), CrawlerScheduler.name(topic))
}
