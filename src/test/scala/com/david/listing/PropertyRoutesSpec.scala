package com.david.listing

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, RequestEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import com.david.listing.domain.Domain._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

class PropertyRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with PropertyRoutes with BeforeAndAfterAll {

  implicit lazy val timeout = Timeout(5.seconds)

  val propertyActor: ActorRef = system.actorOf(PropertyActor.props, "listingRegistryActor")

  val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
  lazy val routes = propertiesRoutes

  override def beforeAll() {
    ListingService.init
  }

  "ListingRoutes" should {
    "return the initial empty list of listings (GET /listings)" in {
      val request = HttpRequest(uri = "/listings")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"listings":[]}""")
      }
    }

    "add one listing (POST /listings)" in {
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        val returnValue = entityAs[Property]
        assert(checkEqualsListings(returnValue, listing, false) == true)
        assert(returnValue.id != None)
      }
    }

    "return a 400 when the listing to be created contains already an ID (POST /listings)" in {
      val listing = Property(Listing(Some("5e22a83a-6f4f-11e6-8b77-86f30ca893d3"), Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("""IdNonEmptyError""")
      }
    }

    "delete a listing" in {
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[Property]
        val request2 = Delete(uri = s"/listings/${returnValue.id.get}")

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
        }
      }
    }

    "return an error when it is tried to delete a non existing listing" in {
      Delete(uri = s"/listings/fdsdfs") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "get a listing (GET /listings)" in {
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[Property]
        val request2 = HttpRequest(uri = s"/listings/${returnValue.id.get}")

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
          entityAs[Property] should ===(returnValue)
        }
      }

    }

    "update listing details in case listing exist already (PUT /listings)" in {
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[Property]
        val updatedListing = returnValue.copy(listing = returnValue.listing.copy(contact = Contact("441545423264", "+44 154-542-3264")))
        val listingEntity2 = Marshal(updatedListing).to[RequestEntity].futureValue
        val request2 = Put(uri = "/listings").withEntity(listingEntity2)

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
        }
      }
    }

    "return an error 400 in case the id is not provided as part of the update (PUT /listings)" in {
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Put(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("IdEmptyError")
      }
    }

    "return an error 404 in case the id of the listing provided doesnt exist (PUT /listings)" in {
      val listing = Property(Listing(Some("c00918a3-ce28-4188-9ff1-838001a83d2b"), Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Put(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "return an error 400 in case the id has not a correct format" in {
      val listing = Property(Listing(Some("NON EXISTING UUID"), Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Put(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        entityAs[String] should ===("NonUUIDFormatError")
      }
    }

    "return an error 400 in case the validation fails" in {
      val listing = Property(Listing(Some("NOT CORRECT UUID"), Contact("4AA41525463264", "+44 152-546-3264"), address, GeoLocation("AAA40.4255485534668", "-3.7075681686401367")))
      val listingEntity = Marshal(listing).to[RequestEntity].futureValue
      val request = Post(uri = "/listings").withEntity(listingEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        entityAs[String] should ===("IdNonEmptyError PhoneContactNotNumericError GeoLocationNonExistingError")
      }
    }
  }

  private def checkEqualsListings(a: Property, b: Property, checkId: Boolean): Boolean = {
    a.listing.contact == b.listing.contact &&
      a.listing.address == b.listing.address &&
      a.listing.location == b.listing.location && (!checkId || (a.id == b.id))
  }

}
