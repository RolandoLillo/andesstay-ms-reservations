package com.andesstay.reservations.exception;

import com.andesstay.reservations.domain.ReservationStatus;

public class InvalidStatusTransitionException extends RuntimeException {

	private final ReservationStatus from;
	private final ReservationStatus to;

	public InvalidStatusTransitionException(ReservationStatus from, ReservationStatus to) {
		super("Transicion invalida de estado: " + from + " -> " + to);
		this.from = from;
		this.to = to;
	}

	public ReservationStatus getFrom() {
		return from;
	}

	public ReservationStatus getTo() {
		return to;
	}
}