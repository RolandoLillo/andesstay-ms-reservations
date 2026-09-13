package com.andesstay.reservations.service;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;
import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.exception.ReservationNotFoundException;
import com.andesstay.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;

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
		Reservation reservation = Reservation.builder()
				.customerUserId(request.customerUserId())
				.unitId(request.unitId())
				.checkInDate(request.checkInDate())
				.checkOutDate(request.checkOutDate())
				.amount(request.amount())
				.status(ReservationStatus.CREADA)
				.build();
		return reservationRepository.save(reservation);
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
	public Reservation updateStatus(Long id, ReservationStatus target) {
		if (target == null) {
			throw new IllegalArgumentException("El estado objetivo es obligatorio");
		}
		Reservation reservation = findById(id);
		reservation.setStatus(reservation.getStatus().transit(target));
		return reservationRepository.save(reservation);
	}
}