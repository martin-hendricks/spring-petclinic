variable "repository_name" {
  description = "Nombre del repositorio ECR para la imagen de la app"
  type        = string
  default     = "spring-petclinic"
}

variable "max_images" {
  description = "Número máximo de imágenes a conservar (lifecycle policy)"
  type        = number
  default     = 10
}
