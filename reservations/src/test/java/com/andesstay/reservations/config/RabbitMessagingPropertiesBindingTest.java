package com.andesstay.reservations.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RabbitMessagingPropertiesBindingTest {

	@Autowired
	private RabbitMessagingProperties properties;

	@Test
	void enlazaPropertiesDesdeApplicationProperties() {
		assertNotNull(properties);
		assertNotNull(properties.getExchange());
		assertNotNull(properties.getExchange().getCmd());
		assertEquals("cmd.direct", properties.getExchange().getCmd());
		assertNotNull(properties.getRouting());
		assertNotNull(properties.getRouting().getEmail());
		assertEquals("email.send", properties.getRouting().getEmail());
		assertNotNull(properties.getRouting().getHousekeeping());
		assertEquals("housekeeping.ticket", properties.getRouting().getHousekeeping());
		assertNotNull(properties.getRouting().getVoucher());
		assertEquals("voucher.gen", properties.getRouting().getVoucher());
	}
}
