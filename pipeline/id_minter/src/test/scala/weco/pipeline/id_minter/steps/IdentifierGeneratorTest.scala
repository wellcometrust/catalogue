package weco.pipeline.id_minter.steps

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inspectors, OptionValues}
import scalikejdbc._
import weco.fixtures.TestWith
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.pipeline.id_minter.config.models.IdentifiersTableConfig
import weco.pipeline.id_minter.database.IdentifiersDao
import weco.pipeline.id_minter.fixtures.IdentifiersDatabase
import weco.pipeline.id_minter.models.{Identifier, IdentifiersTable}

import scala.util.{Failure, Success, Try}

class IdentifierGeneratorTest
    extends AnyFunSpec
    with IdentifiersDatabase
    with Matchers
    with Inspectors
    with OptionValues
    with IdentifiersGenerators {

  def withIdentifierGenerator[R](maybeIdentifiersDao: Option[IdentifiersDao] =
                                   None,
                                 existingDaoEntries: Seq[Identifier] = Nil)(
    testWith: TestWith[(IdentifierGenerator, IdentifiersTable), R]): R =
    withIdentifiersDao(existingDaoEntries) {
      case (identifiersDao, table) =>
        val identifierGenerator = maybeIdentifiersDao match {
          case Some(dao) => new IdentifierGenerator(dao)
          case None      => new IdentifierGenerator(identifiersDao)
        }

        testWith((identifierGenerator, table))
    }

  it("queries the database and returns matching canonical IDs") {
    val sourceIdentifiers = (1 to 5).map(_ => createSourceIdentifier).toList
    val canonicalIds = (1 to 5).map(_ => createCanonicalId).toList
    val existingEntries = (sourceIdentifiers zip canonicalIds).map {
      case (sourceId, canonicalId) =>
        Identifier(
          canonicalId,
          sourceId
        )
    }
    withIdentifierGenerator(existingDaoEntries = existingEntries) {
      case (identifierGenerator, _) =>
        val triedIds =
          identifierGenerator.retrieveOrGenerateCanonicalIds(sourceIdentifiers)

        triedIds shouldBe a[Success[_]]
        val idsMap = triedIds.get
        forAll(sourceIdentifiers zip canonicalIds) {
          case (sourceId, canonicalId) =>
            idsMap.get(sourceId).value.CanonicalId should be(canonicalId)
        }
    }
  }

  it("generates and saves new identifiers") {
    val sourceIdentifiers = (1 to 5).map(_ => createSourceIdentifier).toList

    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = NamedAutoSession('primary)

        val triedIds = identifierGenerator.retrieveOrGenerateCanonicalIds(
          sourceIdentifiers
        )

        triedIds shouldBe a[Success[_]]

        val ids = triedIds.get
        ids.size shouldBe sourceIdentifiers.length

        val i = identifiersTable.i
        val maybeIdentifiers = withSQL {
          select
            .from(identifiersTable as i)
            .where
            .in(i.SourceId, sourceIdentifiers.map(_.value))
        }.map(Identifier(i)).list.apply()

        maybeIdentifiers should have length sourceIdentifiers.length
        maybeIdentifiers should contain theSameElementsAs
          sourceIdentifiers.flatMap(ids.get)
    }
  }

  it("returns a failure if it fails registering new identifiers") {
    val config = IdentifiersTableConfig(
      database = createDatabaseName,
      tableName = createTableName
    )

    val saveException = new Exception("Don't do that please!")

    val identifiersDao = new IdentifiersDao(
      identifiers = new IdentifiersTable(config)
    ) {
      override def lookupIds(sourceIdentifiers: Seq[SourceIdentifier])(
        implicit session: DBSession
      ): Try[IdentifiersDao.LookupResult] =
        Success(
          IdentifiersDao.LookupResult(
            existingIdentifiers = Map.empty,
            unmintedIdentifiers = sourceIdentifiers.toList
          )
        )

      override def saveIdentifiers(ids: List[Identifier])(
        implicit session: DBSession
      ): Try[IdentifiersDao.InsertResult] =
        Failure(
          IdentifiersDao.InsertError(Nil, saveException, Nil)
        )
    }

    val sourceIdentifiers = (1 to 5).map(_ => createSourceIdentifier).toList

    withIdentifierGenerator(Some(identifiersDao)) {
      case (identifierGenerator, _) =>
        val triedGeneratingIds =
          identifierGenerator.retrieveOrGenerateCanonicalIds(
            sourceIdentifiers
          )

        triedGeneratingIds shouldBe a[Failure[_]]
        triedGeneratingIds.failed.get
          .asInstanceOf[IdentifiersDao.InsertError]
          .e shouldBe saveException
    }
  }

  it("preserves the ontologyType when generating new identifiers") {
    withIdentifierGenerator() {
      case (identifierGenerator, identifiersTable) =>
        implicit val session = NamedAutoSession('primary)

        val sourceIdentifier = createSourceIdentifierWith(
          ontologyType = "Item"
        )

        val triedId = identifierGenerator.retrieveOrGenerateCanonicalIds(
          List(sourceIdentifier)
        )

        val id = triedId.get.values.head.CanonicalId
        id.underlying should not be empty

        val i = identifiersTable.i
        val maybeIdentifier = withSQL {

          select
            .from(identifiersTable as i)
            .where
            .eq(i.SourceId, sourceIdentifier.value)

        }.map(Identifier(i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(
          canonicalId = id,
          sourceIdentifier = sourceIdentifier
        )
    }
  }
}
