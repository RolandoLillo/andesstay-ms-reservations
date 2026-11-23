package com.andesstay.reservations.dto;

import com.andesstay.reservations.domain.PreparationStatus;
import jakarta.validation.constraints.NotNull;

public record PreparationStatusRequest(@NotNull PreparationStatus status) {
}
