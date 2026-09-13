# 🏨 AndesStay — Microservicio de Reservas (`ms-andesstay-reservations`)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

Servicio de dominio encargado de la gestión integral de reservas, control de estados del ciclo de hospedaje y coordinación de disponibilidad para la plataforma **AndesStay**.

---

## 👥 Integrantes del Equipo

* **Cristian Monsalve** — *Identidad, Frontend Angular & GitHub Project*
* **Héctor Olivares** — *BFF, Catálogo & Seguridad Backend*
* **Rolando Lillo** — *API Gateway, Reservas & Despliegue Infraestructura* (Dueño del servicio)

---

## 🏗️ Arquitectura y Flujo de Llamadas

Este microservicio se despliega de manera privada dentro de la red interna de AWS EC2 / Docker y **nunca es consumido directamente por el cliente**.

Cliente (Angular + MSAL) ──► AWS API Gateway (JWT Authorizer) ──► ms-andesstay-bff ──► ms-andesstay-reservations


---

## 🛠️ Tecnologías

* **Lenguaje & Framework:** Java 17 / Spring Boot 4.1.1 (Spring WebMVC, Spring Data JPA)
* **Base de Datos:** Oracle Database (Nube) / PostgreSQL / H2 (Local)
* **Pruebas:** JUnit 5, MockMvc
* **Contenerización:** Docker / Docker Compose (Build multi-stage con Alpine JRE)

---

## 📌 Contrato de Endpoints y Roles (`/api/reservations/*`)

| Método | Endpoint | Roles Permitidos | Descripción / Regla |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/reservations/health` | Público | Health check del servicio. |
| `GET` | `/api/reservations` | Admin, Operador, Cliente | Lista reservas (Cliente solo ve las suyas). |
| `POST` | `/api/reservations` | Operador, Cliente | Crea una reserva en estado `CREADA`. |
| `GET` | `/api/reservations/{id}` | Admin, Operador, Cliente | Detalle de reserva (Cliente solo si es dueño). |
| `PUT` | `/api/reservations/{id}/status` | Admin, Operador | Cambia el estado de la reserva. |

### 🔄 Máquina de Estados del Ciclo de Vida
CREADA ──► CONFIRMADA ──► CHECKIN_PENDIENTE ──► EN_ESTADÍA ──► CHECKOUT
│           │
└──► CANCELADA ◄──┘

> ⚠️ **Regla de Negocio Crítica:** No se permite realizar check-in (`CHECKIN_PENDIENTE` / `EN_ESTADÍA`) si la reserva no fue previamente **CONFIRMADA**.

---

## 🚀 Ejecución Local

### Prerrequisitos
* Java 17 JDK
* Maven 3.9+ (o usar `./mvnw`)
* Docker Desktop (opcional para pruebas en contenedor)

### 1. Compilación y Pruebas Unitarias
```bash
./mvnw clean verify
```

### 2. Ejecutar con Spring Boot
```bash
./mvnw spring-boot:run
```

El servicio estará disponible en http://localhost:8082.

---

## 🐳 Ejecución con Docker

### Construir la imagen local
```bash
docker build -t andesstay-reservations .
```

### Ejecutar contenedor
```bash
docker run -d -p 8082:8082 --name ms-reservations andesstay-reservations
```

---

## 🔑 Variables de Entorno

| Variable | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8082` | Puerto interno de escucha. |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo (dev, prod). |
| `DB_URL` | `jdbc:h2:mem:reservationsdb` | URL de conexión a la base de datos. |