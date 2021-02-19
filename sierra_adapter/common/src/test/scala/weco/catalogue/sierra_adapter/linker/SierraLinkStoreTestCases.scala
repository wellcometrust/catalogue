package weco.catalogue.sierra_adapter.linker

import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.sierra_adapter.model.{
  AbstractSierraRecord,
  SierraBibNumber,
  SierraGenerators,
  SierraTypedRecordNumber
}
import uk.ac.wellcome.storage.{
  Identified,
  StoreWriteError,
  UpdateWriteError,
  Version
}
import uk.ac.wellcome.storage.maxima.memory.MemoryMaxima
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.storage.store.memory.{MemoryStore, MemoryVersionedStore}

import java.time.Instant

trait SierraLinkStoreTestCases[Id <: SierraTypedRecordNumber, Record <: AbstractSierraRecord[Id], Link <: SierraLink]
  extends AnyFunSpec
    with Matchers
    with EitherValues
    with SierraGenerators {
  val linker: SierraLinker[Record, Link]

  def withLinkStore[R](
    store: VersionedStore[Id, Int, Link]
  )(testWith: TestWith[SierraLinkStore[Id, Record, Link], R]): R

  def createStoreWith(initialEntries: Map[Version[Id, Int], Link]): VersionedStore[Id, Int, Link] =
    MemoryVersionedStore[Id, Link](initialEntries = initialEntries)

  def createStore: VersionedStore[Id, Int, Link] =
    createStoreWith(initialEntries = Map.empty)

  def createId: Id

  def createRecord: Record =
    createRecordWith(
      id = createId,
      bibIds = createSierraBibNumbers(count = randomInt(from = 0, to = 5)),
      modifiedDate = randomInstant
    )

  def createLinkWith(unlinkedBibIds: List[SierraBibNumber]): Link

  def createRecordWith(id: Id, bibIds: List[SierraBibNumber], modifiedDate: Instant): Record

  def createRecordWith(id: Id, modifiedDate: Instant): Record =
    createRecordWith(
      id = id,
      bibIds = createSierraBibNumbers(count = randomInt(from = 0, to = 5)),
      modifiedDate = modifiedDate
    )

  def getBibIds(record: Record): List[SierraBibNumber]
  def getUnlinkedBibIds(record: Record): List[SierraBibNumber]

  describe("behaves as a SierraLinkStore") {
    it("stores a new record") {
      val store = createStore

      val record = createRecord

      withLinkStore(store) {
        _.update(record).value shouldBe Some(record)
      }

      store.getLatest(record.id).value.identifiedT shouldBe linker.createNewLink(record)
    }

    it("does not overwrite new data with old data") {
      val id = createId

      val newRecord = createRecordWith(id = id, modifiedDate = newerDate)
      val oldRecord = createRecordWith(id = id, modifiedDate = olderDate)

      val newLink = linker.createNewLink(newRecord)

      val store = createStoreWith(
        initialEntries = Map(Version(id, 1) -> newLink)
      )

      withLinkStore(store) {
        _.update(oldRecord) shouldBe Right(None)
      }

      store.getLatest(id).value shouldBe Identified(Version(id, 1), newLink)
    }

    it("overwrites old data with new data") {
      val id = createId

      val newRecord = createRecordWith(id = id, modifiedDate = newerDate)
      val oldRecord = createRecordWith(id = id, modifiedDate = olderDate)

      val oldLink = linker.createNewLink(oldRecord)
      val expectedLink = linker.mergeLinks(oldLink, newRecord).get

      val store = createStoreWith(
        initialEntries = Map(Version(id, 1) -> oldLink)
      )

      withLinkStore(store) {
        _.update(newRecord).value.get shouldBe linker.updateRecord(newRecord, expectedLink)
      }

      store.getLatest(id).value shouldBe Identified(Version(id, 2), expectedLink)
    }

    it("fails if the store returns an error") {
      val record = createRecord

      val exception = new RuntimeException("BOOM!")

      val brokenStore = new MemoryVersionedStore(
        new MemoryStore[Version[Id, Int], Link](initialEntries = Map.empty)
          with MemoryMaxima[Id, Link]
      ) {
        override def upsert(id: Id)(link: Link)(
          f: UpdateFunction): UpdateEither =
          Left(UpdateWriteError(StoreWriteError(exception)))
      }

      withLinkStore(brokenStore) {
        _.update(record) shouldBe Left(exception)
      }
    }

    it("records unlinked bibIds") {
      val oldBibIds = createSierraBibNumbers(count = 3)

      val unlinkedBibIds = List(oldBibIds.head)
      val newBibIds = oldBibIds.filterNot { unlinkedBibIds.contains }

      assert(newBibIds.size + unlinkedBibIds.size == oldBibIds.size)

      val id = createId

      val oldRecord = createRecordWith(id = id, bibIds = oldBibIds, modifiedDate = olderDate)
      val newRecord = createRecordWith(id = id, bibIds = newBibIds, modifiedDate = newerDate)

      val store = createStoreWith(
        initialEntries = Map(Version(id, 1) -> linker.createNewLink(oldRecord))
      )

      withLinkStore(store) { linkStore =>
        val updatedRecord = linkStore.update(newRecord).value.get

        updatedRecord.id shouldBe newRecord.id
        updatedRecord.data shouldBe newRecord.data

        getBibIds(updatedRecord) shouldBe newBibIds
        getUnlinkedBibIds(updatedRecord) shouldBe unlinkedBibIds
      }

      val storedLink = store.getLatest(id).value.identifiedT
      storedLink.bibIds shouldBe newBibIds
      storedLink.unlinkedBibIds shouldBe unlinkedBibIds
    }

    it("adds new bibIds and records unlinked bibIds in the same update") {
      val oldBibIds = createSierraBibNumbers(count = 4)

      val unlinkedBibIds = List(oldBibIds.head)
      val newBibIds = oldBibIds.filterNot { unlinkedBibIds.contains } ++ createSierraBibNumbers(count = 2)

      val id = createId

      val oldRecord = createRecordWith(id = id, modifiedDate = olderDate, bibIds = oldBibIds)
      val newRecord = createRecordWith(id = id, modifiedDate = newerDate, bibIds = newBibIds)

      val store = createStoreWith(
        initialEntries = Map(Version(id, 1) -> linker.createNewLink(oldRecord))
      )

      withLinkStore(store) { linkStore =>
        val updatedRecord = linkStore.update(newRecord).value.get

        updatedRecord.id shouldBe newRecord.id
        updatedRecord.data shouldBe newRecord.data

        getBibIds(updatedRecord) shouldBe newBibIds
        getUnlinkedBibIds(updatedRecord) shouldBe unlinkedBibIds
      }

      val storedLink = store.getLatest(id).value.identifiedT
      storedLink.bibIds shouldBe newBibIds
      storedLink.unlinkedBibIds shouldBe unlinkedBibIds
    }

    it("preserves the existing unlinked bibIds") {
      val unlinkedBibIds = createSierraBibNumbers(count = 3)

      val record = createRecordWith(
        id = createId,
        bibIds = List.empty,
        modifiedDate = Instant.parse("2099-12-12T12:12:12Z")
      )

      val store = createStoreWith(
        initialEntries = Map(Version(record.id, 1) -> createLinkWith(unlinkedBibIds = unlinkedBibIds))
      )

      withLinkStore(store) { linkStore =>
        val updatedRecord = linkStore.update(record).value.get

        updatedRecord.id shouldBe record.id
        updatedRecord.data shouldBe record.data

        getBibIds(updatedRecord) shouldBe getBibIds(record)
        getUnlinkedBibIds(updatedRecord) shouldBe unlinkedBibIds
      }

      val storedLink = store.getLatest(record.id).value.identifiedT
      storedLink.bibIds shouldBe getBibIds(record)
      storedLink.unlinkedBibIds shouldBe unlinkedBibIds
    }
  }
}
