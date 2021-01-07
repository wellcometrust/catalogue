module "id_minter_queue" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.1.2"
  queue_name = "${local.namespace_hyphen}_id_minter"
  topic_arns = [module.calm_transformer_output_topic.arn,
    module.mets_transformer_output_topic.arn,
    module.miro_transformer_output_topic.arn,
  module.sierra_transformer_output_topic.arn, ]
  aws_region                 = var.aws_region
  alarm_topic_arn            = var.dlq_alarm_arn
  visibility_timeout_seconds = 120
}

module "id_minter" {
  source          = "../modules/service"
  service_name    = "${local.namespace_hyphen}_id_minter"
  container_image = local.id_minter_image

  security_group_ids = [
    aws_security_group.service_egress.id,
    aws_security_group.interservice.id,
    var.rds_ids_access_security_group_id,
  ]

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.id

  env_vars = {
    metrics_namespace    = "${local.namespace_hyphen}_id_minter"
    messages_bucket_name = aws_s3_bucket.messages.id

    queue_url                     = module.id_minter_queue.url
    topic_arn                     = module.id_minter_topic.arn
    max_connections               = local.id_minter_task_max_connections
    es_source_index               = local.es_works_source_index
    es_identified_index           = local.es_works_identified_index
    ingest_batch_size             = 100
    ingest_flush_interval_seconds = 30
  }

  secret_env_vars = {
    cluster_url          = "rds/identifiers-delta-cluster/endpoint"
    cluster_url_readonly = "rds/identifiers-delta-cluster/reader_endpoint"
    db_port              = "rds/identifiers-delta-cluster/port"
    db_username          = "catalogue/id_minter/rds_user"
    db_password          = "catalogue/id_minter/rds_password"

    es_host     = "catalogue/pipeline_storage/es_host"
    es_port     = "catalogue/pipeline_storage/es_port"
    es_protocol = "catalogue/pipeline_storage/es_protocol"
    es_username = "catalogue/pipeline_storage/id_minter/es_username"
    es_password = "catalogue/pipeline_storage/id_minter/es_password"
  }

  // The total number of connections to RDS across all tasks from all ID minter
  // services must not exceed the maximum supported by the RDS instance.
  max_capacity = min(
    floor(
      local.id_minter_rds_max_connections / local.id_minter_task_max_connections
    ),
    var.max_capacity
  )

  subnets             = var.subnets
  messages_bucket_arn = aws_s3_bucket.messages.arn
  queue_read_policy   = module.id_minter_queue.read_policy

  cpu    = 1024
  memory = 2048

  deployment_service_env  = var.release_label
  deployment_service_name = "id-minter"
  shared_logging_secrets  = var.shared_logging_secrets
}

# Output topic

module "id_minter_topic" {
  source = "../modules/topic"

  name       = "${local.namespace_hyphen}_work_id_minter"
  role_names = [module.id_minter.task_role_name]

  messages_bucket_arn = aws_s3_bucket.messages.arn
}

module "id_minter_scaling_alarm" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//autoscaling?ref=v1.1.3"
  queue_name = module.id_minter_queue.name

  queue_high_actions = [module.id_minter.scale_up_arn]
  queue_low_actions  = [module.id_minter.scale_down_arn]
}
