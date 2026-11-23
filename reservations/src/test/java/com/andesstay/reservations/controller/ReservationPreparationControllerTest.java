package com.andesstay.reservations.controller;

import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.event.ReservationEventProducer;
import com.andesstay.reservations.messaging.RabbitMqPublisherService;
import com.andesstay.reservations.messaging.ReservationNotification;
import com.andesstay.reservations.repository.ReservationRepository;
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
class ReservationPreparationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReservationRepository reservationRepository;

	@MockitoBean
	private ReservationEventProducer reservationEventProducer;

	@MockitoBean
	private RabbitMqPublisherService rabbitMqPublisherService;

	private Long createReservation() throws Exception {
		CreateReservationRequest request = new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), new BigDecimal("120000"),
				"Cabana A", 2, "Juan Perez", "juan@example.com");
		MvcResult result = mockMvc.perform(post("/api/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.preparationStatus").value("NONE"))
				.andReturn();
		JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
		return node.get("id").asLong();
	}

	private Long createConfirmedReservation() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("PENDING"));
		return id;
	}

	@Test
	void confirmarReservaDejaPreparationEnPending() throws Exception {
		Long id = createReservation();

		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMADA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.preparationStatus").value("PENDING"));

		mockMvc.perform(get("/api/reservations/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("PENDING"));
	}

	@Test
	void preparacionPendienteAInProgressDevuelve200() throws Exception {
		Long id = createConfirmedReservation();
		clearInvocations(rabbitMqPublisherService);

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("IN_PROGRESS"));

		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void preparacionInProgressAReadyPublicaUnaVez() throws Exception {
		Long id = createConfirmedReservation();
		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isOk());
		clearInvocations(rabbitMqPublisherService);

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("READY"));

		verify(rabbitMqPublisherService, times(1)).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void preparacionPendienteAReadyPublicaUnaVez() throws Exception {
		Long id = createConfirmedReservation();
		clearInvocations(rabbitMqPublisherService);

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("READY"));

		verify(rabbitMqPublisherService, times(1)).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void repetirReadyNoPublicaOtraVez() throws Exception {
		Long id = createConfirmedReservation();
		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isOk());
		clearInvocations(rabbitMqPublisherService);

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preparationStatus").value("READY"));

		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void headerActorSePropagaAlMensajeCabinReady() throws Exception {
		Long id = createConfirmedReservation();
		clearInvocations(rabbitMqPublisherService);

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.header("X-Actor-Id", "operador-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isOk());

		ArgumentCaptor<ReservationNotification> captor = ArgumentCaptor.forClass(ReservationNotification.class);
		verify(rabbitMqPublisherService).publishCabinReady(captor.capture());
		assertEquals("operador-1", captor.getValue().traceId());
	}

	@Test
	void estadosDeEntradaNoPermitidosDevuelven400() throws Exception {
		Long id = createConfirmedReservation();

		for (String status : new String[] {"NONE", "PENDING", "DESCONOCIDO"}) {
			mockMvc.perform(put("/api/reservations/{id}/preparation", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"" + status + "\"}"))
					.andExpect(status().isBadRequest());
		}
		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void bodySinStatusDevuelve400() throws Exception {
		Long id = createConfirmedReservation();

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void reservaCreadaNoAceptaPreparacionYDevuelve409() throws Exception {
		Long id = createReservation();

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reservaCanceladaNoAceptaPreparacionYDevuelve409() throws Exception {
		Long id = createReservation();
		mockMvc.perform(put("/api/reservations/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CANCELADA\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reservaCheckoutNoAceptaPreparacionYDevuelve409() throws Exception {
		Long id = createConfirmedReservation();
		for (String status : new String[] {"CHECKIN_PENDIENTE", "EN_ESTADIA", "CHECKOUT"}) {
			mockMvc.perform(put("/api/reservations/{id}/status", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"" + status + "\"}"))
					.andExpect(status().isOk());
		}

		mockMvc.perform(put("/api/reservations/{id}/preparation", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"READY\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reservaInexistenteDevuelve404() throws Exception {
		mockMvc.perform(put("/api/reservations/999999/preparation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isNotFound());
	}
}
