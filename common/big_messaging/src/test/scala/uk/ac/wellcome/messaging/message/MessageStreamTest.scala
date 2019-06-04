package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedQueue

import akka.stream.scaladsl.Flow
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.streaming.CodecInstances._

import scala.concurrent.Future
import scala.util.Random

class MessageStreamTest
    extends FunSpec
    with Matchers
    with Messaging {

  def process(list: ConcurrentLinkedQueue[ExampleObject])(o: ExampleObject) = {
    list.add(o)
    Future.successful(())
  }

  describe("small messages (<256KB)") {
    it("reads messages off a queue, processes them and deletes them") {
      withMessageStreamFixtures[ExampleObject, Assertion] {
        case (messageStream, QueuePair(queue, dlq), _) =>
          val messages = createMessages(count = 3)

          messages.foreach { exampleObject =>
            sendInlineNotification(queue = queue, exampleObject = exampleObject)
          }

          val received = new ConcurrentLinkedQueue[ExampleObject]()

          messageStream.foreach(
            streamName = "test-stream",
            process = process(received))

          eventually {
            received should contain theSameElementsAs messages

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
  }

  describe("large messages (>256KB)") {
    it("reads messages off a queue, processes them and deletes them") {
      withMessageStreamFixtures[ExampleObject, Assertion] {
        case (messageStream, QueuePair(queue, dlq), _) =>
          withLocalS3Bucket { bucket =>
            val messages = createMessages(count = 3)

            messages.foreach { exampleObject =>
              sendRemoteNotification(bucket, queue, exampleObject)
            }

            val received = new ConcurrentLinkedQueue[ExampleObject]()

            messageStream.foreach(
              streamName = "test-stream",
              process = process(received))

            eventually {
              received should contain theSameElementsAs messages

              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
            }
          }
      }
    }
  }

  private def sendInlineNotification(queue: Queue,
                                     exampleObject: ExampleObject): Unit =
    sendNotificationToSQS[MessageNotification](
      queue = queue,
      message = InlineNotification(toJson(exampleObject).get)
    )

  private def sendRemoteNotification(bucket: Bucket,
                                     queue: Queue,
                                     exampleObject: ExampleObject): Unit = {
    val s3key = Random.alphanumeric take 10 mkString
    val location = ObjectLocation(namespace = bucket.name, key = s3key)

    val serialisedObj = toJson[ExampleObject](exampleObject).get

    s3Client.putObject(
      location.namespace,
      location.key,
      serialisedObj
    )

    sendNotificationToSQS[MessageNotification](
      queue = queue,
      message = RemoteNotification(location)
    )
  }

  private def createMessages(count: Int): List[ExampleObject] =
    (1 to count).map { idx =>
      ExampleObject("a" * idx)
    }.toList

  it("increments *_ProcessMessage metric when successful") {
    withMessageStreamFixtures[ExampleObject, Future[Unit]] {
      case (messageStream, QueuePair(queue, _), metrics) =>
        val exampleObject = ExampleObject("some value")
        sendInlineNotification(queue = queue, exampleObject = exampleObject)

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received)
        )

        eventually(
          metrics.incrementedCounts shouldBe Seq(
            "test-stream_ProcessMessage_success")
        )
    }
  }

  it("fails gracefully when NotificationMessage cannot be deserialised") {
    withMessageStreamFixtures[ExampleObject, Assertion] {
      case (messageStream, QueuePair(queue, dlq), metrics) =>
        sendInvalidJSONto(queue)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          metrics.incrementedCounts.foreach { entry =>
            entry should not endWith "_ProcessMessage_failure"
          }

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("does not fail gracefully when the s3 object cannot be retrieved") {
    withMessageStreamFixtures[ExampleObject, Assertion] {
      case (messageStream, QueuePair(queue, dlq), metrics) =>
        val streamName = "test-stream"

        // Do NOT put S3 object here
        val objectLocation = ObjectLocation(
          namespace = "bukkit",
          key = "key.json"
        )

        sendNotificationToSQS[MessageNotification](
          queue = queue,
          message = RemoteNotification(objectLocation)
        )

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          metrics.incrementedCounts shouldBe Seq(
            "test-stream_ProcessMessage_failure",
            "test-stream_ProcessMessage_failure",
            "test-stream_ProcessMessage_failure"
          )

          received shouldBe empty

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("continues reading if processing of some messages fails ") {
    withMessageStreamFixtures[ExampleObject, Assertion] {
      case (messageStream, QueuePair(queue, dlq), _) =>
        val exampleObject1 = ExampleObject("some value 1")
        val exampleObject2 = ExampleObject("some value 2")

        sendInvalidJSONto(queue)
        sendInlineNotification(queue = queue, exampleObject = exampleObject1)

        sendInvalidJSONto(queue)
        sendInlineNotification(queue = queue, exampleObject = exampleObject2)

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs List(
            exampleObject1,
            exampleObject2)

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 2)
        }
    }
  }

  describe("runStream") {
    it("processes messages off a queue") {
      withMessageStreamFixtures[ExampleObject, Future[Unit]] {
        case (messageStream, QueuePair(queue, dlq), metrics) =>
          val exampleObject1 = ExampleObject("some value 1")
          sendInlineNotification(queue = queue, exampleObject = exampleObject1)
          val exampleObject2 = ExampleObject("some value 2")
          sendInlineNotification(queue = queue, exampleObject = exampleObject2)

          val received = new ConcurrentLinkedQueue[ExampleObject]()

          messageStream.runStream(
            "test-stream",
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            metrics.incrementedCounts shouldBe Seq(
              "test-stream_ProcessMessage_success",
              "test-stream_ProcessMessage_success"
            )

            received should contain theSameElementsAs List(
              exampleObject1,
              exampleObject2)

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }

    it("does not delete failed messages and sends a failure metric") {
      withMessageStreamFixtures[ExampleObject, Future[Unit]] {
        case (messageStream, QueuePair(queue, dlq), metrics) =>
          val exampleObject = ExampleObject("some value")
          sendInlineNotification(queue = queue, exampleObject = exampleObject)

          messageStream.runStream(
            "test-stream",
            source =>
              source.via(
                Flow.fromFunction(_ => throw new RuntimeException("BOOOM!"))))

          eventually {
            metrics.incrementedCounts shouldBe Seq(
              "test-stream_ProcessMessage_failure",
              "test-stream_ProcessMessage_failure",
              "test-stream_ProcessMessage_failure"
            )

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
      }
    }

    it("continues reading if processing of some messages fails") {
      withMessageStreamFixtures[ExampleObject, Assertion] {
        case (messageStream, QueuePair(queue, dlq), _) =>
          val exampleObject1 = ExampleObject("some value 1")
          val exampleObject2 = ExampleObject("some value 2")

          sendInvalidJSONto(queue)
          sendInlineNotification(queue = queue, exampleObject = exampleObject1)

          sendInvalidJSONto(queue)
          sendInlineNotification(queue = queue, exampleObject = exampleObject2)

          val received = new ConcurrentLinkedQueue[ExampleObject]()
          messageStream.runStream(
            "test-stream",
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            received should contain theSameElementsAs List(
              exampleObject1,
              exampleObject2)

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 2)
          }
      }
    }
  }
}
