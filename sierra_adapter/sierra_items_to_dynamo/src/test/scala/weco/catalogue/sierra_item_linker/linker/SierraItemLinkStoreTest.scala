package weco.catalogue.sierra_item_linker.linker

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.sierra_items_to_dynamo.models.SierraItemLink
import uk.ac.wellcome.sierra_adapter.model.{SierraItemNumber, SierraItemRecord}
import uk.ac.wellcome.storage.store.VersionedStore
import weco.catalogue.sierra_adapter.linker.{SierraLinkStore, SierraLinkStoreTestCases, SierraLinker}

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

  override def createRecordWith(id: SierraItemNumber, modifiedDate: Instant): SierraItemRecord =
    createSierraItemRecordWith(
      id = id,
      bibIds = createSierraBibNumbers(count = randomInt(from = 0, to = 5)),
      modifiedDate = modifiedDate
    )
}
