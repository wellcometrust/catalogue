package weco.pipeline.id_minter.config.models

case class RDSClientConfig(
  primaryHost: String,
  replicaHost: String,
  port: Int,
  username: String,
  password: String
)
