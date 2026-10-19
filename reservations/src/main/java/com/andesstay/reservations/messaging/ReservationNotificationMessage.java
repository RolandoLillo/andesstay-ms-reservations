package com.andesstay.reservations.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationNotificationMessage {

	private Long reservationId;
	private String action;
	private String status;
	private Long unitId;
	private String unitName;
	private String customerUserId;
	private String customerName;
}
