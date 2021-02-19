package weco.catalogue.sierra_item_linker.linker

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.sierra_adapter.model.{
  SierraBibNumber,
  SierraItemNumber,
  SierraItemRecord
}
import uk.ac.wellcome.storage.store.VersionedStore
import weco.catalogue.sierra_adapter.linker.{
  SierraLinkStore,
  SierraLinkStoreTestCases,
  SierraLinker
}

import java.time.Instant

class SierraItemLinkStoreTest
    extends SierraLinkStoreTestCases[SierraItemNumber, SierraItemRecord, SierraItemLink] {
  val linker: SierraLinker[SierraItemRecord, SierraItemLink] =
    SierraItemLinker

  override def withLinkStore[R](
    store: VersionedStore[SierraItemNumber, Int, SierraItemLink]
  )(
    testWith: TestWith[SierraLinkStore[SierraItemNumber, SierraItemRecord, SierraItemLink], R]
  ): R =
    testWith(
      new SierraItemLinkStore(store)
    )

  override def createId: SierraItemNumber =
    createSierraItemNumber

  override def createRecordWith(id: SierraItemNumber, bibIds: List[SierraBibNumber], modifiedDate: Instant): SierraItemRecord =
    createSierraItemRecordWith(
      id = id,
      bibIds = bibIds,
      modifiedDate = modifiedDate
    )

  override def getBibIds(record: SierraItemRecord): List[SierraBibNumber] =
    record.bibIds

  override def getUnlinkedBibIds(record: SierraItemRecord): List[SierraBibNumber] =
    record.unlinkedBibIds

  override def createLinkWith(unlinkedBibIds: List[SierraBibNumber]): SierraItemLink =
    SierraItemLink(
      bibIds = List.empty,
      unlinkedBibIds = unlinkedBibIds,
      modifiedDate = Instant.now()
    )
}
