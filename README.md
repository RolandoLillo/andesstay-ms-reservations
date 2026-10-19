# AndesStay — Microservicio de Reservas (`ms-reservations`)

Este microservicio gestiona la lógica de negocio, ciclo de vida, persistencia y máquina de estados para las reservas del sistema **AndesStay**.

---

## 🏛️ Lógica de Dominio y Máquina de Estados

El servicio administra las reservas a través de las siguientes etapas validadas por reglas de negocio:

* **CREADA:** Reserva registrada inicialmente en el sistema.
* **CONFIRMADA:** Pago o garantía validada.
* **CHECKIN_PENDIENTE:** A la espera del ingreso del huésped.
* **EN_ESTADIA:** Huésped actualmente alojado.
* **CHECKOUT:** Reserva finalizada con éxito.
* **CANCELADA:** Reserva anulada previa a la estadía.

---

## 🛠️ Stack Tecnológico

* **Java:** 17 (LTS)
* **Framework:** Spring Boot 3.x
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:** PostgreSQL 16 (Perfil `prod`) / H2 In-Memory (Perfil `dev`)
* **Construcción:** Maven

---

## ⚙️ Perfiles de Configuración

### 1. Perfil Desarrollo (`dev`)

Utiliza base de datos H2 en memoria para pruebas rápidas aisladas.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Perfil Producción (`prod`)

Se conecta a la base de datos PostgreSQL (`postgres-db`) mediante las siguientes variables de entorno:

| Variable      | Descripción                        | Valor por Defecto (Compose)                  |
| :------------ | :--------------------------------- | :------------------------------------------- |
| `DB_URL`      | URL JDBC de PostgreSQL             | `jdbc:postgresql://postgres-db:5432/andesstay_db` |
| `DB_USERNAME` | Usuario de BD                      | `andesstay_user`                             |
| `DB_PASSWORD` | Contraseña de BD                   | `andesstay_password`                         |

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 🚀 Endpoints Expuestos

| Método | Endpoint                        | Descripción                         | Acceso / Auth        |
| :----- | :------------------------------ | :---------------------------------- | :------------------- |
| `GET`  | `/api/reservations/health`      | Estado del microservicio (200 OK)   | Público              |
| `GET`  | `/api/reservations`             | Listar reservas registradas         | Requiere Token (BFF) |
| `POST` | `/api/reservations`             | Crear nueva reserva                 | Requiere Token (BFF) |
| `GET`  | `/api/reservations/{id}`        | Consultar detalle de reserva por ID | Requiere Token (BFF) |
| `PUT`  | `/api/reservations/{id}/status` | Actualizar estado de la reserva     | Requiere Token (BFF) |

---

## 🐇 Integración con RabbitMQ (`ms-notify`)

Al **confirmar** una reserva (transición a `CONFIRMADA`) el servicio publica tres mensajes en el
exchange `cmd.direct`, consumidos por `ms-notify`:

| Routing key           | Consumidor                        | Payload mínimo                                             |
| :-------------------- | :-------------------------------- | :--------------------------------------------------------- |
| `email.send`          | `GuestNotificationService`        | `reservationId`, `guestName`, `kind=CONFIRMATION`, `channel=EMAIL`, `guestEmail` |
| `housekeeping.ticket` | `HousekeepingTicketService`       | `reservationId`, `unitId`, `kind=PREPARATION`              |
| `voucher.gen`         | `VoucherService`                  | `reservationId`, `guestName`, `document=VOUCHER`           |

El cuerpo es un `NotificationEnvelope` (contrato compartido con `ms-notify`):
`type` (= routing key), `eventId`, `timestamp`, `traceId`, `correlationId` y `payload`.

Notas de comportamiento:

* La **creación** de una reserva (`CREADA`) **no** publica en RabbitMQ; solo el evento Kafka `RESERVATION_CREATED`.
* La publicación es **best-effort**: si RabbitMQ no está disponible la confirmación **no** se revierte,
  se registra el error en logs y el usuario recibe `200 OK`. (El productor Kafka es estricto: si falla
  devuelve `503` y hace rollback.)
* `guestEmail` es **opcional** en `POST /api/reservations`. Si no se envía, se omite únicamente el
  mensaje `email.send` (se siguen publicando `housekeeping.ticket` y `voucher.gen`).

Variables de entorno (con valores por defecto para `dev`):

| Variable                | Descripción           | Valor por Defecto |
| :---------------------- | :-------------------- | :---------------- |
| `SPRING_RABBITMQ_HOST`  | Host del broker       | `localhost`       |
| `SPRING_RABBITMQ_PORT`  | Puerto del broker     | `5672`            |
| `RABBITMQ_DEFAULT_USER` | Usuario               | `guest`           |
| `RABBITMQ_DEFAULT_PASS` | Contraseña            | `guest`           |

El exchange (`cmd.direct`) y las routing keys (`email.send`, `housekeeping.ticket`, `voucher.gen`)
se configuran en `application.properties` (`rabbitmq.exchange.cmd`, `rabbitmq.routing.*`).

---

## 🐳 Ejecución con Docker

El servicio está empaquetado para ser orquestado junto a PostgreSQL dentro de la red unificada de infraestructura.

```bash
# Construcción del ejecutable y contenedor
docker build -t andesstay/ms-reservations:latest .
```