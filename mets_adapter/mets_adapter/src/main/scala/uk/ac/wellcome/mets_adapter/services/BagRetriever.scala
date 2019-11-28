package uk.ac.wellcome.mets_adapter.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import grizzled.slf4j.Logging
import io.circe.generic.auto._
import uk.ac.wellcome.mets_adapter.models._

import scala.concurrent.{ExecutionContext, Future}

trait BagRetriever {
  def getBag(update: IngestUpdate): Future[Bag]
}

class HttpBagRetriever(url: String, tokenService: TokenService)(
  implicit
  actorSystem: ActorSystem,
  materializer: ActorMaterializer,
  executionContext: ExecutionContext)
    extends BagRetriever
    with Logging {

  def getBag(update: IngestUpdate): Future[Bag] = {
    debug(s"Executing request to ${generateUrl(update)}")
    for {
      token <- tokenService.getToken
      response <- Http().singleRequest(generateRequest(update, token))
      maybeBag <- {
        debug(s"Received response ${response.status}")
        handleResponse(response)
      }
    } yield maybeBag
  }

  private def generateUrl(update: IngestUpdate) =
    s"$url/${update.context.storageSpace}/${update.context.externalIdentifier}"

  private def generateRequest(update: IngestUpdate,
                              token: OAuth2BearerToken): HttpRequest =
    HttpRequest(uri = generateUrl(update)).addHeader(Authorization(token))

  private def handleResponse(response: HttpResponse): Future[Bag] =
    response.status match {
      case StatusCodes.OK => parseResponseIntoBag(response)
      case StatusCodes.NotFound =>
        Future.failed(new Exception("Bag does not exist on storage service"))
      case StatusCodes.Unauthorized =>
        Future.failed(new Exception("Failed to authorize with storage service"))
      case status =>
        Future.failed(
          new Exception(s"Received error from storage service: $status"))
    }

  private def parseResponseIntoBag(response: HttpResponse) =
    Unmarshal(response.entity).to[Bag].recover {
      case t =>
        throw new Exception("Failed parsing response into a Bag")
    }
}
