package com.capslock.crawler

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory

/**
 * Created by capslock1874.
 */
object MasterBoot extends App {
    val config = ConfigFactory.parseString(
        """
          |akka {
          |  actor {
          |    provider = "akka.cluster.ClusterActorRefProvider"
          |  }
          |  remote {
          |    log-remote-lifecycle-events = off
          |    netty.tcp {
          |      hostname = "127.0.0.1"
          |      port = 2552
          |    }
          |  }
          |
          |  cluster {
          |    roles = [master]
          |    seed-nodes = [
          |      "akka.tcp://ClusterSystem@127.0.0.1:2551"]
          |  }
          |
          |}
        """.stripMargin)
    implicit val system = ActorSystem("ClusterSystem", config)
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    val topic = "zhihu"
    system.actorOf(Master.props(topic), Master.name(topic))
}
