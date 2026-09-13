package com.andesstay.reservations.exception;

public class ReservationNotFoundException extends RuntimeException {

	public ReservationNotFoundException(Long id) {
		super("Reserva no encontrada con id: " + id);
	}
}