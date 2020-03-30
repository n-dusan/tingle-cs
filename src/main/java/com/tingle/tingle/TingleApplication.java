package com.tingle.tingle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TingleApplication {

	public static void main(String[] args) {
		SpringApplication.run(TingleApplication.class, args);
		System.out.println("Tingle is up and running");
	}

}
