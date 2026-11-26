package com.redcell.soat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class SoatApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoatApplication.class, args);
	}

}