variable "log_group_name" {
  description = "Nombre del log group de CloudWatch"
  type        = string
  default     = "/spring-petclinic/app"
}

variable "retention_days" {
  description = "Retención de logs en días"
  type        = number
  default     = 7
}

variable "instance_ids" {
  description = "IDs de las instancias EC2 a monitorear"
  type        = list(string)
}

variable "cpu_alarm_threshold" {
  description = "Umbral (%) de CPU para disparar la alarma"
  type        = number
  default     = 80
}

variable "memory_alarm_threshold" {
  description = "Umbral (%) de memoria para disparar la alarma"
  type        = number
  default     = 80
}
