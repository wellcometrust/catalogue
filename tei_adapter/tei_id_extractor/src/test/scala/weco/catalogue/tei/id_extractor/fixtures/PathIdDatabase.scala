package weco.catalogue.tei.id_extractor.fixtures

import org.scalatest.Assertion
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import scalikejdbc._
import weco.fixtures.TestWith
import weco.catalogue.tei.id_extractor.database.{
  PathIdTable,
  PathIdTableConfig,
  RDSClientConfig,
  TableProvisioner
}
import weco.catalogue.tei.id_extractor.FieldDescription

trait PathIdDatabase
    extends Eventually
    with IntegrationPatience
    with Matchers
    with TableNameGenerators {

  val host = "localhost"
  val port = 3307
  val username = "root"
  val password = "password"
  val maxSize = 8

  def eventuallyTableExists(tableConfig: PathIdTableConfig): Assertion =
    eventually {
      val database: SQLSyntax = SQLSyntax.createUnsafely(tableConfig.database)
      val table: SQLSyntax = SQLSyntax.createUnsafely(tableConfig.tableName)

      val fields = NamedDB('default) readOnly { implicit session =>
        sql"DESCRIBE $database.$table"
          .map(
            rs =>
              FieldDescription(
                rs.string("Field"),
                rs.string("Type"),
                rs.string("Null"),
                rs.string("Key")))
          .list()
          .apply()
      }

      fields.sortBy(_.field) shouldBe Seq(
        FieldDescription(
          field = "id",
          dataType = "varchar(255)",
          nullable = "NO",
          key = "UNI"),
        FieldDescription(
          field = "path",
          dataType = "varchar(255)",
          nullable = "NO",
          key = "PRI"),
        FieldDescription(
          field = "timeModified",
          dataType = "bigint(20) unsigned",
          nullable = "NO",
          key = "")
      ).sortBy(_.field)
    }

  val rdsClientConfig = RDSClientConfig(
    host = host,
    port = port,
    username = username,
    password = password,
    maxConnections = 3
  )

  def withPathIdDatabase[R](testWith: TestWith[PathIdTableConfig, R]): R = {
    ConnectionPool.add(
      'default,
      s"jdbc:mysql://$host:$port",
      username,
      password,
      settings = ConnectionPoolSettings(maxSize = maxSize)
    )

    implicit val session = AutoSession
    val databaseName: String = createDatabaseName
    val tableName: String = createTableName

    val pathIdDatabase: SQLSyntax = SQLSyntax.createUnsafely(databaseName)

    val pathIdTableConfig = PathIdTableConfig(
      database = databaseName,
      tableName = tableName
    )

    try {
      sql"CREATE DATABASE $pathIdDatabase".execute().apply()

      testWith(pathIdTableConfig)
    } finally {
      NamedDB('default) localTx { implicit session =>
        sql"DROP DATABASE IF EXISTS $pathIdDatabase".execute().apply()
      }

      session.close()
    }

  }

  def withPathIdTable[R](
    testWith: TestWith[(PathIdTableConfig, PathIdTable), R]): R = {
    withPathIdDatabase { config =>
      val table = new PathIdTable(config)
      testWith((config, table))
    }
  }

  def withInitializedPathIdTable[R](testWith: TestWith[PathIdTable, R]): R = {
    withPathIdTable {
      case (config, table) =>
        val provisioner = new TableProvisioner(rdsClientConfig, config)

        provisioner
          .provision()
        eventuallyTableExists(config)

        testWith(table)
    }
  }
}
