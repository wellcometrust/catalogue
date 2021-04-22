package uk.ac.wellcome.platform.transformer.sierra.transformers

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.transformer.sierra.source.SierraOrderData
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.{LocationType, PhysicalLocation}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.source_model.sierra.{
  SierraOrderNumber,
  TypedSierraRecordNumber
}

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Try

/** This transformer creates catalogue items that correspond to items that
  * are "on order" or "awaiting cataloguing" -- which don't have their own
  * item record yet.
  *
  *
  * To understand how this works, it's useful to understand the ordering
  * process:
  *
  *   1)  A staff member orders a book.  They create a skeleton bib record
  *       in Sierra, which is linked to an order record with status 'o' ("on order").
  *       At this point, there are no item records.
  *
  *   2)  When the book arrives, it's "received".  The RDATE on the order
  *       record gets populated, the status is updated to 'a' ("fully paid"),
  *       and invoice information gets attached to the order record.
  *
  *   3)  At some point after that, an item record is created.  This supercedes
  *       the order record.
  *
  * The Sierra documentation for fixed fields on order records is useful reading:
  * https://documentation.iii.com/sierrahelp/Default.htm#sril/sril_records_fixed_field_types_order.html%3FTocPath%3DSierra%2520Reference%7CHow%2520Innovative%2520Systems%2520Store%2520Information%7CFixed-length%2520Fields%7C_____11
  *
  */
object SierraItemsOnOrder extends Logging {
  def apply(
    id: TypedSierraRecordNumber,
    hasItems: Boolean,
    orderDataMap: Map[SierraOrderNumber, SierraOrderData]
  ): List[Item[IdState.Unidentifiable.type]] =
    if (!hasItems) {
      orderDataMap.toList
        .filterNot {
          case (_, orderData) => orderData.suppressed || orderData.deleted
        }
        .sortBy { case (id, _) => id.withoutCheckDigit }
        .flatMap { case (_, orderData) => createItem(id, orderData) }
    } else {
      List()
    }

  private def createItem(
    id: TypedSierraRecordNumber,
    order: SierraOrderData): Option[Item[IdState.Unidentifiable.type]] =
    (
      getStatus(order),
      getOrderDate(order),
      getReceivedDate(order),
      getCopies(order)) match {

      // status 'o' = "On order"
      //
      // We create an item with a message something like "1 copy ordered for Wellcome Collection on 1 Jan 2001"
      case (Some(status), orderedDate, _, copies) if status == "o" =>
        Some(
          Item(
            title = None,
            locations = List(
              PhysicalLocation(
                locationType = LocationType.OnOrder,
                label = createOnOrderMessage(orderedDate, copies)
              )
            )
          )
        )

      // status 'a' = "Fully paid", RDATE = "when we actually received the thing"
      //
      // We create an item with a message like "Awaiting cataloguing for Wellcome Collection"
      // We don't expose the received date publicly (in case an item has been in the queue
      // for a long time) -- but we do expect it to be there for these records.
      case (Some(status), _, receivedDate, copies)
          if status == "a" && receivedDate.isDefined =>
        Some(
          Item(
            title = None,
            locations = List(
              PhysicalLocation(
                locationType = LocationType.OnOrder,
                label = createAwaitingCataloguingMessage(copies)
              )
            )
          )
        )

      // We're deliberately quite conservative here -- if we're not sure what an order
      // means, we ignore it.  I don't know how many orders this will affect, and how many
      // will be ignored because they're suppressed/there are other items.
      case (Some(status), _, receivedDate, _)
          if status == "a" && receivedDate.isEmpty =>
        warn(
          s"${id.withCheckDigit}: order has STATUS 'a' (fully paid) but no RDATE.  Where is this item?")
        None

      case (status, _, _, _) =>
        warn(
          s"${id.withCheckDigit}: order has unrecognised STATUS $status.  How do we handle it?")
        None
    }

  // Fixed field 20 = STATUS
  private def getStatus(order: SierraOrderData): Option[String] =
    order.fixedFields.get("20").map { _.value }

  private val marcDateFormat = new SimpleDateFormat("yyyy-MM-dd")

  // Fixed field 13 = ODATE.  This is usually a date in the form YYYY-MM-DD.
  private def getOrderDate(order: SierraOrderData): Option[Date] =
    order.fixedFields
      .get("13")
      .map { _.value }
      .flatMap { d =>
        Try(marcDateFormat.parse(d)).toOption
      }

  // Fixed field 17 = RDATE.  This is usually a date in the form YYYY-MM-DD.
  private def getReceivedDate(order: SierraOrderData): Option[Date] =
    order.fixedFields
      .get("17")
      .map { _.value }
      .flatMap { d =>
        Try(marcDateFormat.parse(d)).toOption
      }

  // Fixed field 5 = COPIES
  private def getCopies(order: SierraOrderData): Option[Int] =
    order.fixedFields
      .get("5")
      .map { _.value }
      .flatMap { c =>
        Try(c.toInt).toOption
      }

  private val displayFormat = new SimpleDateFormat("d MMMM yyyy")

  private def createOnOrderMessage(maybeOrderedDate: Option[Date],
                                   maybeCopies: Option[Int]): String =
    (maybeOrderedDate, maybeCopies) match {
      case (Some(orderedDate), Some(copies)) =>
        s"$copies ${if (copies == 1) "copy" else "copies"} ordered for Wellcome Collection on ${displayFormat
          .format(orderedDate)}"

      case (None, Some(copies)) =>
        s"$copies ${if (copies == 1) "copy" else "copies"} ordered for Wellcome Collection"

      case (Some(orderedDate), None) =>
        s"Ordered for Wellcome Collection on ${displayFormat.format(orderedDate)}"

      case (None, None) =>
        "Ordered for Wellcome Collection"
    }

  private def createAwaitingCataloguingMessage(
    maybeCopies: Option[Int]): String =
    maybeCopies match {
      case Some(copies) if copies == 1 =>
        "1 copy awaiting cataloguing for Wellcome Collection"

      case Some(copies) =>
        s"$copies copies awaiting cataloguing for Wellcome Collection"

      case _ =>
        s"Awaiting cataloguing for Wellcome Collection"
    }
}