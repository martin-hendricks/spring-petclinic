variable "instance_count" {
  description = "Número de instancias EC2. Por defecto 2: el pre-experimento pide instancias gemelas (legado / modernizado) para poder comparar métricas."
  type        = number
  default     = 2
}

variable "instance_type" {
  description = "Tipo de instancia EC2"
  type        = string
  default     = "t3.medium"
}

variable "ami_id" {
  description = "AMI base. Si se deja null, se resuelve la Amazon Linux 2023 x86_64 más reciente vía data source."
  type        = string
  default     = null
}

variable "public_subnet_ids" {
  description = "Subredes públicas donde lanzar las instancias"
  type        = list(string)
}

variable "app_security_group_id" {
  description = "Security group con la entrada al puerto de la app"
  type        = string
}

variable "ecr_repository_url" {
  description = "URL del repositorio ECR desde el que las instancias hacen docker pull"
  type        = string
}

variable "aws_region" {
  description = "Región AWS, usada por user_data para autenticar contra ECR"
  type        = string
}

variable "instance_profile_name" {
  description = "Nombre de un instance profile IAM YA EXISTENTE a asociar a las instancias (este módulo no crea IAM: ver la nota de desviación en main.tf). En AWS Academy Learner Lab, usar 'LabInstanceProfile'."
  type        = string
}

variable "root_volume_size" {
  description = "Tamaño (GB) del volumen raíz. La AMI base observada trae solo 2 GB, insuficiente para docker + amazon-cloudwatch-agent (ver nota en main.tf)."
  type        = number
  default     = 20
}
