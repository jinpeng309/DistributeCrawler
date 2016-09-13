package com.capslock.crawler

import akka.http.scaladsl.model.HttpRequest

/**
 * Created by capslock1874.
 */
case class Task(topic: String, request: HttpRequest)
