package com.andesstay.reservations.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqPublisherService {

	private final RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.exchange.cmd:cmd.direct}")
	private String cmdExchange;

	@Value("${rabbitmq.routing.email:email.send}")
	private String emailRoutingKey;

	@Value("${rabbitmq.routing.housekeeping:housekeeping.ticket}")
	private String housekeepingRoutingKey;

	@Value("${rabbitmq.routing.voucher:voucher.gen}")
	private String voucherRoutingKey;

	public void publishEmailNotification(ReservationNotificationMessage message) {
		publish(emailRoutingKey, message);
	}

	public void publishHousekeepingTicket(ReservationNotificationMessage message) {
		publish(housekeepingRoutingKey, message);
	}

	public void publishVoucherGeneration(ReservationNotificationMessage message) {
		publish(voucherRoutingKey, message);
	}

	private void publish(String routingKey, ReservationNotificationMessage message) {
		try {
			rabbitTemplate.convertAndSend(cmdExchange, routingKey, message);
			log.info("Mensaje publicado en exchange={} routingKey={} para reservationId={}",
					cmdExchange, routingKey, message.getReservationId());
		} catch (AmqpException | RuntimeException ex) {
			log.error("No se pudo publicar mensaje en exchange={} routingKey={} para reservationId={}",
					cmdExchange, routingKey, message.getReservationId(), ex);
		}
	}
}
