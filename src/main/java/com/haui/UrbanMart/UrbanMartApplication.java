package com.haui.UrbanMart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UrbanMartApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrbanMartApplication.class, args);
	}

}
