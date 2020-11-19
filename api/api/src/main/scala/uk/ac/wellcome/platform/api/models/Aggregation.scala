package uk.ac.wellcome.platform.api.models

import scala.util.{Failure, Success, Try}
import io.circe.{Decoder, Json}
import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.sksamuel.elastic4s.requests.searches.aggs.responses.{
  Aggregations => Elastic4sAggregations
}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import grizzled.slf4j.Logging
import uk.ac.wellcome.display.models.LocationTypeQuery
import uk.ac.wellcome.models.marc.MarcLanguageCodeList
import uk.ac.wellcome.models.work.internal._

case class Aggregations(
  format: Option[Aggregation[Format]] = None,
  genres: Option[Aggregation[Genre[IdState.Minted]]] = None,
  productionDates: Option[Aggregation[Period[IdState.Minted]]] = None,
  languages: Option[Aggregation[Language]] = None,
  subjects: Option[Aggregation[Subject[IdState.Minted]]] = None,
  license: Option[Aggregation[License]] = None,
  locationType: Option[Aggregation[LocationTypeQuery]] = None,
)

object Aggregations extends Logging {

  def apply(searchResponse: SearchResponse): Option[Aggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        Aggregations(
          format = e4sAggregations.decodeAgg[Format]("format"),
          genres = e4sAggregations.decodeAgg[Genre[IdState.Minted]]("genres"),
          productionDates = e4sAggregations
            .decodeAgg[Period[IdState.Minted]]("productionDates"),
          languages = e4sAggregations.decodeAgg[Language]("languages"),
          subjects = e4sAggregations
            .decodeAgg[Subject[IdState.Minted]]("subjects"),
          license = e4sAggregations.decodeAgg[License]("license"),
          locationType =
            e4sAggregations.decodeAgg[LocationTypeQuery]("locationType")
        ))
    } else {
      None
    }
  }

  // Elasticsearch encodes the date key as milliseconds since the epoch
  implicit val decodePeriod: Decoder[Period[IdState.Minted]] =
    Decoder.decodeLong.emap { epochMilli =>
      Try { Instant.ofEpochMilli(epochMilli) }
        .map { instant =>
          LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }
        .map { date =>
          Right(Period(date.getYear.toString))
        }
        .getOrElse { Left("Error decoding") }
    }

  implicit val decodeLicense: Decoder[License] =
    Decoder.decodeString.emap { str =>
      Try(License.createLicense(str)).toEither.left
        .map(err => err.getMessage)
    }

  implicit val decodeFormat: Decoder[Format] =
    Decoder.decodeString.emap { str =>
      Format.fromCode(str) match {
        case Some(format) => Right(format)
        case None         => Left(s"couldn't find format for code '$str'")
      }
    }

  // Both the Calm and Sierra transformers use the MARC language code list
  // to populate the "languages" field, so we can use the ID (code) to
  // unambiguously identify a language.
  implicit val decodeLanguage: Decoder[Language] =
    Decoder.decodeString.emap { code =>
      MarcLanguageCodeList.lookupByCode(code) match {
        case Some(lang) => Right(lang)
        case None       => Left(s"couldn't find language for code $code")
      }
    }

  implicit val decodeGenreFromLabel: Decoder[Genre[IdState.Minted]] =
    Decoder.decodeString.map { str =>
      Genre(label = str)
    }

  implicit val decodeSubjectFromLabel: Decoder[Subject[IdState.Minted]] =
    Decoder.decodeString.map { str =>
      Subject(label = str, concepts = Nil)
    }

  implicit val decodeLocationTypeFromLabel: Decoder[LocationTypeQuery] =
    Decoder.decodeString.map {
      case "DigitalLocationDeprecated"  => LocationTypeQuery.DigitalLocation
      case "PhysicalLocationDeprecated" => LocationTypeQuery.PhysicalLocation
    }

  implicit class EnhancedEsAggregations(aggregations: Elastic4sAggregations) {

    def decodeAgg[T: Decoder](
      name: String,
      documentPath: Option[String] = None): Option[Aggregation[T]] = {
      aggregations
        .getAgg(name)
        .flatMap(
          _.safeTo[Aggregation[T]](
            (json: String) => {
              AggregationMapping.aggregationParser[T](json, documentPath)
            }
          ).recoverWith {
            case err =>
              warn("Failed to parse aggregation from ES", err)
              Failure(err)
          }.toOption
        )
    }
  }
}

