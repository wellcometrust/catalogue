package weco.pipeline.ingestor.images

import weco.messaging.sns.NotificationMessage
import weco.pipeline_storage.PipelineStorageStream
import weco.catalogue.internal_model.image.Image
import weco.catalogue.internal_model.image.ImageState.{Augmented, Indexed}
import weco.pipeline.ingestor.common.IngestorWorkerService
import weco.pipeline_storage.{PipelineStorageStream, Retriever}

import scala.concurrent.ExecutionContext

class ImageIngestorWorkerService[Destination](
  pipelineStream: PipelineStorageStream[NotificationMessage,
                                        Image[Indexed],
                                        Destination],
  imageRetriever: Retriever[Image[Augmented]],
  transform: Image[Augmented] => Image[Indexed] = ImageTransformer.deriveData,
)(implicit
  ec: ExecutionContext)
    extends IngestorWorkerService[
      Destination,
      Image[Augmented],
      Image[Indexed]](pipelineStream, imageRetriever, transform)
