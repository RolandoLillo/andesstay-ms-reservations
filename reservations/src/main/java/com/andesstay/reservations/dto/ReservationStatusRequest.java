package com.andesstay.reservations.dto;

import com.andesstay.reservations.domain.ReservationStatus;

public record ReservationStatusRequest(ReservationStatus status) {
}