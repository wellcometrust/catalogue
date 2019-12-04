package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.models.work.internal.WorkType.{Books, ManuscriptsAsian, Music}

class AggregationResultsTest extends FunSpec with Matchers {
  it("destructures a single aggregation result") {
    // val searchResponse: com.sksamuel.elastic4s.requests.searches.SearchResponse
    // This mimics the searchResponse.aggregationsAsString
    val responseString =
      """
        |{
        |  "workType": {
        |    "doc_count_error_upper_bound": 0,
        |    "sum_other_doc_count": 0,
        |    "buckets": [
        |      {
        |          "key" : {
        |            "id" : "a",
        |            "label" : "Books",
        |            "type" : "WorkType"
        |          },
        |          "doc_count" : 393145
        |        },
        |        {
        |          "key" : {
        |            "id" : "b",
        |            "label" : "Manuscripts, Asian",
        |            "type" : "WorkType"
        |          },
        |          "doc_count" : 5696
        |        },
        |        {
        |          "key" : {
        |            "id" : "c",
        |            "label" : "Music",
        |            "type" : "WorkType"
        |          },
        |          "doc_count" : 9
        |        }
        |    ]
        |  }
        |}
        |""".stripMargin

    val singleAgg = Aggregations(responseString)
    singleAgg.get.workType shouldBe Some(
      Aggregation[WorkType](List(
        AggregationBucket(data = Books, count = 393145),
        AggregationBucket(
          data = ManuscriptsAsian,
          count = 5696),
        AggregationBucket(data = Music, count = 9)
      )))
  }
}
