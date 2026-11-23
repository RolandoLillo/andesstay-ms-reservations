package com.andesstay.reservations.service;

import com.andesstay.reservations.domain.PreparationStatus;
import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;
import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.event.ReservationEvent;
import com.andesstay.reservations.event.ReservationEventProducer;
import com.andesstay.reservations.exception.InvalidPreparationTransitionException;
import com.andesstay.reservations.exception.ReservationNotFoundException;
import com.andesstay.reservations.messaging.RabbitMqPublisherService;
import com.andesstay.reservations.messaging.ReservationNotification;
import com.andesstay.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private static final String SYSTEM_ACTOR = "system";

	private static final Set<ReservationStatus> PREPARATION_ALLOWED_RESERVATION_STATUSES = EnumSet.of(
			ReservationStatus.CONFIRMADA, ReservationStatus.CHECKIN_PENDIENTE, ReservationStatus.EN_ESTADIA);

	private final ReservationRepository reservationRepository;

	private final ReservationEventProducer reservationEventProducer;

	private final RabbitMqPublisherService rabbitMqPublisherService;

	@Transactional
	public Reservation create(CreateReservationRequest request) {
		if (request.customerUserId() == null || request.customerUserId().isBlank()) {
			throw new IllegalArgumentException("El customerUserId es obligatorio");
		}
		if (request.unitId() == null) {
			throw new IllegalArgumentException("El unitId es obligatorio");
		}
		if (request.amount() == null || request.amount().signum() <= 0) {
			throw new IllegalArgumentException("El monto debe ser un valor positivo");
		}
		if (request.checkInDate() == null || request.checkOutDate() == null) {
			throw new IllegalArgumentException("Las fechas de entrada y salida son obligatorias");
		}
		if (!request.checkOutDate().isAfter(request.checkInDate())) {
			throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada");
		}
		if (request.guests() != null && request.guests() <= 0) {
			throw new IllegalArgumentException("El número de huéspedes debe ser un valor positivo");
		}
		Reservation reservation = Reservation.builder()
				.customerUserId(request.customerUserId())
				.unitId(request.unitId())
				.checkInDate(request.checkInDate())
				.checkOutDate(request.checkOutDate())
				.amount(request.amount())
				.status(ReservationStatus.CREADA)
				.unitName(request.unitName())
				.guests(request.guests())
				.customerName(request.customerName())
				.guestEmail(request.guestEmail())
				.build();
		Reservation saved = reservationRepository.save(reservation);
		reservationEventProducer.publish(ReservationEvent.created(saved, saved.getCustomerUserId()));
		return saved;
	}

	@Transactional(readOnly = true)
	public Reservation findById(Long id) {
		return reservationRepository.findById(id)
				.orElseThrow(() -> new ReservationNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public List<Reservation> findAll() {
		return reservationRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
	}

	@Transactional
	public Reservation updateStatus(Long id, ReservationStatus target, String actor) {
		if (target == null) {
			throw new IllegalArgumentException("El estado objetivo es obligatorio");
		}
		Reservation reservation = findById(id);
		ReservationStatus previousStatus = reservation.getStatus();
		ReservationStatus nextStatus = previousStatus.transit(target);
		if (target == ReservationStatus.CONFIRMADA && previousStatus != ReservationStatus.CONFIRMADA) {
			reservation.setPreparationStatus(PreparationStatus.PENDING);
		}
		reservation.setStatus(nextStatus);
		Reservation saved = reservationRepository.save(reservation);
		String effectiveActor = (actor == null || actor.isBlank()) ? SYSTEM_ACTOR : actor;
		reservationEventProducer.publish(ReservationEvent.statusChanged(saved, previousStatus, effectiveActor));
		if (target == ReservationStatus.CONFIRMADA) {
			publishConfirmationNotifications(saved);
		}
		return saved;
	}

	@Transactional
	public Reservation updatePreparation(Long id, PreparationStatus target, String actor) {
		if (target != PreparationStatus.IN_PROGRESS && target != PreparationStatus.READY) {
			throw new IllegalArgumentException("Estado de preparacion no permitido: " + target);
		}
		Reservation reservation = findById(id);
		if (!PREPARATION_ALLOWED_RESERVATION_STATUSES.contains(reservation.getStatus())) {
			throw new InvalidPreparationTransitionException(reservation.getStatus(), target);
		}
		PreparationStatus previous = reservation.getPreparationStatus() == null
				? PreparationStatus.NONE
				: reservation.getPreparationStatus();
		if (previous == target) {
			return reservation;
		}
		PreparationStatus next = previous.transit(target);
		reservation.setPreparationStatus(next);
		Reservation saved = reservationRepository.save(reservation);
		if (next == PreparationStatus.READY) {
			publishCabinReady(saved, actor);
		}
		return saved;
	}

	private void publishCabinReady(Reservation reservation, String actor) {
		String traceId = (actor == null || actor.isBlank()) ? UUID.randomUUID().toString() : actor;
		ReservationNotification notification = new ReservationNotification(
				String.valueOf(reservation.getId()),
				reservation.getCustomerName(),
				reservation.getGuestEmail(),
				reservation.getUnitId() != null ? String.valueOf(reservation.getUnitId()) : null,
				traceId,
				UUID.randomUUID().toString());
		rabbitMqPublisherService.publishCabinReady(notification);
	}

	private void publishConfirmationNotifications(Reservation reservation) {
		ReservationNotification notification = new ReservationNotification(
				String.valueOf(reservation.getId()),
				reservation.getCustomerName(),
				reservation.getGuestEmail(),
				reservation.getUnitId() != null ? String.valueOf(reservation.getUnitId()) : null,
				UUID.randomUUID().toString(),
				UUID.randomUUID().toString());
		rabbitMqPublisherService.publishEmailConfirmation(notification);
		rabbitMqPublisherService.publishHousekeepingTicket(notification);
		rabbitMqPublisherService.publishVoucherGeneration(notification);
	}
}