// This object contains the tools required to map an aggregation result from ES
// into our Aggregation data type.
//
// There 2 independent variables in each bucket of these aggregations:
//
// ## Root count vs filtered count
// If aggregation buckets have a subaggregation named `filtered` from
// [[uk.ac.wellcome.platform.api.services.FiltersAndAggregationsBuilder]] then
// we use the count from there, otherwise we use the count from the root of the
// bucket.
//
// ## Key-only vs sample document
// For some aggregation types `T` (in `Aggregation[T]`) we can decode `T`
// directly from the bucket key - for example with Format, we can use
// `fromCode` to get the label of the Format given its id.
//
// However, sometimes
// we need more information from the index, which is achieved using a top hits
// aggregation of size 1 - see
// [[uk.ac.wellcome.platform.api.services.ElasticsearchQueryBuilder]].
//
// In this case there is a `sample_doc` subaggregation for which we can provide
// a `path` to the relevant data inside it. For example, the Language type uses
// this to obtain both the label and id at the path `data.language` within
// `sample_doc`.
object AggregationMapping extends Logging {
  import io.circe.parser._
  import io.circe.optics.JsonPath
  import io.circe.optics.JsonPath._
  import cats.syntax.traverse._
  import cats.instances.list._
  import cats.instances.try_._

  private val buckets = root.buckets.arr
  private val bucketFilteredCount = root.filtered.doc_count.int
  private val bucketRootCount = root.doc_count.int
  private val bucketKey = root.key.json
  private val bucketSampleDoc =
    root.sample_doc.hits.hits.index(0)._source.json

  def aggregationParser[T: Decoder](json: String,
                                    path: Option[String]): Try[Aggregation[T]] =
    parse(json).toTry
      .map { parsedJson =>
        for {
          bucket <- getBuckets(parsedJson)
          bucketDoc <- getBucketDoc(bucket, path).map(_.as[T])
          bucketCount <- getBucketCount(bucket)
        } yield {
          bucketDoc match {
            case Right(data) =>
              Success(Some(AggregationBucket(data, bucketCount)))
            case Left(err) => Failure(err)
          }
        }
      }
      .flatMap {
        _.toList.sequence.map { maybeBuckets =>
          Aggregation(
            maybeBuckets.flatten
            // For consistency across all combinations of aggregations and filters,
            // we need to filter out empty buckets when they are constructed from a top hit
            // (See the language aggregation in WorksRequestBuilder)
            //
            // If we don't do this then empty buckets will be returned inconsistently
            // depending on other filters and aggregations applied due to the subaggregation
            // filtering technique used in FiltersAndAggregationsBuilder and parsed here.
              .filterNot(path.isDefined && _.count == 0)
          )
        }
      }

  // Takes a path of format "fruit.apple.seed" and converts it to a JsonPath optic
  private def pathToOptic(path: String): JsonPath =
    path.split("\\.").foldLeft(root) { (optic, pathElement) =>
      optic.selectDynamic(pathElement)
    }

  private def getBuckets(agg: Json) = buckets.getOption(agg).getOrElse(List())

  private def getBucketDoc(bucket: Json, path: Option[String]) =
    bucketSampleDoc
      .getOption(bucket)
      .flatMap(getJsonAtPath(path))
      .orElse(bucketKey.getOption(bucket))

  private def getJsonAtPath(path: Option[String])(doc: Json) =
    path.flatMap(pathToOptic(_).json.getOption(doc))

  private def getBucketCount(bucket: Json) =
    bucketFilteredCount
      .getOption(bucket)
      .orElse(bucketRootCount.getOption(bucket))
}

case class Aggregation[+T](buckets: List[AggregationBucket[T]])
case class AggregationBucket[+T](data: T, count: Int)
