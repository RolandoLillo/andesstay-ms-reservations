package com.andesstay.reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMessagingProperties {

	private Exchange exchange = new Exchange();

	private Routing routing = new Routing();

	public Exchange getExchange() {
		return exchange;
	}

	public void setExchange(Exchange exchange) {
		this.exchange = exchange;
	}

	public Routing getRouting() {
		return routing;
	}

	public void setRouting(Routing routing) {
		this.routing = routing;
	}

	public static class Exchange {
		private String cmd;

		public String getCmd() {
			return cmd;
		}

		public void setCmd(String cmd) {
			this.cmd = cmd;
		}
	}

	public static class Routing {
		private String email;
		private String housekeeping;
		private String voucher;

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getHousekeeping() {
			return housekeeping;
		}

		public void setHousekeeping(String housekeeping) {
			this.housekeeping = housekeeping;
		}

		public String getVoucher() {
			return voucher;
		}

		public void setVoucher(String voucher) {
			this.voucher = voucher;
		}
	}
}
