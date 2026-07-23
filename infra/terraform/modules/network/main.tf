data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "spring-petclinic-vpc"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "spring-petclinic-igw"
  }
}

# 2 subredes públicas y 2 privadas en AZs distintas: public[i]/private[i] comparten AZ,
# de modo que cada AZ tiene un par público/privado.
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "spring-petclinic-public-${count.index}"
    Tier = "public"
  }
}

resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "spring-petclinic-private-${count.index}"
    Tier = "private"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "spring-petclinic-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Las subredes privadas no tienen ruta a un NAT Gateway: para el alcance de este
# experimento (RDS sin acceso público) no necesitan salida a internet. Si una fase futura
# necesita que EC2/tareas en subred privada descarguen paquetes, se añadirá un NAT Gateway.
resource "aws_security_group" "app" {
  name        = "spring-petclinic-app-sg"
  description = "Trafico HTTP de la aplicacion desde internet"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "App HTTP"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "spring-petclinic-app-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "spring-petclinic-rds-sg"
  description = "Postgres accesible unicamente desde el security group de la app"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Postgres desde la app"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "spring-petclinic-rds-sg"
  }
}
