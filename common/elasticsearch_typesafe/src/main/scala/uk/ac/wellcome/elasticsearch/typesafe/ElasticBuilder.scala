package uk.ac.wellcome.elasticsearch.typesafe

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import uk.ac.wellcome.elasticsearch.ElasticClientBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object ElasticBuilder {
  def buildElasticClient(config: Config): ElasticClient = {
    val hostname = config.getOrElse[String]("es.host")(default = "localhost")
    val port = config
      .getOrElse[String]("es.port")(
        default = "9200"
      )
      .toInt
    val protocol = config.getOrElse[String]("es.protocol")(default = "http")
    val username = config.getOrElse[String]("es.username")(default = "username")
    val password = config.getOrElse[String]("es.password")(default = "password")

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      username = username,
      password = password
    )
  }
}
