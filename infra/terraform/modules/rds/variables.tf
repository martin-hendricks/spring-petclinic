variable "identifier" {
  description = "Identificador de la instancia RDS"
  type        = string
  default     = "spring-petclinic-db"
}

variable "engine_version" {
  description = "Versión de PostgreSQL. Debe coincidir en major version con docker-compose.yml del legado (postgres:18.3) para que el experimento sea comparable. Verificar disponibilidad exacta con `aws rds describe-db-engine-versions --engine postgres` antes de aplicar: no hay acceso a una cuenta AWS real en este entorno para confirmarla."
  type        = string
  default     = "18.3"
}

variable "instance_class" {
  description = "Clase de instancia RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "allocated_storage" {
  description = "Almacenamiento asignado en GB"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Nombre de la base de datos"
  type        = string
  default     = "petclinic"
}

variable "db_username" {
  description = "Usuario administrador de la base de datos"
  type        = string
  default     = "petclinic"
}

variable "db_password" {
  description = "Password de la base de datos. Nunca hardcodear: proveer via terraform.tfvars (no versionado) o la variable de entorno TF_VAR_db_password."
  type        = string
  sensitive   = true
}

variable "private_subnet_ids" {
  description = "Subredes privadas donde se aloja la instancia (sin acceso público)"
  type        = list(string)
}

variable "vpc_security_group_ids" {
  description = "Security groups a asociar (debe permitir 5432 solo desde el SG de la app)"
  type        = list(string)
}
