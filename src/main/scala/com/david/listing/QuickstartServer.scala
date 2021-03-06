package com.david.listing

import java.util.logging.Logger

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.util.Timeout

import scala.concurrent.duration._

object QuickstartServer extends HttpApp with App with PropertyRoutes {
  val logger = Logger.getLogger(QuickstartServer.getClass.getName)
  logger.info("Initializating Server")
  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("usersHttpServer")
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  val propertyActor: ActorRef = system.actorOf(PropertyActor.props, "userRegistryActor")

  lazy val routes: Route = propertiesRoutes

  logger.info("Server started at http://localhost:8080/ ")
  startServer("0.0.0.0", 8080)

}

