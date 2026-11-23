package com.andesstay.reservations.domain;

import com.andesstay.reservations.exception.InvalidPreparationTransitionException;

public enum PreparationStatus {

	NONE,
	PENDING,
	IN_PROGRESS,
	READY;

	public PreparationStatus transit(PreparationStatus target) {
		if (this == target) {
			return this;
		}
		boolean allowed = switch (this) {
			case PENDING -> target == IN_PROGRESS || target == READY;
			case IN_PROGRESS -> target == READY;
			case NONE, READY -> false;
		};
		if (!allowed) {
			throw new InvalidPreparationTransitionException(this, target);
		}
		return target;
	}
}
