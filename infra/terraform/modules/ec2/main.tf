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

# DESVIACIÓN (ver ESTADO-PLAN.md / README.md): el diseño original de esta fase creaba un
# aws_iam_role + aws_iam_instance_profile propios, de mínimo privilegio. La cuenta real
# usada para desplegar (AWS Academy Learner Lab) deniega iam:CreateRole por política de la
# cuenta (verificado con un create-role de prueba: AccessDenied), y solo permite reutilizar
# el rol/instance profile pre-aprovisionado "LabRole"/"LabInstanceProfile". Ese rol ya trae
# adjunta AmazonEC2ContainerRegistryReadOnly (cubre el pull de ECR que pedía el diseño
# original) más otras policies del laboratorio con permisos más amplios de lo que el
# principio de mínimo privilegio hubiera elegido — es una limitación de la cuenta, no una
# decisión de diseño de este módulo.
resource "aws_instance" "app" {
  count                       = var.instance_count
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = var.public_subnet_ids[count.index % length(var.public_subnet_ids)]
  vpc_security_group_ids      = [var.app_security_group_id]
  iam_instance_profile        = var.instance_profile_name
  associate_public_ip_address = true

  # La AMI base de este AWS Academy Learner Lab trae solo 2 GB de disco raíz (verificado con
  # `aws ec2 describe-images`), insuficiente para instalar docker + amazon-cloudwatch-agent
  # (~517 MB instalados) más el propio SO: el primer intento de apply falló silenciosamente
  # a mitad del user_data (dnf sin espacio, "needs 49MB more space"), dejando la instancia
  # arriba pero sin el contenedor de la app corriendo. Se fuerza un volumen mayor.
  root_block_device {
    volume_size = var.root_volume_size
    volume_type = "gp3"
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    aws_region         = var.aws_region
    ecr_repository_url = var.ecr_repository_url
  })

  tags = {
    Name = "spring-petclinic-${count.index < length(local.roles) ? local.roles[count.index] : "extra-${count.index}"}"
    Rol  = count.index < length(local.roles) ? local.roles[count.index] : "extra"
  }
}
