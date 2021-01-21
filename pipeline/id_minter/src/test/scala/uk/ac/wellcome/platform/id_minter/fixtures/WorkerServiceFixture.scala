package uk.ac.wellcome.platform.id_minter.fixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import io.circe.Json
import io.circe.syntax._
import scalikejdbc.{ConnectionPool, ConnectionPoolSettings}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.platform.id_minter.config.models.IdentifiersTableConfig
import uk.ac.wellcome.platform.id_minter.database.IdentifiersDao
import uk.ac.wellcome.platform.id_minter.models.IdentifiersTable
import uk.ac.wellcome.platform.id_minter.steps.IdentifierGenerator
import uk.ac.wellcome.platform.id_minter.services.IdMinterWorkerService
import uk.ac.wellcome.pipeline_storage.{MemoryIndexer, MemoryRetriever}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.Implicits._
import WorkState.{Identified, Source}
import uk.ac.wellcome.pipeline_storage.fixtures.PipelineStorageStreamFixtures

trait WorkerServiceFixture
    extends IdentifiersDatabase
    with PipelineStorageStreamFixtures {
  def withWorkerService[R](
    messageSender: MemoryMessageSender = new MemoryMessageSender(),
    queue: Queue = Queue("url://q", "arn::q", visibilityTimeout = 1),
    identifiersDao: IdentifiersDao,
    identifiersTableConfig: IdentifiersTableConfig,
    mergedIndex: Map[String, Json] = Map.empty,
    identifiedIndex: mutable.Map[String, Work[Identified]] = mutable.Map.empty)(
    testWith: TestWith[IdMinterWorkerService[String], R]): R =
    withPipelineStream(
      queue,
      new MemoryIndexer(index = identifiedIndex),
      messageSender) { stream =>
      val identifierGenerator = new IdentifierGenerator(
        identifiersDao = identifiersDao
      )
      val workerService = new IdMinterWorkerService(
        identifierGenerator = identifierGenerator,
        pipelineStream = stream,
        jsonRetriever =
          new MemoryRetriever(index = mutable.Map(mergedIndex.toSeq: _*)),
        rdsClientConfig = rdsClientConfig,
        identifiersTableConfig = identifiersTableConfig
      )

      workerService.run()

      testWith(workerService)
    }

  def withWorkerService[R](
    messageSender: MemoryMessageSender,
    queue: Queue,
    identifiersTableConfig: IdentifiersTableConfig,
    mergedIndex: Map[String, Json],
    identifiedIndex: mutable.Map[String, Work[Identified]])(
    testWith: TestWith[IdMinterWorkerService[String], R]): R = {
    Class.forName("com.mysql.jdbc.Driver")
    ConnectionPool.singleton(
      s"jdbc:mysql://$host:$port",
      username,
      password,
      settings = ConnectionPoolSettings(maxSize = maxSize)
    )

    val identifiersDao = new IdentifiersDao(
      identifiers = new IdentifiersTable(
        identifiersTableConfig = identifiersTableConfig
      )
    )

    withWorkerService(
      messageSender,
      queue,
      identifiersDao,
      identifiersTableConfig,
      mergedIndex,
      identifiedIndex) { service =>
      testWith(service)
    }
  }

  def createIndex(works: List[Work[Source]]): Map[String, Json] =
    works.map(work => (work.id, work.asJson)).toMap
}
