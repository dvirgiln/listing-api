package com.david.listing

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.david.listing.domain.Domain._
import com.david.item.StoreService

object PropertyActor {
  final case object GetProperties
  final case class CreateProperty(property: Property)
  final case class UpdateProperty(property: Property)
  final case class GetProperty(id: String)
  final case class DeleteProperty(id: String)
  def props: Props = Props[PropertyActor]
}

/*
 * ListinService store class. Makes use of the generic store service.
 */
object ListingService extends StoreService[Property] {
  override def storeId(id: String, item: Property): Property = item.copy(listing = item.listing.copy(id = Some(id)))
}

class PropertyActor extends Actor with ActorLogging {

  import PropertyActor._
  import ListingService._

  def receive: Receive = {
    case GetProperties =>
      sender() ! getAll
    case CreateProperty(listing) =>
      sender() ! add(listing)
    case UpdateProperty(listing) =>
      val updated = update(listing)
      sender() ! updated
    case GetProperty(id) =>
      sender() ! get(id)
    case DeleteProperty(id) =>
      sender() ! remove(id)
  }
}