package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.DateHistogramInterval
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.aggs.{
  CompositeAggregation,
  DateHistogramAggregation,
  TermsValueSource,
}
import com.sksamuel.elastic4s.requests.searches.queries.{Query, RangeQuery}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.{ElasticDate, Index}
import uk.ac.wellcome.display.models.{
  AggregationRequest,
  ProductionDateSortRequest,
  SortingOrder,
}

import uk.ac.wellcome.platform.api.models._

case class ElastsearchSearchRequestBuilder(
  index: Index,
  maybeWorkQuery: Option[WorkQuery],
  sortDefinitions: List[FieldSort],
  queryOptions: ElasticsearchQueryOptions) {

  lazy val request: SearchRequest = search(index)
    .aggs { aggregations }
    .query { filteredQuery }
    .postFilter { ??? }
    .sortBy { sort ++ sortDefinitions }
    .limit { queryOptions.limit }
    .from { queryOptions.from }

  lazy val aggregations = queryOptions.aggregations.map {
    case AggregationRequest.WorkType =>
      CompositeAggregation("workType")
        .size(100)
        .sources(
          List(
            TermsValueSource("label", field = Some("data.workType.label.raw")),
            TermsValueSource("id", field = Some("data.workType.id")),
            TermsValueSource("type", field = Some("data.workType.ontologyType"))
          )
        )

    case AggregationRequest.ProductionDate =>
      DateHistogramAggregation("productionDates")
        .interval(DateHistogramInterval.Year)
        .field("data.production.dates.range.from")
        .minDocCount(1)

    // We don't split genres into concepts, as the data isn't great, and for rendering isn't useful
    // at the moment. But we've left it as a CompositeAggregation to scale when we need to.
    case AggregationRequest.Genre =>
      CompositeAggregation("genres")
        .size(20)
        .sources(
          List(
            TermsValueSource(
              "label",
              field = Some("data.genres.concepts.agent.label.raw"))
          )
        )
        .subAggregations(sortedByCount)

    case AggregationRequest.Subject =>
      CompositeAggregation("subjects")
        .size(20)
        .sources(
          List(
            TermsValueSource(
              "label",
              field = Some("data.subjects.agent.label.raw")
            )
          )
        )
        .subAggregations(sortedByCount)

    case AggregationRequest.Language =>
      CompositeAggregation("language")
        .size(200)
        .sources(
          List(
            TermsValueSource("id", field = Some("data.language.id")),
            TermsValueSource("label", field = Some("data.language.label.raw"))
          )
        )
  }

  lazy val sort = queryOptions.sortBy
    .map {
      case ProductionDateSortRequest => "data.production.dates.range.from"
    }
    .map { FieldSort(_).order(sortOrder) }

  lazy val sortOrder = queryOptions.sortOrder match {
    case SortingOrder.Ascending  => SortOrder.ASC
    case SortingOrder.Descending => SortOrder.DESC
  }

  lazy val filteredQuery = maybeWorkQuery
    .map { workQuery =>
      must(workQuery.query)
    }
    .getOrElse { boolQuery }
    .filter {
      (IdentifiedWorkFilter :: queryOptions.filters).map(buildWorkFilterQuery)
    }

  private def buildWorkFilterQuery(workFilter: WorkFilter): Query =
    workFilter match {
      case IdentifiedWorkFilter =>
        termQuery(field = "type", value = "IdentifiedWork")
      case ItemLocationTypeFilter(itemLocationTypeIds) =>
        termsQuery(
          field = "data.items.agent.locations.locationType.id",
          values = itemLocationTypeIds)
      case WorkTypeFilter(workTypeIds) =>
        termsQuery(field = "data.workType.id", values = workTypeIds)
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery("data.production.dates.range.from", lte = lte, gte = gte)
      case LanguageFilter(languageIds) =>
        termsQuery(field = "data.language.id", values = languageIds)
      case GenreFilter(genreQuery) =>
        matchQuery(field = "data.genres.label", value = genreQuery)
      case SubjectFilter(subjectQuery) =>
        matchQuery(field = "data.subjects.agent.label", value = subjectQuery)
    }

  private def sortedByCount =
    List(
      bucketSortAggregation(
        "sort_by_count",
        Seq(FieldSort("_count").order(SortOrder.DESC))
      )
    )
}
