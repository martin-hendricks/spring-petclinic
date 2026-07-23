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
