package com.david.listing.domain

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import cats.{ ApplicativeError, Apply }
import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import com.david.listing.domain.Domain._

import scala.util.Try

trait ListingValidator[F[_]] {
  def validate(listing: Listing, emptyID: Boolean): ValidatedNel[PropertyValidationError, Listing]
}

object ListingValidator {
  /*
   * Definition of the implicit address and contact validators
   */
  implicit val contactValidatorValidatedInterpreter =
    ContactValidator.validator[Validated[NonEmptyList[PropertyValidationError], ?], NonEmptyList[PropertyValidationError]](NonEmptyList(_, Nil))
  implicit val addressValidatorValidatedInterpreter =
    AddressValidator.validator[Validated[NonEmptyList[PropertyValidationError], ?], NonEmptyList[PropertyValidationError]](NonEmptyList(_, Nil))

  def apply[F[_]](implicit ev: ListingValidator[F]): ListingValidator[F] = ev

  def validator[F[_], E](mkError: PropertyValidationError => E)(
    implicit
    A: ApplicativeError[F, E]
  ): ListingValidator[F] =
    new ListingValidator[F] {

      def validateId(idOpt: Option[String], emptyID: Boolean): Validated[PropertyValidationError, Option[String]] = {
        emptyID match {
          case true => idOpt match {
            case None => Valid(idOpt)
            case Some(_) => Invalid(IdNonEmptyError)
          }
          case false => idOpt match {
            case None => Invalid(IdEmptyError)
            case Some(id) => Try(UUID.fromString(id)).isSuccess match {
              case true => Valid(idOpt)
              case false => Invalid(NonUUIDFormatError)
            }
          }
        }

      }

      def validateGeoLocation(location: GeoLocation): Validated[PropertyValidationError, GeoLocation] = {
        val tryLat = Try(location.lat.toDouble)
        val tryLong = Try(location.lng.toDouble)
        if (tryLat.isSuccess && tryLong.isSuccess)
          Valid(location)
        else Invalid(GeoLocationNonExistingError)
      }

      /*
        Validates that the property has a valid format. It calls internally another validators and join the result.
        @param property -> The property
        @param emptyID -> Indicates to the validator if the id should be empty or not. In case the id shouldnt be empty, checks that it has UUID format.
       */
      def validate(listing: Listing, emptyID: Boolean): ValidatedNel[PropertyValidationError, Listing] = {
        Apply[ValidatedNel[PropertyValidationError, ?]].map4(
          validateId(listing.id, emptyID).toValidatedNel,
          ContactValidator.validate(listing.contact),
          AddressValidator.validate(listing.address),
          validateGeoLocation(listing.location).toValidatedNel
        ) {
            case (id, contact, address, location) => Listing(id, contact, address, location)
          }
      }
    }

  /*
   * Validate a listing. For doing that it requires to know if it is allowed to have an emptyID as part of the document or not.
   * An emptyID would be the case of a POST, while in a PUT the Id shouldnt be empty.
   *
   * It join all the validations of all the different attributes of the Listing object. ContactValidator, AddressValidator.
   * The geoLocation and Id validation are being done as part of the ListingValidator.
   */
  def validate[F[_]: ListingValidator, E](listing: Listing, emptyID: Boolean): ValidatedNel[PropertyValidationError, Listing] =
    ListingValidator[F].validate(listing, emptyID)

}

