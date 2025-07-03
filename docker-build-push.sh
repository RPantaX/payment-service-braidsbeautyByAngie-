#!/bin/bash

# docker-build-push.sh - Script para construir y subir imagen a Docker Hub

set -e

# Configuración - CAMBIA ESTOS VALORES
DOCKER_HUB_USERNAME="rpantax"  # Cambia por tu username real
IMAGE_NAME="payment-service"
VERSION="1.0.0"
LATEST_TAG="latest"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Funciones de utilidad
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️ $1${NC}"; }
print_header() { echo -e "${BLUE}🔨 $1${NC}"; }

# Variables calculadas
FULL_IMAGE_NAME="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}"
VERSIONED_TAG="${FULL_IMAGE_NAME}:${VERSION}"
LATEST_FULL_TAG="${FULL_IMAGE_NAME}:${LATEST_TAG}"

echo "🐳 Docker Build & Push Script para payment Service"
echo "=================================================="
echo ""

# Verificaciones iniciales
print_header "1. Verificaciones iniciales"

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ] || [ ! -f "Dockerfile" ]; then
    print_error "No se encontró pom.xml o Dockerfile. Asegúrate de estar en la raíz del proyecto."
    exit 1
fi
print_success "Archivos principales encontrados"

# Verificar Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker no está instalado o no está en PATH"
    exit 1
fi
print_success "Docker está disponible"

# Verificar login en Docker Hub
if ! docker info | grep -q "Username"; then
    print_warning "No estás logueado en Docker Hub"
    print_info "Ejecutando 'docker login'..."
    docker login
else
    print_success "Ya estás logueado en Docker Hub"
fi

echo ""

# Compilar proyecto
print_header "2. Compilando proyecto Maven"

print_info "Limpiando proyecto..."
mvn clean -q

print_info "Compilando proyecto..."
if mvn compile -q; then
    print_success "Compilación exitosa"
else
    print_error "Error en compilación"
    exit 1
fi

print_info "Ejecutando tests..."
if mvn test -q; then
    print_success "Tests ejecutados correctamente"
else
    print_warning "Algunos tests fallaron, continuando..."
fi

print_info "Generando JAR..."
if mvn package -DskipTests -q; then
    print_success "JAR generado exitosamente"
else
    print_error "Error generando JAR"
    exit 1
fi

# Verificar JAR
JAR_PATH="target/payment-service-0.0.1-SNAPSHOT.jar"
if [ -f "$JAR_PATH" ]; then
    JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
    print_success "JAR encontrado: $JAR_PATH ($JAR_SIZE)"
else
    print_error "JAR no encontrado en $JAR_PATH"
    print_info "Archivos en target/:"
    ls -la target/ || true
    exit 1
fi

echo ""

# Construir imagen Docker
print_header "3. Construyendo imagen Docker"

print_info "Construyendo imagen: $VERSIONED_TAG"
if docker build -t "$VERSIONED_TAG" .; then
    print_success "Imagen construida exitosamente"
else
    print_error "Error construyendo imagen Docker"
    exit 1
fi

# Tagear como latest
print_info "Creando tag 'latest'..."
docker tag "$VERSIONED_TAG" "$LATEST_FULL_TAG"
print_success "Tag 'latest' creado"

# Verificar imágenes
print_info "Imágenes creadas:"
docker images | grep "$DOCKER_HUB_USERNAME/$IMAGE_NAME" | head -5

echo ""

# Probar imagen localmente
print_header "4. Probando imagen localmente"

print_info "Iniciando contenedor de prueba..."
CONTAINER_ID=$(docker run -d -p 8082:8081 --name payment-service-test "$VERSIONED_TAG")

if [ $? -eq 0 ]; then
    print_success "Contenedor iniciado: $CONTAINER_ID"

    print_info "Esperando que el servicio inicie (30 segundos)..."
    sleep 30

    # Verificar que el contenedor sigue corriendo
    if docker ps | grep -q "payment-service-test"; then
        print_success "Contenedor ejecutándose correctamente"

        # Intentar hacer health check
        print_info "Verificando health check..."
        if docker exec payment-service-test curl -f http://localhost:8081/actuator/health 2>/dev/null; then
            print_success "Health check exitoso"
        else
            print_warning "Health check falló, pero el contenedor está corriendo"
        fi

        # Mostrar logs del contenedor
        print_info "Últimas líneas del log:"
        docker logs --tail 10 payment-service-test

    else
        print_error "El contenedor se detuvo inesperadamente"
        print_info "Logs del contenedor:"
        docker logs payment-service-test
    fi

    # Limpiar contenedor de prueba
    print_info "Limpiando contenedor de prueba..."
    docker stop payment-service-test >/dev/null 2>&1 || true
    docker rm payment-service-test >/dev/null 2>&1 || true
    print_success "Contenedor de prueba limpiado"

else
    print_error "No se pudo iniciar el contenedor de prueba"
    exit 1
fi

echo ""

# Push a Docker Hub
print_header "5. Subiendo a Docker Hub"

print_info "Subiendo imagen versionada: $VERSIONED_TAG"
if docker push "$VERSIONED_TAG"; then
    print_success "Imagen versionada subida exitosamente"
else
    print_error "Error subiendo imagen versionada"
    exit 1
fi

print_info "Subiendo imagen latest: $LATEST_FULL_TAG"
if docker push "$LATEST_FULL_TAG"; then
    print_success "Imagen 'latest' subida exitosamente"
else
    print_error "Error subiendo imagen 'latest'"
    exit 1
fi

echo ""

# Verificación final
print_header "6. Verificación final"

print_info "Verificando imagen en Docker Hub..."
sleep 5

# Limpiar imagen local y descargar desde Docker Hub para verificar
print_info "Limpiando imágenes locales..."
docker rmi "$VERSIONED_TAG" "$LATEST_FULL_TAG" >/dev/null 2>&1 || true

print_info "Descargando imagen desde Docker Hub para verificar..."
if docker pull "$LATEST_FULL_TAG"; then
    print_success "Imagen verificada en Docker Hub"
else
    print_error "No se pudo descargar la imagen desde Docker Hub"
    exit 1
fi

echo ""

# Resumen final
print_header "🎉 ¡Proceso completado exitosamente!"
echo ""
print_success "Tu imagen está disponible en Docker Hub:"
echo "   📦 Imagen: $FULL_IMAGE_NAME"
echo "   🏷️  Tags: $VERSION, latest"
echo "   🌐 URL: https://hub.docker.com/r/$DOCKER_HUB_USERNAME/$IMAGE_NAME"
echo ""
print_info "Comandos para usar tu imagen:"
echo "   docker pull $LATEST_FULL_TAG"
echo "   docker run -p 8081:8081 $LATEST_FULL_TAG"
echo ""
print_info "Próximos pasos:"
echo "   1. Actualiza el Jenkinsfile con: DOCKER_HUB_REPO = '$FULL_IMAGE_NAME'"
echo "   2. Configura las credenciales de Docker Hub en Jenkins"
echo "   3. Prueba el pipeline de Jenkins"

# Limpiar imágenes locales para ahorrar espacio
print_info "Limpiando imágenes locales..."
docker image prune -f >/dev/null 2>&1 || true
print_success "Limpieza completada"