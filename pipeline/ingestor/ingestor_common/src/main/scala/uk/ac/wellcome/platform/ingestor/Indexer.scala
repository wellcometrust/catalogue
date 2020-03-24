package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.bulk.{BulkResponse, BulkResponseItem}
import com.sksamuel.elastic4s.requests.common.VersionType.ExternalGte
import com.sksamuel.elastic4s.{ElasticClient, Index, Indexable, Response}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

trait CanonicalId[T]{
  def canonicalId(t: T): String
}

trait Indexer[T] extends Logging {
  val elasticClient: ElasticClient

  implicit val ec: ExecutionContext
  implicit val indexable: Indexable[T]
  implicit val id: CanonicalId[T]

  final def index(documents: Seq[T], index: Index)
    : Future[Either[Seq[T], Seq[T]]] = {

    debug(s"Indexing ${documents.map(d => id.canonicalId(d)).mkString(", ")}")

    val inserts = documents.map { document =>
      indexInto(index.name)
        .version(calculateEsVersion(document))
        .versionType(ExternalGte)
        .id(id.canonicalId(document))
        .doc(document)
    }

    elasticClient
      .execute {
        bulk(inserts)
      }
      .map { response: Response[BulkResponse] =>
        if (response.isError) {
          warn(s"Error from Elasticsearch: $response")
          Left(documents)
        } else {
          debug(s"Bulk response = $response")
          val bulkResponse = response.result
          val actualFailures = bulkResponse.failures.filterNot {
            isVersionConflictException
          }

          if (actualFailures.nonEmpty) {
            val failedIds = actualFailures.map { failure =>
              error(s"Failed ingesting ${failure.id}: ${failure.error}")
              failure.id
            }

            Left(documents.filter(d => {
              failedIds.contains(id.canonicalId(d))
            }))
          } else Right(documents)
        }
      }
  }

  def calculateEsVersion(t: T): Int

  /** Did we try to PUT a document with a lower version than the existing version?
    *
    */
  private def isVersionConflictException(
    bulkResponseItem: BulkResponseItem): Boolean = {
    // This error is returned by Elasticsearch when we try to PUT a document
    // with a lower version than the existing version.
    val alreadyIndexedHasHigherVersion = bulkResponseItem.error
      .exists(bulkError =>
        bulkError.`type`.contains("version_conflict_engine_exception"))

    if (alreadyIndexedHasHigherVersion) {
      info(
        s"Skipping ${bulkResponseItem.id} because already indexed item has a higher version (${bulkResponseItem.error}")
    }

    alreadyIndexedHasHigherVersion
  }

}
