package com.andesstay.reservations.exception;

import com.andesstay.reservations.domain.PreparationStatus;
import com.andesstay.reservations.domain.ReservationStatus;

public class InvalidPreparationTransitionException extends RuntimeException {

	public InvalidPreparationTransitionException(ReservationStatus reservationStatus, PreparationStatus requested) {
		super("Preparacion no permitida para reserva en estado " + reservationStatus + " con destino " + requested);
	}

	public InvalidPreparationTransitionException(PreparationStatus from, PreparationStatus to) {
		super("Transicion invalida de estado de preparacion: " + from + " -> " + to);
	}
}
