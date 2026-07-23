output "instance_ids" {
  value = aws_instance.app[*].id
}

output "public_ips" {
  value = aws_instance.app[*].public_ip
}

output "iam_role_arn" {
  value = aws_iam_role.ec2.arn
}
