package com.david.listing.domain

import com.david.item.UUIDable

object Domain {

  case class Property(listing: Listing) extends UUIDable { def id: Option[String] = listing.id }
  case class Listing(id: Option[String], contact: Contact, address: Address, location: GeoLocation)
  case class Contact(phone: String, formattedPhone: String)
  case class Address(address: String, postalCode: String, countryCode: String, city: String, state: Option[String], country: String)
  case class GeoLocation(lat: String, lng: String)
  case class Listings(listings: Seq[Property])

  //case classes used for validation errors
  sealed trait PropertyValidationError

  case object PhoneContactNotNumericError extends PropertyValidationError
  case object PhoneContactNonCorrectlyFormattedError extends PropertyValidationError
  case object GeoLocationNonExistingError extends PropertyValidationError
  case object AddressNonCorrectCountryCodeError extends PropertyValidationError
  case object AddressShouldContainStateError extends PropertyValidationError
  case object AddressShouldNotContainStateError extends PropertyValidationError
  case object AddressCityIsEmptyError extends PropertyValidationError
  case object AddressCountryIsEmptyError extends PropertyValidationError
  case object NonUUIDFormatError extends PropertyValidationError
  case object IdEmptyError extends PropertyValidationError
  case object IdNonEmptyError extends PropertyValidationError

}

