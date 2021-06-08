package weco.catalogue.source_model.sierra.rules

import grizzled.slf4j.Logging
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus,
  ItemStatus,
  LocationType,
  PhysicalLocationType
}
import weco.catalogue.source_model.sierra.{
  SierraBibNumber,
  SierraItemData,
  SierraItemNumber
}
import weco.catalogue.source_model.sierra.source.{
  OpacMsg,
  SierraQueryOps,
  Status
}

/** There are multiple sources of truth for item information in Sierra, and whether
  * a given item can be requested online.
  *
  * This object tries to create a single, consistent view of this data.
  * It returns two values:
  *
  *   - An access condition that can be added to a location on an Item.
  *     This would be set in the Catalogue API.
  *   - An ItemStatus that returns a simpler "is this available right now".
  *     This would be returned from the items API with the most up-to-date
  *     data from Sierra.
  *
  */
object SierraItemAccess extends SierraQueryOps with Logging {
  def apply(
    bibId: SierraBibNumber,
    itemId: SierraItemNumber,
    bibStatus: Option[AccessStatus],
    location: Option[PhysicalLocationType],
    itemData: SierraItemData
  ): (Option[AccessCondition], ItemStatus) = {
    val holdCount = itemData.holdCount
    val status = itemData.status
    val opacmsg = itemData.opacmsg
    val isRequestable = SierraRulesForRequesting(itemData)

    (bibStatus, holdCount, status, opacmsg, isRequestable, location) match {

      // Items in the closed stores that are requestable get the "Online request" condition.
      //
      // Example: b18799966 / i17571170
      case (
          bibStatus,
          Some(0),
          Some(Status.Available),
          Some(OpacMsg.OnlineRequest),
          Requestable,
          Some(LocationType.ClosedStores))
          if bibStatus.isEmpty || bibStatus.contains(AccessStatus.Open) =>
        val ac = createAccessCondition(
          method = Some(AccessMethod.OnlineRequest),
          status = bibStatus,
          note = itemData.displayNote
        )

        (Some(ac), ItemStatus.Available)

      // Note: it is possible for individual items within a restricted bib to be available
      // online, e.g. in archives.  The "restricted" on the bib applies to the archive as
      // a whole, but individual files may be open.
      //
      // Consider an archive with three items:
      //
      //      Item 1 = Open
      //      Item 2 = contains sensitive material, so restricted
      //      Item 3 = Open
      //
      // Then the top-level bib status would be "certain restrictions apply" for the
      // archive as a whole, referring to item 2 -- but items 1 and 3 would be open.
      //
      // This is distinct from the case above because we want to replace the bib-level
      // status with "Open", rather than pass it through.
      //
      // Example: b1842941 / i17286803
      case (
          Some(AccessStatus.Restricted),
          Some(0),
          Some(Status.Available),
          Some(OpacMsg.OnlineRequest),
          Requestable,
          Some(LocationType.ClosedStores)) =>
        val ac = createAccessCondition(
          method = Some(AccessMethod.OnlineRequest),
          status = Some(AccessStatus.Open),
          note = itemData.displayNote
        )

        (Some(ac), ItemStatus.Available)

      // Items on the open shelves don't have any access conditions.
      //
      // We could add an access status of "Open" here, but it feels dubious to be
      // synthesising access information that doesn't come from the source records.
      //
      // Note: We create an AccessCondition here so we can carry information from the
      // display note.  This is used sparingly, but occasionally contains useful information
      // for readers, e.g.
      //
      //      Shelved at the end of the Quick Ref. section with the oversize Quick Ref. books.
      //
      // Example: b1659504x / i15894897
      case (
          None,
          Some(0),
          Some(Status.Available),
          Some(OpacMsg.OpenShelves),
          NotRequestable.OnOpenShelves(_),
          Some(LocationType.OpenShelves)) =>
        val ac = itemData.displayNote.map { note =>
          createAccessCondition(note = Some(note))
        }
        (ac, ItemStatus.Available)

      // There are some items that are labelled "bound in above" or "contained in above".
      //
      // These items aren't requestable on their own; you have to request the "primary" item.
      case (None, _, _, _, NotRequestable.RequestTopItem(message), _) =>
        (
          Some(
            createAccessCondition(
              method = Some(AccessMethod.NotRequestable),
              terms = Some(message),
              note = itemData.displayNote)),
          ItemStatus.Unavailable)

      // Handle any cases that require a manual request.
      //
      // Example: b32214832 / i19389383
      case (
          None,
          Some(0),
          Some(Status.Available),
          Some(OpacMsg.ManualRequest),
          NotRequestable.NeedsManualRequest(_),
          Some(LocationType.ClosedStores)) =>
        (
          Some(
            createAccessCondition(
              method = Some(AccessMethod.ManualRequest),
              note = itemData.displayNote)),
          ItemStatus.Available)

      // Handle any cases where the item is closed.
      //
      // We don't show the text from rules for requesting -- it's not saying anything
      // that you can't work out from the AccessStatus.
      //
      // Examples: b20657365 / i18576503, b1899457x / i17720734
      case (
          Some(AccessStatus.Closed),
          _,
          Some(Status.Closed),
          Some(OpacMsg.Unavailable),
          NotRequestable.ItemClosed(_),
          locationType)
          if locationType.isEmpty || locationType.contains(
            LocationType.ClosedStores) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.Closed),
              note = itemData.displayNote)),
          ItemStatus.Unavailable)

      // Handle any cases where the item is explicitly unavailable.
      case (
          None,
          _,
          Some(Status.Unavailable),
          Some(OpacMsg.Unavailable),
          NotRequestable.ItemUnavailable(_),
          _) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.Unavailable),
              note = itemData.displayNote)),
          ItemStatus.Unavailable)

      case (
          None,
          _,
          Some(Status.Unavailable),
          Some(OpacMsg.AtDigitisation),
          NotRequestable.ItemUnavailable(_),
          _) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.TemporarilyUnavailable),
              terms = Some(
                "This item is being digitised and is currently unavailable."),
              note = itemData.displayNote)),
          ItemStatus.TemporarilyUnavailable)

      // An item which is restricted can be requested online -- the user will have to fill in
      // any paperwork when they actually visit the library.
      //
      // Example: b29459126 / i19023340
      case (
          Some(AccessStatus.Restricted),
          Some(0),
          Some(Status.Restricted),
          Some(OpacMsg.OnlineRequest),
          Requestable,
          Some(LocationType.ClosedStores)) =>
        (
          Some(
            createAccessCondition(
              method = Some(AccessMethod.OnlineRequest),
              status = Some(AccessStatus.Restricted),
              note = itemData.displayNote)),
          ItemStatus.Available)

      // The status "by appointment" takes precedence over "permission required".
      //
      // Examples: b32214832 / i19389383, b16576111 / 15862409
      case (
          bibStatus,
          Some(0),
          Some(Status.PermissionRequired),
          Some(OpacMsg.ByAppointment),
          NotRequestable.NoReason,
          Some(LocationType.ClosedStores))
          if bibStatus.isEmpty || bibStatus.contains(AccessStatus.ByAppointment) || bibStatus
            .contains(AccessStatus.PermissionRequired) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.ByAppointment),
              note = itemData.displayNote)),
          ItemStatus.Available)

      case (
          bibStatus,
          Some(0),
          Some(Status.PermissionRequired),
          Some(OpacMsg.DonorPermission),
          _: NotRequestable,
          Some(LocationType.ClosedStores))
          if bibStatus.isEmpty || bibStatus.contains(
            AccessStatus.PermissionRequired) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.PermissionRequired),
              note = itemData.displayNote)),
          ItemStatus.Available)

      // A missing status overrides all other values.
      //
      // Example: b10379198 / i10443861
      case (
          _,
          _,
          Some(Status.Missing),
          _,
          NotRequestable.ItemMissing(message),
          _) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.Unavailable),
              terms = Some(message),
              note = itemData.displayNote)),
          ItemStatus.Unavailable)

      // A withdrawn status also overrides all other values.
      case (
          _,
          _,
          Some(Status.Withdrawn),
          _,
          NotRequestable.ItemWithdrawn(message),
          _) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.Unavailable),
              terms = Some(message),
              note = itemData.displayNote)),
          ItemStatus.Unavailable)

      // If an item is on hold for another reader, it can't be requested -- even
      // if it would ordinarily be requestable.
      //
      // Note that an item on hold goes through two stages:
      //  1. A reader places a hold, but the item is still in the store.
      //     The status is still "-" (Available)
      //  2. A staff member collects the item from the store, and places it on the holdshelf
      //     Then the status becomes "!" (On holdshelf)
      //
      case (
          None,
          Some(holdCount),
          _,
          _,
          Requestable,
          Some(LocationType.ClosedStores)) if holdCount > 0 =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.TemporarilyUnavailable),
              terms = Some(
                "Item is in use by another reader. Please ask at Enquiry Desk."),
              note = itemData.displayNote
            )),
          ItemStatus.TemporarilyUnavailable)

      case (
          None,
          _,
          _,
          _,
          NotRequestable.OnHold(_),
          Some(LocationType.ClosedStores)) =>
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.TemporarilyUnavailable),
              terms = Some(
                "Item is in use by another reader. Please ask at Enquiry Desk."),
              note = itemData.displayNote
            )),
          ItemStatus.TemporarilyUnavailable)

      // If we can't work out how this item should be handled, then let's mark it
      // as unavailable for now.
      //
      // TODO: We should work with the Collections team to better handle any records
      // that are hitting this branch.  Sending readers to Encore isn't a long-term
      // solution.  Remove this link when Encore goes away.
      //
      // Note: once you remove the link, you can also remove the bibId passed into
      // this apply() method.
      case (bibStatus, holdCount, status, opacmsg, isRequestable, location) =>
        warn(
          s"Unable to assign access status for item ${itemId.withCheckDigit}: " +
            s"bibStatus=$bibStatus, holdCount=$holdCount, status=$status, " +
            s"opacmsg=$opacmsg, isRequestable=$isRequestable, location=$location"
        )
        (
          Some(
            createAccessCondition(
              status = Some(AccessStatus.TemporarilyUnavailable),
              note = Some(
                s"""Please check this item <a href="https://search.wellcomelibrary.org/iii/encore/record/C__Rb${bibId.withoutCheckDigit}?lang=eng">on the Wellcome Library website</a> for access information""")
            )
          ),
          ItemStatus.Unavailable)
    }
  }

  implicit class ItemDataAccessOps(itemData: SierraItemData) {
    def status: Option[String] =
      itemData.fixedFields.get("88").map { _.value.trim }

    def opacmsg: Option[String] =
      itemData.fixedFields.get("108").map { _.value.trim }
  }

  private def createAccessCondition(
    method: Option[AccessMethod] = None,
    status: Option[AccessStatus] = None,
    terms: Option[String] = None,
    note: Option[String]
  ): AccessCondition = {
    val (accessTerms, accessNote) =
      (terms, note) match {
        case (None, Some(note)) if note.containsAccessTerms =>
          (Some(note), None)

        case (Some(terms), Some(note)) if terms == note =>
          (Some(terms), None)

        case other => other
      }

    AccessCondition(
      method = method,
      status = status,
      terms = accessTerms,
      note = accessNote
    )
  }

  // The display note field has been used for multiple purposes, in particular:
  //
  //  1) Distinguishing between different copies of an item, so people know
  //     which item to request, e.g. "impression lacking lettering"
  //  2) Recording information about how to access the item, e.g. "please email us"
  //
  // This method uses a few heuristics to guess whether a given note is actually information
  // about access that we should copy to the "terms" field.
  private implicit class NoteStringOps(s: String) {
    def containsAccessTerms: Boolean =
      s.toLowerCase.contains("unavailable") ||
        s.toLowerCase.contains("access") ||
        s.toLowerCase.contains("please contact") ||
        s.toLowerCase.contains("@wellcomecollection.org") ||
        s == "Offsite"
  }
}