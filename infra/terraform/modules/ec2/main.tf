data "aws_ami" "amazon_linux" {
  count       = var.ami_id == null ? 1 : 0
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

locals {
  ami_id = var.ami_id != null ? var.ami_id : data.aws_ami.amazon_linux[0].id

  # Instancia 0 = legado, instancia 1 = modernizado (instancias gemelas del pre-experimento
  # para comparar métricas). Si instance_count > 2, el resto se etiqueta "extra-N".
  roles = ["legado", "modernizado"]
}

# Permisos mínimos para las instancias: pull de ECR y escritura de logs/métricas en
# CloudWatch. Nada de AdministratorAccess ni policies gestionadas amplias.
resource "aws_iam_role" "ec2" {
  name = "spring-petclinic-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "ecr_pull" {
  name = "ecr-pull"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchCheckLayerAvailability",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "cloudwatch" {
  name = "cloudwatch-logs-metrics"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
        ]
        Resource = "arn:aws:logs:*:*:log-group:/spring-petclinic/*"
      },
      {
        Effect   = "Allow"
        Action   = "cloudwatch:PutMetricData"
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "spring-petclinic-ec2-profile"
  role = aws_iam_role.ec2.name
}

resource "aws_instance" "app" {
  count                       = var.instance_count
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = var.public_subnet_ids[count.index % length(var.public_subnet_ids)]
  vpc_security_group_ids      = [var.app_security_group_id]
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    aws_region         = var.aws_region
    ecr_repository_url = var.ecr_repository_url
  })

  tags = {
    Name = "spring-petclinic-${count.index < length(local.roles) ? local.roles[count.index] : "extra-${count.index}"}"
    Rol  = count.index < length(local.roles) ? local.roles[count.index] : "extra"
  }
}
