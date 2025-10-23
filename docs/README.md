# 🏦 Sistema de Gestão de Benefícios

Sistema de gerenciamento de benefícios com transferências seguras entre contas, implementando correção de bug de concorrência (Lost Update Problem) através de **Optimistic Locking**.

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e Execução](#instalação-e-execução)
- [Endpoints da API](#endpoints-da-api)

---

## 🎯 Sobre o Projeto

Este projeto é uma solução para o desafio técnico de correção de bug de concorrência em transferências financeiras. O sistema permite:

- ✅ CRUD completo de benefícios
- ✅ Transferências seguras entre benefícios
- ✅ Prevenção de **Lost Update Problem** via Optimistic Locking
- ✅ Idempotência em transferências
- ✅ Integração Spring Boot + EJB
- ✅ Retry automático em conflitos de concorrência

## 🏗️ Arquitetura

### Visão Geral
```
┌─────────────────────┐         ┌──────────────────────┐
│   Cliente REST      │         │   PostgreSQL         │
│   (Frontend/Mobile) │         │   (Banco de Dados)   │
└──────────┬──────────┘         └──────────┬───────────┘
           │                               │
           │ HTTP/JSON                     │ JDBC
           ↓                               ↓
┌─────────────────────────────────────────────────────┐
│              BACKEND-MODULE (Spring Boot)           │
│                                                     │
│  ┌─────────────┐    ┌──────────────┐              │
│  │ Controller  │───→│   Service    │              │
│  └─────────────┘    └──────┬───────┘              │
│                            │                        │
│                            ↓                        │
│                    ┌──────────────┐                │
│                    │ EJB Client   │                │
│                    └──────┬───────┘                │
│                            │                        │
└────────────────────────────┼────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────┐
│              EJB-MODULE (Sistema Legado)            │
│                                                     │
│  ┌──────────────────────┐                          │
│  │ BeneficioEjbService  │                          │
│  │  - transfer()        │ ← Lógica crítica         │
│  │  - Optimistic Lock   │   de negócio             │
│  │  - Validações        │                          │
│  └──────────────────────┘                          │
└─────────────────────────────────────────────────────┘
```

### Módulos

#### **1. ejb-module** (Sistema Legado)
- **Responsabilidade:** Lógica de negócio crítica de transferências
- **Tecnologia:** Jakarta EE (EJB 3.2), JPA
- **Status:** Sistema legado compartilhado por múltiplas aplicações

#### **2. backend-module** (Nova API REST)
- **Responsabilidade:** API REST, integração com EJB, idempotência
- **Tecnologia:** Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- **Status:** Sistema novo consumindo o legado

---

## 🛠️ Tecnologias

### Backend

| Tecnologia | Versão | Finalidade |
|------------|--------|------------|
| **Java** | 17 | Linguagem principal |
| **Spring Boot** | 3.2.5 | Framework web |
| **Jakarta EE** | 10.0.0 | EJB (sistema legado) |
| **PostgreSQL** | 15 | Banco de dados |
| **Flyway** | 9.x | Migrations de banco |
| **Hibernate** | 6.2.7 | ORM/JPA |
| **Spring Retry** | 2.0.x | Retry automático |
| **SpringDoc OpenAPI** | 2.3.0 | Documentação Swagger |
| **SLF4J + Logback** | 2.0.x | Logging |

### Testes

| Tecnologia | Finalidade |
|------------|------------|
| **JUnit 5** | Framework de testes |
| **Mockito** | Mocks para testes unitários |
| **AssertJ** | Assertions fluentes |
| **Spring Boot Test** | Testes de integração |
| **H2 Database** | Banco em memória para testes unitários |

### DevOps

| Tecnologia | Finalidade |
|------------|------------|
| **Docker** | Containerização do PostgreSQL |
| **Docker Compose** | Orquestração de containers |
| **Maven** | Build e gerenciamento de dependências |

---

## ⚙️ Pré-requisitos

Certifique-se de ter instalado:

- ✅ **Java 17** ou superior ([Download](https://adoptium.net/))
- ✅ **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- ✅ **Docker** e **Docker Compose** ([Download](https://www.docker.com/products/docker-desktop))
- ✅ **Git** ([Download](https://git-scm.com/downloads))
- ✅ **IntelliJ IDEA** (opcional, mas recomendado)

---

## 🚀 Instalação e Execução

### 1️⃣ Clonar o Repositório
```bash
git clone https://github.com/seu-usuario/bip-teste-integrado.git
cd bip-teste-integrado
```

### 2️⃣ Subir o PostgreSQL com Docker Compose
```bash
# Na raiz do projeto
cd docker
docker-compose up -d

# Verificar se está rodando
docker-compose ps
```

### 3️⃣ Compilar o Projeto
```bash
# Na raiz do projeto
mvn clean install
```

### 4️⃣ Rodar a Aplicação

#### **Opção A: Via IntelliJ IDEA**

1. Abra o projeto no IntelliJ
2. Localize a classe: `backend-module/src/main/java/com/example/backend/BackendApplication.java`
3. Clique com botão direito → **Run 'BackendApplication'**

#### **Opção B: Via Terminal**
```bash
cd backend-module
mvn spring-boot:run
```

#### **Swagger UI:**
```
http://localhost:8080/swagger-ui/index.html
```

### Rodar Todos os Testes
```bash
# Testes unitários + integração
mvn test

# Com relatório de cobertura (Jacoco)
mvn clean test jacoco:report

# Visualizar relatório HTML
open target/site/jacoco/index.html
```

## 📡 Endpoints da API

### Base URL
```
http://localhost:8080/api/v1
```

---

## 🐳 Docker Commands
```bash
# Subir o PostgreSQL
docker-compose up -d

# Parar o PostgreSQL
docker-compose down

# Ver logs
docker-compose logs -f postgres

# Reiniciar
docker-compose restart

# Remover volumes (CUIDADO: apaga dados)
docker-compose down -v

---

## 📊 Monitoramento

### Actuator Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Info da aplicação
curl http://localhost:8080/actuator/info
```

---

## 📚 Referências

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Jakarta EE Specification](https://jakarta.ee/)
- [JPA Optimistic Locking](https://docs.oracle.com/javaee/7/tutorial/persistence-locking.htm)
- [Spring Retry](https://github.com/spring-projects/spring-retry)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---