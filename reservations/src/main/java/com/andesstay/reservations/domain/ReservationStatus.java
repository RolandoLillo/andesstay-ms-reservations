package com.andesstay.reservations.domain;

import com.andesstay.reservations.exception.InvalidStatusTransitionException;

public enum ReservationStatus {

	CREADA,
	CONFIRMADA,
	CHECKIN_PENDIENTE,
	EN_ESTADIA,
	CHECKOUT,
	CANCELADA;

	public ReservationStatus transit(ReservationStatus target) {
		if (this == target) {
			return this;
		}
		boolean allowed = switch (this) {
			case CREADA -> target == CONFIRMADA || target == CANCELADA;
			case CONFIRMADA -> target == CHECKIN_PENDIENTE || target == CANCELADA;
			case CHECKIN_PENDIENTE -> target == EN_ESTADIA || target == CANCELADA;
			case EN_ESTADIA -> target == CHECKOUT;
			case CHECKOUT, CANCELADA -> false;
		};
		if (!allowed) {
			throw new InvalidStatusTransitionException(this, target);
		}
		return target;
	}
}