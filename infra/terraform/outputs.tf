output "vpc_id" {
  description = "ID de la VPC creada"
  value       = module.network.vpc_id
}

output "public_subnet_ids" {
  description = "IDs de las subredes públicas"
  value       = module.network.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs de las subredes privadas"
  value       = module.network.private_subnet_ids
}

output "app_security_group_id" {
  description = "Security group de la app (entrada al puerto de la aplicación desde internet)"
  value       = module.network.app_security_group_id
}

output "rds_security_group_id" {
  description = "Security group de RDS (entrada 5432 solo desde el security group de la app)"
  value       = module.network.rds_security_group_id
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR de la imagen de la app"
  value       = module.ecr.repository_url
}

output "ec2_public_ips" {
  description = "IPs públicas de las instancias EC2 (índice 0 = legado, índice 1 = modernizado)"
  value       = module.ec2.public_ips
}

output "rds_endpoint" {
  description = "Endpoint host:puerto de conexión a RDS"
  value       = module.rds.endpoint
  sensitive   = true
}
