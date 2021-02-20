# Introduction
sjdkjghfdjbgfkjbkfvThis project provides an example of API using Akka Http and Akka Actors and Cats Form Validation.

The API contains basic calls for making transactions between properties Listings.
### Usage

        sbt docker:publishLocal
        docker run --rm -p8080:8080 listing-api:0.1-SNAPSHOT

### Endpoints

- GET /listings  -> retrieves a list of all the listings
- GET /listings/${UUID} -> retrieves the listing with the ID
- POST /listings  -> stores a new Listing. The only difference with the original request is that the lat and lng are String instead of Double. The reason is to avoid lose the original data due to the floating precision.
```
        {
          "listing": {
            "contact": {
              "phone": "15126841100",
              "formattedPhone": "+1 512-684-1100"
            },
            "address": {
              "address": "1011 W 5th St",
              "postalCode": "1011",
              "countryCode": "US",
              "city": "Austin",
              "state": "TX",
              "country": "United States"
            },
            "location": {
              "lat": "40.4255485534668",
              "lng": "-3.7075681686401367"
            }
          }
        }
```
  - Possible Values:
    - Sucessfull 200
    - Error 400:
        - PhoneContactNotNumericError
        - PhoneContactNonCorrectlyFormattedError
        - GeoLocationNonExistingError
        - AddressNonCorrectCountryCodeError
        - AddressShouldContainStateError
        - AddressShouldNotContainStateError
        - AddressCityIsEmptyError
        - AddressCountryIsEmptyError
        - NonUUIDFormatError
        - IdNonEmptyError

- PUT /listings -> Update an existing listing. Possible values
    - Sucessfull 200
    - Error 400:
        - PhoneContactNotNumericError
        - PhoneContactNonCorrectlyFormattedError
        - GeoLocationNonExistingError
        - AddressNonCorrectCountryCodeError
        - AddressShouldContainStateError
        - AddressShouldNotContainStateError
        - AddressCityIsEmptyError
        - AddressCountryIsEmptyError
        - NonUUIDFormatError
        - IdEmptyError
    - Error 404
- DELETE /users/${ID} -> Possible values
    - Sucessful 200
    - Error 404

It has been attached a postman exported collection with the different endpoints.


###  Exercise Considerations

It was a bit confusing at the begining how to structure the model. In the exercise was specified that is a service that allows to store, list and delete property documents, but then it was including the endpoint as /listings.

The Property Document specified in the exercise included a property "listing":
- If it is already a Listing, I would have removed the property Listing and just have in the root all the listing attributes: id, contact, address and location.
- So, to be clear what I have done, as the exercise was a bit open, I tried to adjust the maximum to what I was asked:
  1. The endpoints are /listings
  2. As there is an attribute listing, inside of the json document, I opted to create an entity Property(listing: Listing).
  3. The Actor and Routes files they have the prefix Property.
  4. Taking a look to the model, you can have a more clear picture about my considerations.
  5. From the original document, I opted to define location.lat and location.lng as String, to avoid problems with the floating precision.

###  Tech Stack
The code uses:
  - Akka Actors: PropertyActor. Simple actor that receive the PUT, POST, GET and DELETE messages.
  - Akka Http: simple and intuitive routes file. It instantiate the cats validation and route messages to the akka actor.
  - Cats: used for form validation. It uses the Validated monad and Applicative monad to sequence validations.
 - Docker: it is used to deploy the app in a light tomcat container.

###  Code Structure
This is a guide about how the code is structured:
 - The docker entrypoint is the QuickstartServer.
 - It contains as Controller layer an Akka Http Routes file: PropertyRoutes.
 - As a service asynchronous layer -> PropertyActor.
 - As a storage it has been implemented an interesting GenericStoreService. This store service is a typed class and can store different kind of objects. It contains the basic methods for get, add, delete, get_all. The only requirement is that the stored class should implement UUIDable.
 - For form validation has been used cats, making use of Validated and Applicative monads. What it is interesting here is that there is a nested validation:
```
PropertyValidator -> ListingValidator -> AddressValidator
                                         ContactValidator
 ```
 - It contains scala tests that proves the correct run of the Routes and the Form Validation (Cats).









