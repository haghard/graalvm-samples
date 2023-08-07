package com.github.vmencik.akkanative

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.http.scaladsl.Http
import akka.actor.CoordinatedShutdown
import akka.actor.{ ActorSystem => ClassicSystem }

import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps

object Main extends Routes {

  sealed trait Command

  private case object BindFailure extends CoordinatedShutdown.Reason

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName()

    val httpAddress = sys.env.getOrElse("HTTP_ADDRESS", "::1")
    val httpPort = sys.env.getOrElse("HTTP_PORT", "8080").toInt

    /*
    config.getString("http.service.bind-to"),
    config.getInt("http.service.port")
    val config = ConfigFactory.load()
    implicit val system = ActorSystem[Nothing](Behaviors.empty, "graal", config)
     */

    val classicSystem = ClassicSystem("graal")
    classicSystem.spawn(Main(httpAddress, httpPort), "guardian")
  }

  def apply(httpAddress: String, httpPort: Int): Behavior[Command] =
    Behaviors.setup { ctx =>
      implicit val system = ctx.system
      implicit val logger = ctx.log
      import ctx.executionContext

      logger.info(system.printTree)
      logger.info(s"Binding on $httpAddress $httpPort")

      Http()
        .newServerAt(httpAddress, httpPort)
        .bind(rndRoute())
        .failed
        .foreach { ex =>
          logger.error("Binding error", ex)
          CoordinatedShutdown(system).run(BindFailure)
        }
      Behaviors.empty
    }
}
