package com.david.listing

import com.david.listing.domain.Domain._
//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {

  import DefaultJsonProtocol._
  implicit val geoLocationJsonFormat = jsonFormat2(GeoLocation)
  implicit val contactJsonFormat = jsonFormat2(Contact)
  implicit val addressJsonFormat = jsonFormat6(Address)
  implicit val listingJsonFormat = jsonFormat4(Listing)
  implicit val propertyJsonFormat = jsonFormat1(Property)
  implicit val listingsJsonFormat = jsonFormat1(Listings)
}
