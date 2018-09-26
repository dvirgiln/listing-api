package com.david.listing.domain

import cats.ApplicativeError
import cats.implicits._
import com.david.listing.domain.Domain._

trait AddressValidator[F[_]] {
  def validate(listing: Address): F[Address]
}

object AddressValidator {
  def apply[F[_]](implicit ev: AddressValidator[F]): AddressValidator[F] = ev

  def validator[F[_], E](mkError: PropertyValidationError => E)(
    implicit
    A: ApplicativeError[F, E]
  ): AddressValidator[F] =
    new AddressValidator[F] {

      /*
       * No real validation has been done in the address
       */
      def validateAddress(address: String): F[String] = address.pure[F]

      /*
       * No real validation has been done in the postal code.
       */
      def validatePostalCode(postalCode: String): F[String] = postalCode.pure[F]

      /*
       * I assumed here that the country code is a 2 characters code.
       */
      def validateCountryCode(countryCode: String): F[String] =
        if (countryCode.size == 2) countryCode.pure[F]
        else A.raiseError(mkError(AddressNonCorrectCountryCodeError))

      /*
       * Validates that the state property is assigned correctly. I assumed that US is the only country with the state property.
       * In case the country code is US then it should have the state property. In case it is any other country the state should be None.
       */
      def validateState(address: Address): F[Option[String]] = {
        if (address.countryCode == "US") address.state match {
          case None => A.raiseError(mkError(AddressShouldContainStateError))
          case Some(_) => address.state.pure[F]
        }
        else {
          address.state match {
            case None => address.state.pure[F]
            case Some(_) => A.raiseError(mkError(AddressShouldNotContainStateError))
          }
        }
      }

      /*
       * It has been validated that the city is not empty. Mandatory property.
       */
      def validateCity(city: String): F[String] =
        if (city.isEmpty)
          A.raiseError(mkError(AddressCityIsEmptyError))
        else
          city.pure[F]

      /*
       * It has been validated that the country is not empty. Mandatory property.
       */
      def validateCountry(country: String): F[String] =
        if (country.isEmpty)
          A.raiseError(mkError(AddressCountryIsEmptyError))
        else
          country.pure[F]

      /*
       * Main entry point, it provides the validation of the Address class
       * sequencing the validation of the object using Cats applicative
       */
      def validate(address: Address): F[Address] = {
        (Address.apply _).curried.pure[F].
          ap(validateAddress(address.address)).
          ap(validatePostalCode(address.postalCode)).
          ap(validateCountryCode(address.countryCode)).
          ap(validateCity(address.city)).
          ap(validateState(address)).
          ap(validateCountry(address.country))
      }
    }

  def validate[F[_]: AddressValidator, E](contact: Address): F[Address] = AddressValidator[F].validate(contact)
}