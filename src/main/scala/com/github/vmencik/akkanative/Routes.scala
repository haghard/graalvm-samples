package com.github.vmencik.akkanative

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives.*

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.{ExecutionContext, Future}

trait Routes {

  def rndRoute() = {

    def nextRnd()(implicit ec: ExecutionContext, logger: LoggingAdapter) =
      Future {
        val rnd = ThreadLocalRandom.current().nextLong()
        logger.info("[{}] - Rnd({})", Thread.currentThread().getName, rnd)
        rnd
      }

    extractLog { implicit log =>
      extractExecutionContext { implicit ec =>
        path("rnd") {
          get {
            onSuccess(nextRnd()) { rnd => complete(rnd.toString()) }
          }
        }
      }
    }
  }
}
