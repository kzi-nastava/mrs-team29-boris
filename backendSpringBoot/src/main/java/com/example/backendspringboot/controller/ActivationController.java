package com.example.backendspringboot.controller;

import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.repositories.PassengerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/activation")
public class ActivationController {

    private final PassengerRepository passengerRepository;

    public ActivationController(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @GetMapping("/activate-account")
    public ResponseEntity<String> activateAccount(@RequestParam String token) {
        Optional<Passenger> passenger = passengerRepository.findByActivationToken(token);

        if (passenger.isPresent()) {
            Passenger p = passenger.get();
            p.setActivated(true);
            p.setActivationToken(null);
            p.setActivationTokenExpiry(null);
            passengerRepository.save(p);

            return ResponseEntity.ok("<h2 style='text-align:center;margin-top:50px;'>Profile activated successfully!</h2>");
        } else {
            return ResponseEntity.badRequest().body("<h2 style='text-align:center;margin-top:50px;color:red;'>Invalid or expired activation token</h2>");
        }
    }
}
