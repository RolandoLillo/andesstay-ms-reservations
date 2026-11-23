package com.andesstay.reservations.domain;

import com.andesstay.reservations.exception.InvalidPreparationTransitionException;
import org.junit.jupiter.api.Test;

import static com.andesstay.reservations.domain.PreparationStatus.IN_PROGRESS;
import static com.andesstay.reservations.domain.PreparationStatus.NONE;
import static com.andesstay.reservations.domain.PreparationStatus.PENDING;
import static com.andesstay.reservations.domain.PreparationStatus.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreparationStatusTest {

	@Test
	void transicionesValidas() {
		assertEquals(IN_PROGRESS, PENDING.transit(IN_PROGRESS));
		assertEquals(READY, PENDING.transit(READY));
		assertEquals(READY, IN_PROGRESS.transit(READY));
		assertEquals(READY, READY.transit(READY));
	}

	@Test
	void transicionesInvalidas() {
		assertThrows(InvalidPreparationTransitionException.class, () -> NONE.transit(IN_PROGRESS));
		assertThrows(InvalidPreparationTransitionException.class, () -> READY.transit(IN_PROGRESS));
		assertThrows(InvalidPreparationTransitionException.class, () -> IN_PROGRESS.transit(PENDING));
	}
}
