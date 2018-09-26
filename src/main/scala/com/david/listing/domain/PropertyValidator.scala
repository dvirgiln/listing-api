package com.david.listing.domain

import cats.{ ApplicativeError, Apply }
import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import com.david.listing.domain.Domain._

trait PropertyValidator[F[_]] {
  def validate(property: Property, emptyID: Boolean): ValidatedNel[PropertyValidationError, Property]
}

/*
 * The Property Validator initially just call the Listing Validator using the ValidatedNel Monad.
 * In the future if the Property has extra attributes apart from the listing they can be easily attached to this validator.
 */
object PropertyValidator {
  implicit val listingValidatorValidatedInterpreter =
    ListingValidator.validator[Validated[NonEmptyList[PropertyValidationError], ?], NonEmptyList[PropertyValidationError]](NonEmptyList(_, Nil))

  def apply[F[_]](implicit ev: PropertyValidator[F]): PropertyValidator[F] = ev

  def validator[F[_], E](mkError: PropertyValidationError => E)(
    implicit
    A: ApplicativeError[F, E]
  ): PropertyValidator[F] = new PropertyValidator[F] {

    /*
     * Makes use of the Validated Monad to attach all the validation messages provide by the ListingValidator to the result.
     */
    def validate(property: Property, emptyID: Boolean): ValidatedNel[PropertyValidationError, Property] = {
      val listingValidation = ListingValidator.validate(property.listing, emptyID)
      Apply[ValidatedNel[PropertyValidationError, ?]].map(listingValidation) {
        case (listing) => Property(listing)
      }
    }
  }

  def validate[F[_]: PropertyValidator, E](property: Property, emptyID: Boolean): ValidatedNel[PropertyValidationError, Property] =
    PropertyValidator[F].validate(property, emptyID)
}

