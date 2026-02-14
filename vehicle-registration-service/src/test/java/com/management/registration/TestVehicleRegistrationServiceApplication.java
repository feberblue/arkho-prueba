package com.management.registration;

import org.springframework.boot.SpringApplication;

public class TestVehicleRegistrationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(VehicleRegistrationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
