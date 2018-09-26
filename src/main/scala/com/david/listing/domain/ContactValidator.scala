package com.david.listing.domain

import cats.ApplicativeError
import cats.implicits._
import com.david.listing.domain.Domain._

import scala.util.Try

trait ContactValidator[F[_]] {
  def validate(listing: Contact): F[Contact]
}

object ContactValidator {
  def apply[F[_]](implicit ev: ContactValidator[F]): ContactValidator[F] = ev

  def validator[F[_], E](mkError: PropertyValidationError => E)(
    implicit
    A: ApplicativeError[F, E]
  ): ContactValidator[F] =
    new ContactValidator[F] {

      val FormattedPhoneRegex = """(\+\d{1,3} \d{3,4}-\d{2,4}-\d{3,4})""".r

      /*
       * It validates that the phone is a numeric value.
       */
      def validatePhone(phone: String): F[String] = {
        val tryPhone = Try(phone.toLong)
        if (tryPhone.isSuccess) phone.pure[F]
        else A.raiseError(mkError(PhoneContactNotNumericError))
      }

      /*
       * It validates that the formattedPhone follow the format specified in a regex.
       */
      def validateFormattedPhone(formattedPhone: String): F[String] = formattedPhone match {
        case FormattedPhoneRegex(formatted) => formattedPhone.pure[F]
        case _ => A.raiseError(mkError(PhoneContactNonCorrectlyFormattedError))
      }

      /*
       * Main entry point, it provides the validation of the Contact class
       * sequencing the validation of the object using Cats applicative
       */
      def validate(contact: Contact): F[Contact] = {
        (Contact.apply _).curried.pure[F].
          ap(validatePhone(contact.phone)).
          ap(validateFormattedPhone(contact.formattedPhone))
      }
    }

  def validate[F[_]: ContactValidator, E](contact: Contact): F[Contact] =
    ContactValidator[F].validate(contact)
}