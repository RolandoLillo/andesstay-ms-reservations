package com.andesstay.reservations.controller;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.dto.ReservationResponse;
import com.andesstay.reservations.dto.ReservationStatusRequest;
import com.andesstay.reservations.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	@PostMapping
	public ResponseEntity<ReservationResponse> create(@RequestBody CreateReservationRequest request) {
		Reservation reservation = reservationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ReservationResponse.from(reservationService.findById(id)));
	}

	@GetMapping
	public ResponseEntity<List<ReservationResponse>> getAll() {
		List<ReservationResponse> response = reservationService.findAll().stream()
				.map(ReservationResponse::from)
				.toList();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
			@RequestBody ReservationStatusRequest request) {
		Reservation reservation = reservationService.updateStatus(id, request.status());
		return ResponseEntity.ok(ReservationResponse.from(reservation));
	}
}