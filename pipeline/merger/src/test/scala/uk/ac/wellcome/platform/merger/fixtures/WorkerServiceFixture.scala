package uk.ac.wellcome.platform.merger.fixtures

import scala.concurrent.ExecutionContext.Implicits.global

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.work.internal.BaseWork
import uk.ac.wellcome.platform.merger.services._
import uk.ac.wellcome.models.Implicits._

import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue

trait WorkerServiceFixture extends LocalWorksVhs {

  def withWorkerService[R](vhs: VHS, queue: Queue, topic: Topic)(testWith: TestWith[MergerWorkerService[SNSConfig], R]): R =
    withLocalS3Bucket { bucket =>
      withSqsBigMessageSender[BaseWork, R](bucket, topic) { msgSender =>
        withActorSystem { implicit actorSystem =>
          withSQSStream[NotificationMessage, R](queue) { sqsStream =>
            val workerService = new MergerWorkerService(
              sqsStream = sqsStream,
              playbackService = new RecorderPlaybackService(vhs),
              mergerManager = new MergerManager(PlatformMerger),
              msgSender = msgSender
            )
            workerService.run()
            testWith(workerService)
        }
      }
    }
  }

  def withWorkerService[R](vhs: VHS)(testWith: TestWith[MergerWorkerService[SNSConfig], R]): R =
      withLocalSqsQueue { queue =>
        withLocalSnsTopic { topic =>
          withWorkerService(vhs, queue, topic) { workerService =>
            testWith(workerService)
          }
        }
      }
}
