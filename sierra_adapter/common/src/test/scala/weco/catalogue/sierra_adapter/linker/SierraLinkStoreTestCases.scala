package weco.catalogue.sierra_adapter.linker

import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.sierra_adapter.model.{AbstractSierraRecord, SierraGenerators, SierraTypedRecordNumber}
import uk.ac.wellcome.storage.{Identified, StoreWriteError, UpdateWriteError, Version}
import uk.ac.wellcome.storage.maxima.memory.MemoryMaxima
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.storage.store.memory.{MemoryStore, MemoryVersionedStore}

import java.time.Instant

trait SierraLinkStoreTestCases[Id <: SierraTypedRecordNumber, Record <: AbstractSierraRecord[Id], Link]
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
    createRecordWith(id = createId, modifiedDate = randomInstant)
  def createRecordWith(id: Id, modifiedDate: Instant): Record

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
  }
}
