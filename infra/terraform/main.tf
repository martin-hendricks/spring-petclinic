module "network" {
  source = "./modules/network"

  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  app_port             = var.app_port
}

module "ecr" {
  source = "./modules/ecr"

  repository_name = var.ecr_repository_name
}

module "rds" {
  source = "./modules/rds"

  instance_class         = var.rds_instance_class
  db_password            = var.db_password
  private_subnet_ids     = module.network.private_subnet_ids
  vpc_security_group_ids = [module.network.rds_security_group_id]
}

module "ec2" {
  source = "./modules/ec2"

  instance_count        = var.instance_count
  public_subnet_ids     = module.network.public_subnet_ids
  app_security_group_id = module.network.app_security_group_id
  ecr_repository_url    = module.ecr.repository_url
  aws_region            = var.aws_region
  instance_profile_name = var.instance_profile_name
}

module "observability" {
  source = "./modules/observability"

  instance_ids = module.ec2.instance_ids
}
