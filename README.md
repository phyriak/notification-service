# Notification Service - Deployment & Infrastructure Documentation

## Overview

Notification Service is a Spring Boot microservice deployed to a Hetzner VPS using Docker and GitHub Actions CI/CD.

Current deployment differs from Payment Service:

| Service              | Deployment     |
| -------------------- | -------------- |
| payment-service      | Docker Compose |
| notification-service | Docker Run     |

The long-term goal is to standardize deployment and infrastructure across all services.

---

# Infrastructure

## Environment

### VPS

Provider: Hetzner Cloud

Server:

* Ubuntu 24.04
* Docker installed
* SSH key authentication only
* UFW Firewall enabled

---

# CI/CD

Deployment is fully automated through GitHub Actions.

Pipeline flow:

```text
git push
    ↓
GitHub Actions
    ↓
mvn clean package
    ↓
docker build
    ↓
docker push
    ↓
SSH to Hetzner
    ↓
docker pull
    ↓
docker stop/rm
    ↓
docker run
    ↓
Health Check
```

---

# Docker Hub

Images are published to Docker Hub.

Repository:

```text
phyriak/notification-service
```

Tags:

```text
latest
github.sha
```

---

# GitHub Secrets

## Docker Hub

Used for image publishing.

```text
DOCKER_USERNAME
DOCKER_PASSWORD
```

---

## Hetzner Deployment

Used by GitHub Actions to connect to VPS.

```text
HETZNER_HOST
HETZNER_USER
HETZNER_SSH_KEY
```

---

## Database Configuration

Used by Notification Service.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
DB_SCHEMA
```

---

## Mail Configuration

Used by Notification Service SMTP integration.

```text
EMAIL_USER
EMAIL_PASSWORD
```

---

## Spring Profiles

Used to activate environment-specific configuration.

```text
SPRING_PROFILES_ACTIVE
```

Current value:

```text
uat
```

---

# Notification Service Deployment

Current deployment uses Docker directly.

Example:

```bash
docker pull phyriak/notification-service:latest

docker stop notification-service || true
docker rm notification-service || true

docker run -d \
  --name notification-service \
  --restart unless-stopped \
  -p 8089:8089 \
  -e SPRING_PROFILES_ACTIVE=uat \
  -e DB_URL=... \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e DB_SCHEMA=... \
  -e EMAIL_USER=... \
  -e EMAIL_PASSWORD=... \
  phyriak/notification-service:latest
```

Health endpoint:

```text
http://<host>:8089/actuator/health
```

---

# Spring Profiles

Notification Service requires:

```text
SPRING_PROFILES_ACTIVE=uat
```

Without an active profile Spring Boot starts using:

```text
default
```

which causes datasource configuration to be unavailable.

---

# Database Migrations

Current state:

## payment-service

Migration tool:

```text
Flyway
```

Deployment:

```text
Docker Compose
```

---

## notification-service

Migration tool:

```text
Liquibase
```

Deployment:

```text
Docker Run
```

---

# Current Architecture

```text
GitHub Repository
        │
        ▼
GitHub Actions
        │
        ▼
Docker Hub
        │
        ▼
Hetzner VPS
        │
        ▼
Docker
        │
        ▼
Notification Service
        │
        ▼
PostgreSQL
```

---

# Future Improvements

## Deployment Alignment

Standardize deployment model across services:

Option A:

* Docker Compose everywhere

Option B:

* Kubernetes everywhere

---

## Migration Alignment

Current:

```text
payment-service      -> Flyway
notification-service -> Liquibase
```

Target:

```text
Single migration framework
```

Most likely:

```text
Flyway
```

---

## Planned Roadmap

1. Deployment standardization
2. Database migration standardization
3. Shared configuration approach
4. k3s on Hetzner
5. Helm Charts
6. GitHub Actions + Helm Deployments
7. ArgoCD (GitOps)
8. Centralized Secret Management
9. Full Kubernetes Platform

```
```
