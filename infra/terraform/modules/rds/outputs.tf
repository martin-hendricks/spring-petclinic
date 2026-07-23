output "endpoint" {
  description = "Endpoint host:puerto de conexión a la instancia"
  value       = aws_db_instance.this.endpoint
  sensitive   = true
}

output "db_instance_identifier" {
  value = aws_db_instance.this.identifier
}
