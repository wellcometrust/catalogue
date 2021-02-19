package weco.catalogue.sierra_item_linker

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scanamo.generic.auto._
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.sierra_adapter.model.Implicits._
import uk.ac.wellcome.sierra_adapter.model.SierraItemNumber
import uk.ac.wellcome.storage.store.dynamo.DynamoSingleVersionStore
import uk.ac.wellcome.storage.typesafe.DynamoBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.catalogue.sierra_adapter.linker.{
  SierraLinkStore,
  SierraLinkerWorkerService
}
import weco.catalogue.sierra_item_linker.linker.{
  SierraItemLink,
  SierraItemLinker
}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    implicit val dynamoClient: DynamoDbClient =
      DynamoBuilder.buildDynamoClient(config)

    val versionedStore =
      new DynamoSingleVersionStore[SierraItemNumber, SierraItemLink](
        config = DynamoBuilder.buildDynamoConfig(config)
      )

    new SierraLinkerWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      linkStore = new SierraLinkStore(
        store = versionedStore,
        linker = SierraItemLinker
      ),
      messageSender = SNSBuilder
        .buildSNSMessageSender(config, subject = "Sierra item linker")
    )
  }
}
