package weco.catalogue.sierra_adapter.linker

import uk.ac.wellcome.sierra_adapter.model.{
  AbstractSierraRecord,
  SierraTypedRecordNumber
}
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.storage.{Identified, UpdateNotApplied}

class SierraLinkStore[Id <: SierraTypedRecordNumber, Record <: AbstractSierraRecord[Id], Link <: SierraLink](
  store: VersionedStore[Id, Int, Link],
  linker: SierraLinker[Record, Link]
) {
  def update(newRecord: Record): Either[Throwable, Option[Record]] = {
    val newLink = linker.createNewLink(newRecord)

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
        Right(Some(linker.updateRecord(newRecord, updatedLink)))

      case Left(_: UpdateNotApplied) => Right(None)
      case Left(err)                 => Left(err.e)
    }
  }
}
