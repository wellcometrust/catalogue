package weco.pipeline.id_minter

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.memory.MemoryMessageSender
import weco.catalogue.internal_model.work.WorkState.Identified
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState}
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.pipeline.id_minter.fixtures.WorkerServiceFixture

import scala.collection.mutable

class IdMinterFeatureTest
    extends AnyFunSpec
    with Matchers
    with IntegrationPatience
    with Eventually
    with WorkerServiceFixture
    with WorkGenerators {

  it("mints the same IDs where source identifiers match") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersTable { identifiersTableConfig =>
        val work = sourceWork()
        val inputIndex = createIndex(List(work))
        val outputIndex = mutable.Map.empty[String, Work[Identified]]
        withWorkerService(
          messageSender,
          queue,
          identifiersTableConfig,
          inputIndex,
          outputIndex) { _ =>
          eventuallyTableExists(identifiersTableConfig)

          val messageCount = 5
          (1 to messageCount).foreach { _ =>
            sendNotificationToSQS(queue = queue, body = work.id)
          }

          eventually {
            messageSender.messages.length shouldBe messageCount
          }

          val sentIds = messageSender.messages.map(_.body).toSet
          sentIds.size shouldBe 1

          val identifiedWork = outputIndex(sentIds.head)
          identifiedWork.sourceIdentifier shouldBe work.sourceIdentifier
          identifiedWork.state.canonicalId shouldBe CanonicalId(sentIds.head)
        }
      }
    }
  }

  it("mints an identifier for a invisible work") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersTable { identifiersTableConfig =>
        val work = sourceWork().invisible()
        val inputIndex = createIndex(List(work))
        val outputIndex = mutable.Map.empty[String, Work[Identified]]
        withWorkerService(
          messageSender,
          queue,
          identifiersTableConfig,
          inputIndex,
          outputIndex) { _ =>
          eventuallyTableExists(identifiersTableConfig)

          sendNotificationToSQS(queue = queue, body = work.id)

          eventually {
            messageSender.messages.length shouldBe 1
          }

          val sentId = messageSender.messages.map(_.body).head

          val identifiedWork = outputIndex(sentId)
          identifiedWork.sourceIdentifier shouldBe work.sourceIdentifier
          identifiedWork.state.canonicalId shouldBe CanonicalId(sentId)
        }
      }
    }
  }

  it("mints an identifier for a redirected work") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersTable { identifiersTableConfig =>
        val work = sourceWork()
          .redirected(
            redirectTarget =
              IdState.Identifiable(sourceIdentifier = createSourceIdentifier))
        val inputIndex = createIndex(List(work))
        val outputIndex = mutable.Map.empty[String, Work[Identified]]
        withWorkerService(
          messageSender,
          queue,
          identifiersTableConfig,
          inputIndex,
          outputIndex) { _ =>
          eventuallyTableExists(identifiersTableConfig)

          sendNotificationToSQS(queue = queue, body = work.id)

          eventually {
            messageSender.messages.length shouldBe 1
          }

          val sentId = messageSender.messages.map(_.body).head

          val identifiedWork = outputIndex(sentId)
          identifiedWork.sourceIdentifier shouldBe work.sourceIdentifier
          identifiedWork.state.canonicalId shouldBe CanonicalId(sentId)
          identifiedWork
            .asInstanceOf[Work.Redirected[Identified]]
            .redirectTarget
            .canonicalId
            .underlying shouldNot be(empty)
        }
      }
    }
  }

  it("continues if something fails processing a message") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersTable { identifiersTableConfig =>
        val work = sourceWork()
        val inputIndex = createIndex(List(work))
        val outputIndex = mutable.Map.empty[String, Work[Identified]]
        withWorkerService(
          messageSender,
          queue,
          identifiersTableConfig,
          inputIndex,
          outputIndex) { _ =>
          sendInvalidJSONto(queue)

          sendNotificationToSQS(queue = queue, body = work.id)

          eventually {
            messageSender.messages should not be empty
          }
          assertMessageIsNotDeleted(queue)
        }
      }
    }
  }

  private def assertMessageIsNotDeleted(queue: Queue): Unit = {
    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    getQueueAttribute(
      queue,
      attributeName =
        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE) shouldBe "1"
  }
}
