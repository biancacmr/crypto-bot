package com.bianca.AutomaticCryptoTrader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutomaticCryptoTraderApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomaticCryptoTraderApplication.class, args);
	}

}
