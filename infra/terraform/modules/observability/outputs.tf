output "log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}

output "cpu_alarm_names" {
  value = aws_cloudwatch_metric_alarm.cpu[*].alarm_name
}

output "memory_alarm_names" {
  value = aws_cloudwatch_metric_alarm.memory[*].alarm_name
}
