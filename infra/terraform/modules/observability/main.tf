resource "aws_cloudwatch_log_group" "app" {
  name              = var.log_group_name
  retention_in_days = var.retention_days

  tags = {
    Name = var.log_group_name
  }
}

resource "aws_cloudwatch_metric_alarm" "cpu" {
  count               = length(var.instance_ids)
  alarm_name          = "spring-petclinic-cpu-${var.instance_ids[count.index]}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = var.cpu_alarm_threshold
  alarm_description   = "CPU alta en instancia ${var.instance_ids[count.index]}"

  dimensions = {
    InstanceId = var.instance_ids[count.index]
  }
}

# Requiere que el CloudWatch Agent esté corriendo en la instancia y publicando
# mem_used_percent en el namespace CWAgent (ver modules/ec2/user_data.sh.tftpl). Sin el
# agente, EC2 no expone memoria de forma nativa y esta alarma nunca recibiría datos.
resource "aws_cloudwatch_metric_alarm" "memory" {
  count               = length(var.instance_ids)
  alarm_name          = "spring-petclinic-memoria-${var.instance_ids[count.index]}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = var.memory_alarm_threshold
  alarm_description   = "Memoria alta en instancia ${var.instance_ids[count.index]}"

  dimensions = {
    InstanceId = var.instance_ids[count.index]
  }
}
