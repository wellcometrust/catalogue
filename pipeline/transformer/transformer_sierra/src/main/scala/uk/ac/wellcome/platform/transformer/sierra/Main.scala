package uk.ac.wellcome.platform.transformer.sierra

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.bigmessaging.typesafe.{BigMessagingBuilder, VHSBuilder}
import uk.ac.wellcome.elasticsearch.SourceWorkIndexConfig
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SQSBuilder
import uk.ac.wellcome.models.work.internal.Work
import uk.ac.wellcome.models.work.internal.WorkState.Source
import uk.ac.wellcome.pipeline_storage.typesafe.ElasticIndexerBuilder
import uk.ac.wellcome.platform.transformer.sierra.services.SierraTransformerWorkerService
import uk.ac.wellcome.sierra_adapter.model.SierraTransformable
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    import uk.ac.wellcome.sierra_adapter.model.Implicits._

    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val workIndexer = ElasticIndexerBuilder[Work[Source]](
      config,
      indexConfig = SourceWorkIndexConfig
    )

    new SierraTransformerWorkerService(
      stream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      sender = BigMessagingBuilder.buildBigMessageSender(config),
      workIndexer = workIndexer,
      store = VHSBuilder.build[SierraTransformable](config)
    )
  }
}
