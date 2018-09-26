package com.david.listing

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import cats.data.{ NonEmptyList, Validated }
import com.david.listing.PropertyActor.{ CreateProperty, DeleteProperty, GetProperties, GetProperty, UpdateProperty }
import com.david.listing.domain.{ ListingValidator, PropertyValidator }

import scala.concurrent.Future

trait PropertyRoutes extends JsonSupport {

  import com.david.listing.domain.Domain._

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val logListing = Logging(system, classOf[PropertyRoutes])
  implicit val timeout: Timeout

  implicit val propertyValidatorValidatedInterpreter =
    PropertyValidator.validator[Validated[NonEmptyList[PropertyValidationError], ?], NonEmptyList[PropertyValidationError]](NonEmptyList(_, Nil))

  def propertyActor: ActorRef

  lazy val propertiesRoutes: Route =
    pathPrefix("listings") {
      concat(
        path(Segment) { id =>
          concat(
            get {
              logListing.info(s"Getting property $id")
              val maybeListing: Future[Option[Property]] =
                (propertyActor ? GetProperty(id)).mapTo[Future[Option[Property]]].flatten
              onSuccess(maybeListing) { response =>
                response match {
                  case Some(property) => complete(property)

                  case None => complete(StatusCodes.NotFound)
                }
              }
            },
            delete {
              logListing.info(s"Deleting property $id")
              val responseFuture: Future[Boolean] = (propertyActor ? DeleteProperty(id)).mapTo[Future[Boolean]].flatten
              onSuccess(responseFuture) { response =>
                response match {
                  case true => complete(StatusCodes.OK)
                  case false => complete(StatusCodes.NotFound)
                }
              }
            }
          )
        },
        pathEnd {
          concat(
            get {
              val propertiesFuture: Future[Seq[Property]] =
                (propertyActor ? GetProperties).mapTo[Future[Seq[Property]]].flatten
              onSuccess(propertiesFuture) { properties =>
                complete(Listings(properties))
              }
            },
            post {
              entity(as[Property]) { property =>
                val validatedListing = PropertyValidator.validate(property, emptyID = true)
                if (validatedListing.isValid) {
                  val listingCreated: Future[Property] =
                    (propertyActor ? CreateProperty(property)).mapTo[Future[Property]].flatten
                  onSuccess(listingCreated) { listing =>
                    complete((StatusCodes.Created, listing))
                  }
                } else {
                  val errors = validatedListing.toEither.left.get.toList
                  complete(StatusCodes.BadRequest, s"${errors.mkString(" ")}")
                }
              }
            },
            put {
              entity(as[Property]) { property =>
                val validatedListing = PropertyValidator.validate(property, emptyID = false)
                if (validatedListing.isValid) {
                  val listingUpdated: Future[Boolean] =
                    (propertyActor ? UpdateProperty(property)).mapTo[Future[Boolean]].flatten
                  onSuccess(listingUpdated) { updated =>
                    updated match {
                      case true => complete(StatusCodes.OK)
                      case false => complete(StatusCodes.NotFound)
                    }
                  }
                } else {
                  val errors = validatedListing.toEither.left.get.toList
                  complete(StatusCodes.BadRequest, s"${errors.mkString(" ")}")
                }
              }
            }

          )
        }
      )
    }
}
