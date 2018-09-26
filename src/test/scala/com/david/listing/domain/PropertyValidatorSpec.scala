package com.david.listing.domain

import cats.data.{NonEmptyList, Validated}
import com.david.listing.domain.Domain._
import org.scalatest.{Matchers, WordSpec}

class PropertyValidatorSpec extends WordSpec with Matchers {

  implicit val listingValidatorValidatedInterpreter =
    PropertyValidator.validator[Validated[NonEmptyList[PropertyValidationError], ?],
      NonEmptyList[PropertyValidationError]](NonEmptyList(_, Nil))


  "PropertyValidator" should {
    "check a valid listing with an empty ID" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, true)
      result.isValid should ===(true)
      result.toEither.isRight should ===(true)
      result.toEither.right.get should ===(listing)

    }

    "check a valid listing with UUID" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(Some("c00918a3-ce28-4188-9ff1-838001a83d2b"), Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, false)
      result.isValid should ===(true)
      result.toEither.isRight should ===(true)
      result.toEither.right.get should ===(listing)
    }

    "check a invalid listing with an non empty ID when it should be empty" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(Some("c00918a3-ce28-4188-9ff1-838001a83d2b"), Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, emptyID = true)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(IdNonEmptyError))
    }

    "check a invalid listing with an empty ID when it should be non empty" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, emptyID = false)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(IdEmptyError))
    }

    "check an invalid contact" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(None, Contact("fdsadfsafdsa", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, true)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(PhoneContactNotNumericError))
    }

    "check an invalid contact formatted number" in {
      val address = Address("Street name", "N10SR", "EN", "London", None, "UK")
      val listing = Property(Listing(None, Contact("4444444444", "+4444 152-546-3"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, true)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(PhoneContactNonCorrectlyFormattedError))
    }

    "check an invalid address" in {
      val address = Address("Street name", "N10SR", "ENA", "London", None, "UK")
      val listing = Property(Listing(None, Contact("441525463264", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, true)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(AddressNonCorrectCountryCodeError))
    }


    "check multiple errors" in {
      val address = Address("Street name", "N10SR", "ENA", "London", None, "UK")
      val listing = Property(Listing(None, Contact("fdsadfsafdsa", "+44 152-546-3264"), address, GeoLocation("40.4255485534668", "-3.7075681686401367")))
      val result = PropertyValidator.validate(listing, true)
      result.isValid should ===(false)
      result.toEither.isRight should ===(false)
      result.toEither.left.get.toList should ===(List(PhoneContactNotNumericError, AddressNonCorrectCountryCodeError))
    }
  }
}
