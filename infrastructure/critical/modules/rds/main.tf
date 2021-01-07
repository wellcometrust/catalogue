resource "aws_rds_cluster_instance" "cluster_instances" {
  count = 2

  identifier           = "${var.cluster_identifier}-${count.index}"
  cluster_identifier   = aws_rds_cluster.default.id
  instance_class       = var.instance_class
  db_subnet_group_name = var.aws_db_subnet_group_name
  publicly_accessible  = false
}

resource "aws_rds_cluster" "default" {
  db_subnet_group_name = var.aws_db_subnet_group_name

  cluster_identifier     = var.cluster_identifier
  database_name          = var.database_name
  master_username        = var.username
  master_password        = var.password
  vpc_security_group_ids = [var.db_security_group_id]
}
