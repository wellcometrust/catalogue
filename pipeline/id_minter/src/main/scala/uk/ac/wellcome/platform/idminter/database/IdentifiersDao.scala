package uk.ac.wellcome.platform.idminter.database

import java.sql.{
  BatchUpdateException,
  SQLIntegrityConstraintViolationException,
  Statement
}

import grizzled.slf4j.Logging
import scalikejdbc._
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.platform.idminter.exceptions.IdMinterException
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}

import scala.collection.mutable
import scala.concurrent.blocking
import scala.util.{Failure, Success, Try}

class IdentifiersDao(db: DB, identifiers: IdentifiersTable) extends Logging {
  case class LookupResult(found: Map[SourceIdentifier, Identifier],
                          notFound: List[SourceIdentifier])
  case class LookupError(e: Throwable)
  case class InsertError(failed: List[Identifier],
                         exception: Throwable,
                         succeeded: List[Identifier])
  case class InsertResult(succeeded: List[Identifier])

  implicit val session = AutoSession(db.settingsProvider)

  def lookupIds(sourceIdentifiers: Seq[SourceIdentifier])
    : Either[LookupError, LookupResult] = {
    val tr = Try {
      debug(s"Matching ($sourceIdentifiers)")
      val sqlParametersToSourceIdentifier =
        buildSqlQueryParameters(sourceIdentifiers)

      blocking {

        val i = identifiers.i
        val query = withSQL {

          select
            .from(identifiers as i)
            .where
            .in(
              (i.OntologyType, i.SourceSystem, i.SourceId),
              sqlParametersToSourceIdentifier.keys.toList)

        }.map(rs => {
            val foundParameter = buildSqlParametersFromResult(i, rs)
            sqlParametersToSourceIdentifier.remove(foundParameter) match {
              case Some(sourceIdentifier) =>
                (sourceIdentifier, Identifier(i)(rs))
              case None =>
                // this should be impossible in practice
                throw new RuntimeException(
                  "The values returned by the query could not be matched to a sourceIdentifier")
            }

          })
          .list()
        debug(s"Executing:'${query.statement}'")
        val result = query.apply().toMap

        LookupResult(result, sqlParametersToSourceIdentifier.values.toList)
      }
    }
    tr match {
      case Success(r) => Right(r)
      case Failure(e) => Left(LookupError(e))
    }
  }

  def saveIdentifiers(
    ids: List[Identifier]): Either[InsertError, InsertResult] = {
    val tried = Try {
      val values = ids.map(i =>
        Seq(i.CanonicalId, i.OntologyType, i.SourceSystem, i.SourceId))
      blocking {
        debug(s"Putting new identifier $ids")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(
              identifiers.column.CanonicalId -> sqls.?,
              identifiers.column.OntologyType -> sqls.?,
              identifiers.column.SourceSystem -> sqls.?,
              identifiers.column.SourceId -> sqls.?
            )
        }.batch(values: _*).apply()
        InsertResult(ids)
      }
    }
    tried match {
      case Success(r) => Right(r)
      case Failure(e) if e.isInstanceOf[BatchUpdateException] =>
        val exception = e.asInstanceOf[BatchUpdateException]
        val groupedResult = ids.zip(exception.getUpdateCounts).groupBy {
          case (_, Statement.EXECUTE_FAILED) => Statement.EXECUTE_FAILED
          case (_, _)                        => Statement.SUCCESS_NO_INFO
        }
        val failedIdentifiers =
          groupedResult.getOrElse(Statement.EXECUTE_FAILED, Nil).map {
            case (identifier, _) => identifier
          }
        val succeededIdentifiers =
          groupedResult.getOrElse(Statement.SUCCESS_NO_INFO, Nil).map {
            case (identifier, _) => identifier
          }
        Left(InsertError(failedIdentifiers, e, succeededIdentifiers))
      case Failure(exception) => Left(InsertError(ids, exception, Nil))
    }
  }

  private def buildSqlQueryParameters(
    sourceIdentifiers: Seq[SourceIdentifier]) = {
    mutable.Map(sourceIdentifiers.map(sourceIdentifier =>
      (buildSqlParametersFromSourceIdentifier(sourceIdentifier) -> sourceIdentifier)): _*)
  }

  private def buildSqlParametersFromSourceIdentifier(
    sourceIdentifier: SourceIdentifier) = {
    (
      sourceIdentifier.ontologyType,
      sourceIdentifier.identifierType.id,
      sourceIdentifier.value)
  }

  private def buildSqlParametersFromResult(i: SyntaxProvider[Identifier],
                                           rs: WrappedResultSet) = {
    (
      rs.string(i.resultName.OntologyType),
      rs.string(i.resultName.SourceSystem),
      rs.string(i.resultName.SourceId))
  }

  /* An unidentified record from the transformer can give us a list of
   * identifiers from the source systems, and an ontology type (e.g. "Work").
   *
   * This method looks for existing IDs that have matching ontology type and
   * source identifiers.
   */
  def lookupId(
    sourceIdentifier: SourceIdentifier
  ): Try[Option[Identifier]] = Try {

    val sourceSystem = sourceIdentifier.identifierType.id
    val sourceId = sourceIdentifier.value

    // TODO: handle gracefully, don't TryBackoff ad infinitum
    blocking {
      debug(s"Matching ($sourceIdentifier)")

      val i = identifiers.i
      val query = withSQL {
        select
          .from(identifiers as i)
          .where
          .eq(i.OntologyType, sourceIdentifier.ontologyType)
          .and
          .eq(i.SourceSystem, sourceSystem)
          .and
          .eq(i.SourceId, sourceId)

      }.map(Identifier(i)).single
      debug(s"Executing:'${query.statement}'")
      query.apply()
    }
  }

  /* Save an identifier into the database.
   *
   * Note that this will copy _all_ the fields on `Identifier`, nulling any
   * fields which aren't set on `Identifier`.
   */
  def saveIdentifier(identifier: Identifier): Try[Any] = {
    Try {
      blocking {
        debug(s"Putting new identifier $identifier")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(
              identifiers.column.CanonicalId -> identifier.CanonicalId,
              identifiers.column.OntologyType -> identifier.OntologyType,
              identifiers.column.SourceSystem -> identifier.SourceSystem,
              identifiers.column.SourceId -> identifier.SourceId
            )
        }.update().apply()
      }
    } recover {
      case e: SQLIntegrityConstraintViolationException =>
        warn(
          s"Unable to insert $identifier because of integrity constraints: ${e.getMessage}")
        throw IdMinterException(e)
      case e =>
        error(s"Failed inserting identifier $identifier in database", e)
        throw e
    }
  }
}
