variable "vpc_cidr" {
  description = "Bloque CIDR de la VPC"
  type        = string
}

variable "public_subnet_cidrs" {
  description = "CIDRs de las subredes públicas, una por AZ"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDRs de las subredes privadas, una por AZ"
  type        = list(string)
}

variable "app_port" {
  description = "Puerto de la aplicación, abierto en el security group de la app"
  type        = number
}
