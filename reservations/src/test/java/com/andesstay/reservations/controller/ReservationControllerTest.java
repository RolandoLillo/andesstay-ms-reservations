package com.andesstay.reservations.controller;

import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.event.ReservationAction;
import com.andesstay.reservations.event.ReservationEvent;
import com.andesstay.reservations.event.ReservationEventProducer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReservationEventProducer reservationEventProducer;

	private Long createReservation() throws Exception {
		return createReservation("user-1", 10L);
	}

	private Long createReservation(String customerUserId, Long unitId) throws Exception {
		CreateReservationRequest request = new CreateReservationRequest(customerUserId, unitId,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez");
		MvcResult result = mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CREADA"))
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		return node.get("id").asLong();
	}

	@Test
	void crearReservaDevuelve201ConEstadoCreada() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez");
		mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.customerUserId").value("user-1"))
				.andExpect(jsonPath("$.unitId").value(10))
				.andExpect(jsonPath("$.unitName").value("Cabana A"))
				.andExpect(jsonPath("$.guests").value(2))
				.andExpect(jsonPath("$.customerName").value("Juan Perez"))
				.andExpect(jsonPath("$.status").value("CREADA"));
	}

	@Test
	void crearReservaPersisteCamposEnriquecidos() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-2", 42L,
				LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), new BigDecimal("85000"),
				"Cabana Deluxe", 4, "Maria Soto");
		MvcResult result = mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		org.junit.jupiter.api.Assertions.assertEquals("user-2", node.get("customerUserId").asText());
		org.junit.jupiter.api.Assertions.assertEquals(42, node.get("unitId").asLong());
		org.junit.jupiter.api.Assertions.assertEquals("Cabana Deluxe", node.get("unitName").asText());
		org.junit.jupiter.api.Assertions.assertEquals(4, node.get("guests").asInt());
		org.junit.jupiter.api.Assertions.assertEquals("Maria Soto", node.get("customerName").asText());
		org.junit.jupiter.api.Assertions.assertEquals("CREADA", node.get("status").asText());
	}

	@Test
	void crearReservaConCustomerUserIdNumericoComoStringDevuelve201() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("12345", 7L,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), new BigDecimal("50000"),
				"Cabana B", 1, "Pedro Gomez");
		mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.customerUserId").value("12345"))
				.andExpect(jsonPath("$.status").value("CREADA"));
	}

	@Test
	void obtenerReservaDevuelve200() throws Exception {
		Long id = createReservation();
		mockMvc.perform(get("/api/reservations/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.unitName").value("Cabana A"))
				.andExpect(jsonPath("$.guests").value(2))
				.andExpect(jsonPath("$.customerName").value("Juan Perez"));
	}

	@Test
	void obtenerReservaInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/reservations/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listarReservasDevuelve200() throws Exception {
		createReservation();
		mockMvc.perform(get("/api/reservations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void cambiarEstadoAConfirmadaDevuelve200() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"));
	}

	@Test
	void cicloCompletoHastaCheckout() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CHECKIN_PENDIENTE\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EN_ESTADIA\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CHECKOUT\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CHECKOUT"));
	}

	@Test
	void reglaDeNegocioBloqueaCheckinSinConfirmarCon409() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CHECKIN_PENDIENTE\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reglaDeNegocioBloqueaSaltoDirectoACheckoutCon409() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CHECKOUT\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void cancelarDesdeCreadaDevuelve200() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CANCELADA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADA"));
	}

	@Test
	void crearReservaConFechasInvalidasDevuelve400() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 15), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez");
		mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crearReservaConFechasIgualesDevuelve400() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez");
		mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crearReservaConGuestsInvalidosDevuelve400() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), new BigDecimal("120000"),
				"Cabana A", 0, "Juan Perez");
		mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cambiarEstadoDeReservaInexistenteDevuelve404() throws Exception {
		mockMvc.perform(put("/api/reservations/999999/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void crearReservaPublicaEventoReservationCreated() throws Exception {
		clearInvocations(reservationEventProducer);
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez");
		MvcResult result = mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		Long id = node.get("id").asLong();

		ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
		verify(reservationEventProducer, times(1)).publish(captor.capture());
		ReservationEvent event = captor.getValue();
		assertEquals(ReservationAction.RESERVATION_CREATED, event.action());
		assertEquals("RESERVATION", event.resourceType());
		assertEquals(id, event.resourceId());
		assertEquals("user-1", event.actor());
		assertNotNull(event.eventId());
		assertNotNull(event.occurredAt());
		assertEquals("CREADA", event.detail().get("newStatus"));
		assertNull(event.detail().get("previousStatus"));
		assertEquals("Cabana A", event.detail().get("unitName"));
	}

	@Test
	void cambiarEstadoPublicaEventoReservationStatusChanged() throws Exception {
		Long id = createReservation();
		clearInvocations(reservationEventProducer);

		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isOk());

		ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
		verify(reservationEventProducer, times(1)).publish(captor.capture());
		ReservationEvent event = captor.getValue();
		assertEquals(ReservationAction.RESERVATION_STATUS_CHANGED, event.action());
		assertEquals("RESERVATION", event.resourceType());
		assertEquals(id, event.resourceId());
		assertEquals("system", event.actor());
		assertNotNull(event.eventId());
		assertNotNull(event.occurredAt());
		assertEquals("CREADA", event.detail().get("previousStatus"));
		assertEquals("CONFIRMADA", event.detail().get("newStatus"));
	}

	@Test
	void cambioDeEstadoInvalidoNoPublicaEvento() throws Exception {
		Long id = createReservation();
		clearInvocations(reservationEventProducer);

		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CHECKOUT\"}"))
				.andExpect(status().isConflict());

		verify(reservationEventProducer, never()).publish(any(ReservationEvent.class));
	}
}
