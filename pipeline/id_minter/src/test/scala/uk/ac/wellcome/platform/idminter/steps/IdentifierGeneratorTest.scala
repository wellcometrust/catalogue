package uk.ac.wellcome.platform.idminter.steps

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.storage.store.memory.MemoryStore

import scala.util.{Failure, Success}

class IdentifierGeneratorTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with IdentifiersGenerators {

  def withIdentifierGenerator[R](maybeIdentifiersDao: Option[IdentifiersDao[MemoryStore[SourceIdentifier, Identifier]]] = None)(
    testWith: TestWith[IdentifierGenerator[MemoryStore[SourceIdentifier, Identifier]], R]): R = {
      val memoryStore = new MemoryStore[SourceIdentifier, Identifier](Map.empty)

      val identifiersDao = maybeIdentifiersDao.getOrElse(
        new IdentifiersDao(memoryStore)
      )

      val identifierGenerator = new IdentifierGenerator(identifiersDao)

      testWith(identifierGenerator)
    }

  it("queries the database and return a matching canonical id") {
    val sourceIdentifier = createSourceIdentifier
    val canonicalId = createCanonicalId

    withIdentifierGenerator() { identifierGenerator =>

        // TODO: insert id into db

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        triedId shouldBe Success(canonicalId)
    }
  }

  it("generates and saves a new identifier") {
    val sourceIdentifier = createSourceIdentifier

    withIdentifierGenerator() { identifierGenerator =>
        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        triedId shouldBe a[Success[_]]

        val id = triedId.get
        id should not be empty

        true shouldBe false
        // TODO: introspect store to ensure saved
    }
  }

  it("returns a failure if it fails registering a new identifier") {
    val identifiersDao = mock[IdentifiersDao[MemoryStore[SourceIdentifier, Identifier]]]

    val sourceIdentifier = createSourceIdentifier

    val triedLookup = identifiersDao.lookupId(
      sourceIdentifier = sourceIdentifier
    )

    when(triedLookup)
      .thenReturn(Success(None))

    val expectedException = new Exception("Noooo")

    val whenThing = identifiersDao.saveIdentifier(
      org.mockito.Matchers.eq(sourceIdentifier), any[Identifier]()
    )

    when(whenThing).thenReturn(Failure(expectedException))

    withIdentifierGenerator(Some(identifiersDao)) { identifierGenerator =>
        val triedGeneratingId =
          identifierGenerator.retrieveOrGenerateCanonicalId(
            sourceIdentifier
          )

        triedGeneratingId shouldBe a[Failure[_]]
        triedGeneratingId.failed.get shouldBe expectedException
    }
  }

  it("preserves the ontologyType when generating a new identifier") {
    withIdentifierGenerator() { identifierGenerator =>
        val sourceIdentifier = createSourceIdentifierWith(
          ontologyType = "Item"
        )

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )

        val id = triedId.get
        id should not be (empty)

        true shouldBe false
        // TODO: introspect store to ensure saved
    }
  }
}
