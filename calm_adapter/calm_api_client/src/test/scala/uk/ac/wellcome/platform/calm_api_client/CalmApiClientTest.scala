package uk.ac.wellcome.platform.calm_api_client

import java.time.LocalDate

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  Cookie,
  RawHeader
}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.platform.calm_api_client.fixtures.{
  CalmApiTestClient,
  CalmResponseGenerators
}

import scala.concurrent.ExecutionContext.Implicits.global

class CalmApiClientTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with CalmResponseGenerators
    with CalmApiTestClient
    with Akka {

  val query = CalmQuery.ModifiedDate(LocalDate.of(2000, 1, 1))
  val suppressedField = "Secret" -> "Shhhh"

  it("performs search requests") {
    val nResults = 10
    val responses = List(searchResponse(n = nResults))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.search(query)) { response =>
          response shouldBe CalmSession(nResults, Cookie(cookie))
        }
    }
  }

  it("performs summary requests") {
    val responses = List(summaryResponse("RecordID" -> "1"))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.summary(1)) { response =>
          response shouldBe CalmRecord(
            "1",
            Map("RecordID" -> List("1")),
            retrievedAt)
        }
    }
  }

  it("uses basic auth credentials for requests") {
    val responses =
      List(searchResponse(n = 1), summaryResponse("RecordID" -> "1"))
    withCalmClients(responses) {
      case (apiClient, testHttpClient) =>
        val requestFuture = for {
          _ <- apiClient.search(query)
          _ <- apiClient.summary(1)
        } yield ()

        whenReady(requestFuture) { _ =>
          val authHeaders = testHttpClient.requests
            .flatMap(_.headers)
            .collect {
              case auth: Authorization => auth
            }

          every(authHeaders) should have(
            'credentials (BasicHttpCredentials(username, password))
          )
        }
    }
  }

  it("sets the SOAPAction header for requests") {
    val responses =
      List(searchResponse(n = 1), summaryResponse("RecordID" -> "1"))
    withCalmClients(responses) {
      case (apiClient, testHttpClient) =>
        val requestFuture = for {
          _ <- apiClient.search(query)
          _ <- apiClient.summary(1)
        } yield ()

        whenReady(requestFuture) { _ =>
          val rawHeaders = testHttpClient.requests
            .flatMap(_.headers)
            .collect {
              case auth: RawHeader => auth
            }

          every(rawHeaders) should have('name ("SOAPAction"))
          rawHeaders.map(_.value) shouldBe List(
            "http://ds.co.uk/cs/webservices/Search",
            "http://ds.co.uk/cs/webservices/SummaryHeader")
        }
    }
  }

  it("removes suppressed fields from summary responses") {
    val responses =
      List(summaryResponse("RecordID" -> "1", suppressedField))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.summary(1)) { response =>
          response.id shouldBe "1"
          response.data.keys should not contain suppressedField._1
        }
    }
  }

  it("fails if there is no cookie in a search response") {
    val responses = List(searchResponse(n = 1, cookiePair = None))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.search(query).failed) { error =>
          error.getMessage shouldBe "Session cookie not found in CALM response"
        }
    }
  }

  it("fails on error responses from the API") {
    val responses = List(HttpResponse(500, Nil, "Oops", protocol))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.search(query).failed) { error =>
          error.getMessage shouldBe "Max retries attempted when calling Calm API"
        }
    }
  }

  it("fails if there is no RecordID in a summary response") {
    val responses = List(summaryResponse("Beep" -> "Boop"))
    withCalmClients(responses) {
      case (apiClient, _) =>
        whenReady(apiClient.summary(1).failed) { error =>
          error.getMessage shouldBe "RecordID not found"
        }
    }
  }

  implicit val summaryParser: CalmHttpResponseParser[CalmSummaryRequest] =
    CalmHttpResponseParser.createSummaryResponseParser(Set(suppressedField._1))

}
