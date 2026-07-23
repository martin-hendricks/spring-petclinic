variable "aws_region" {
  description = "Región de AWS donde se despliega la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "Bloque CIDR de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDRs de las 2 subredes públicas (una por AZ)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDRs de las 2 subredes privadas (una por AZ)"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24"]
}

variable "app_port" {
  description = "Puerto en el que escucha la aplicación (contenedor spring-petclinic)"
  type        = number
  default     = 8080
}

variable "ecr_repository_name" {
  description = "Nombre del repositorio ECR de la imagen de la app"
  type        = string
  default     = "spring-petclinic"
}

variable "rds_instance_class" {
  description = "Clase de instancia de la base de datos RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "db_password" {
  description = "Password de la base de datos RDS. NUNCA hardcodear aquí: proveer via terraform.tfvars (no versionado, ver terraform.tfvars.example) o la variable de entorno TF_VAR_db_password."
  type        = string
  sensitive   = true
}

variable "instance_count" {
  description = "Número de instancias EC2. Por defecto 2: instancias gemelas legado/modernizado que pide el pre-experimento para comparar métricas."
  type        = number
  default     = 2
}
