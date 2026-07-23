# Fase 6.1: solo la capa de red. Los módulos ecr/rds/ec2/observability se instancian aquí
# mismo en el prompt 6.2, una vez aprobado este checkpoint.
module "network" {
  source = "./modules/network"

  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  app_port             = var.app_port
}
