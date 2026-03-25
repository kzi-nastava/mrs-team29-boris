package com.example.backendspringboot;

import com.example.backendspringboot.model.*;
import com.example.backendspringboot.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class BackendSpringBootApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendSpringBootApplication.class, args);
	}

	@Bean
	@Profile("!test")
	@Transactional
	public CommandLineRunner initData(
			AdministratorRepository administratorRepository,
			DriverRepository driverRepository,
			PassengerRepository passengerRepository,
			VehicleRepository vehicleRepository,
			RideRepository rideRepository,
			LocationRepository locationRepository,
			RouteRepository routeRepository,
			PasswordEncoder passwordEncoder,
			VehiclePriceRepository vehiclePriceRepository) {
		return args -> {

			// ------------------ 1. VEHICLE PRICES ------------------
			VehiclePrice vp = new VehiclePrice();
			if (vehiclePriceRepository.count() == 0) {
				vp.setPerKm(120.0);
				vp.setStandard(200.0);
				vp.setVan(400.0);
				vp.setLuxury(950.0);
				vp = vehiclePriceRepository.save(vp);
			} else {
				vp = vehiclePriceRepository.findAll().get(0);
			}

			// ------------------ 2. ADMIN ------------------
			if (administratorRepository.count() == 0) {
				Administrator admin = new Administrator();
				admin.setName("Admin");
				admin.setSurname("Adminovic");
				admin.setEmail("admin@demo.com");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setAddress("Admin Street 1");
				admin.setPhone("123456789");
				admin.setGender(Gender.MALE);
				administratorRepository.save(admin);
			}

			// ------------------ 3. DRIVERS & VEHICLES ------------------
			double[][] coords = {
					{45.2641, 19.8302},
					{45.2426, 19.8820},
					{45.2399, 19.8253},
					{45.2517, 19.8369},
					{45.2454, 19.8247}
			};
			String[] addresses = {
					"Bulevar Kralja Petra I",
					"Futoški put",
					"Bulevar Evrope",
					"Bulevar oslobođenja",
					"Cara Dušana"
			};
			String[] names = {"Marko", "Ivan", "Petar", "Miloš", "Nikola"};
			String[] emails = {
					"driver@demo.com",
					"driver2@demo.com",
					"driver3@demo.com",
					"driver4@demo.com",
					"driver5@demo.com"
			};
			String[] regs = {"NS-001-AA", "NS-002-BB", "NS-003-CC", "NS-004-DD", "NS-005-EE"};

			for (int i = 0; i < regs.length; i++) {
				if (vehicleRepository.existsByRegistration(regs[i])
						|| driverRepository.existsByEmail(emails[i])) {
					continue;
				}

					Location loc = new Location();
					loc.setLatitude(coords[i][0]);
					loc.setLongitude(coords[i][1]);
					loc.setAddress(addresses[i]);

					Vehicle v = new Vehicle();
					v.setModel("Demo Car");
					v.setRegistration(regs[i]);
					v.setSeats(4);
					v.setType(VehicleType.STANDARD);
					v.setBusy(i >= 3);
					v.setIsBabyFriendly(true);
					v.setIsPetFriendly(true);
					v.setLocation(loc);
					v = vehicleRepository.save(v);

					Driver d = new Driver();
					d.setName(names[i]);
					d.setSurname("Driverovic");
					d.setEmail(emails[i]);
					d.setPassword(passwordEncoder.encode("driver123"));
					d.setPhone("060123456" + i);
					d.setGender(Gender.MALE);
					d.setStatus(DriverStatus.ACTIVE);
					d.setVehicle(v);
					d.setAddress("Driver Street " + i);
					driverRepository.save(d);
			}

			// ------------------ 4. ROUTES (6 RUTA) ------------------
			// Moramo ih imati spremne pre putnika da bismo izbegli Lazy grešku
			List<Route> routes = new ArrayList<>();
			if (routeRepository.count() == 0) {
				Object[][] routeData = {
						{"Bulevar Oslobođenja 45", 45.2485, 19.8331, "Cara Dušana 12", 45.2413, 19.8256, 3.2},
						{"Futoška 10", 45.2512, 19.8365, "Zmaj Jovina 5", 45.2570, 19.8440, 2.5},
						{"Narodnog fronta 22", 45.2390, 19.8310, "Dunavska 1", 45.2460, 19.8510, 4.8},
						{"Bulevar Evrope 1", 45.2450, 19.8150, "Petrovaradinska tvrđava", 45.2530, 19.8630, 5.5},
						{"Gogoljeva 15", 45.2475, 19.8350, "Spens", 45.2440, 19.8430, 1.8},
						{"Rumenačka 3", 45.2630, 19.8220, "Železnička stanica", 45.2650, 19.8290, 2.1}
				};

				for (Object[] rd : routeData) {
					Location start = new Location();
					start.setAddress((String)rd[0]);
					start.setLatitude((double)rd[1]);
					start.setLongitude((double)rd[2]);
					start = locationRepository.save(start);

					Location end = new Location();
					end.setAddress((String)rd[3]);
					end.setLatitude((double)rd[4]);
					end.setLongitude((double)rd[5]);
					end = locationRepository.save(end);

					Route r = new Route();
					r.setOrigin(start);
					r.setDestination(end);
					r.setDistance((double)rd[6]);
					r.setDuration(12);
					routes.add(routeRepository.save(r));
				}
			} else {
				routes = routeRepository.findAll();
			}

			// ------------------ 5. PASSENGERS (SA OMILJENIM RUTAMA) ------------------
			Passenger jovan;
			Passenger pavle;

			if (passengerRepository.count() == 0) {
				jovan = new Passenger();
				jovan.setName("Jovan");
				jovan.setSurname("Jovanovic");
				jovan.setEmail("passenger@demo.com");
				jovan.setPassword(passwordEncoder.encode("passenger123"));
				jovan.setActivated(true);
				jovan.setPhone("555666777");
				jovan.setGender(Gender.MALE);
				jovan.setAddress("Passenger Street 1");
				// Odmah postavljamo omiljene rute dok je sesija otvorena i pre prvog save-a
				jovan.setFavoriteRoutes(new ArrayList<>(List.of(routes.get(0), routes.get(2))));
				jovan = passengerRepository.save(jovan);

				pavle = new Passenger();
				pavle.setName("Pavle");
				pavle.setSurname("Maksimovic");
				pavle.setEmail("makspavle@gmail.com");
				pavle.setPassword(passwordEncoder.encode("pavle123"));
				pavle.setActivated(true);
				pavle.setPhone("069123456");
				pavle.setGender(Gender.MALE);
				pavle.setAddress("Passenger Street 2");
				pavle.setFavoriteRoutes(new ArrayList<>(List.of(routes.get(1), routes.get(4), routes.get(5))));
				pavle = passengerRepository.save(pavle);
			} else {
				jovan = passengerRepository.findByEmail("passenger@demo.com").get();
				pavle = passengerRepository.findByEmail("makspavle@gmail.com").get();
			}

			// ------------------ 6. RIDES (9 VOŽNJI) ------------------
			if (rideRepository.count() == 0) {
				List<Driver> drivers = driverRepository.findAll();
				Object[][] rideMatrix = {
						{0, jovan, 0}, {0, pavle, 1}, {0, jovan, 2},
						{1, pavle, 3}, {1, jovan, 4}, {1, pavle, 5},
						{2, jovan, 0}, {2, pavle, 2}, {2, jovan, 4}
				};

				for (int i = 0; i < rideMatrix.length; i++) {
					Driver d = drivers.get((int)rideMatrix[i][0]);
					Passenger p = (Passenger) rideMatrix[i][1];
					Route r = routes.get((int)rideMatrix[i][2]);

					double finalPrice = Math.round(vp.getStandard() + (r.getDistance() * vp.getPerKm()));

					Ride ride = new Ride();
					ride.setDriver(d);
					ride.setRoute(r);
					ride.setPassengers(new ArrayList<>(List.of(p)));
					ride.setRideCreator(p);
					ride.setPrice(finalPrice);
					ride.setStartTime(LocalDateTime.now().minusDays(i+1).plusHours(i));
					ride.setEndTime(LocalDateTime.now().minusDays(i+1).plusHours(i).plusMinutes(20));
					ride.setStatus(RideStatus.FINISHED);
					if (i == 1) ride.setPanicPressed(true);

					rideRepository.save(ride);
				}
				System.out.println("Inicijalizacija uspešno završena (9 vožnji, 6 ruta, 2 putnika).");
			}
		};
	}
}
