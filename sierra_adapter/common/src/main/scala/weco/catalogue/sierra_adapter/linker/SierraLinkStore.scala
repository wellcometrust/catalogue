package weco.catalogue.sierra_adapter.linker

import uk.ac.wellcome.sierra_adapter.model.{AbstractSierraRecord, SierraRecordNumber}
import uk.ac.wellcome.storage.{Identified, UpdateNotApplied}
import uk.ac.wellcome.storage.store.VersionedStore

trait SierraLinkStore[Id <: SierraRecordNumber, Record <: AbstractSierraRecord[Id], Link] {
  val store: VersionedStore[Id, Int, Link]
  val linker: SierraLinker[Record, Link]

  def createNewLink(record: Record): Link
  def updateRecord(record: Record, updatedLink: Link): Record

  def update(newRecord: Record): Either[Throwable, Option[Record]] = {
    val newLink = createNewLink(newRecord)

    val upsertResult: store.UpdateEither =
      store.upsert(newRecord.id)(newLink) { existingLink =>
        linker.mergeLinks(existingLink, newRecord) match {
          case Some(updatedLink) => Right(updatedLink)
          case None =>
            Left(
              UpdateNotApplied(
                new Throwable(s"Item ${newRecord.id} is already up-to-date")))
        }
      }

    upsertResult match {
      case Right(Identified(_, updatedLink)) =>
        Right(Some(updateRecord(newRecord, updatedLink)))

      case Left(_: UpdateNotApplied) => Right(None)
      case Left(err)                 => Left(err.e)
    }
  }
}
