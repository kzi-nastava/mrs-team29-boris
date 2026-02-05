package com.example.backendspringboot.controller;

import com.example.backendspringboot.dto.request.PanicRequestDTO;
import com.example.backendspringboot.dto.response.PanicResponseDTO;
import com.example.backendspringboot.services.interfaces.PanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/panic")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PanicController {

    private final PanicService panicService;

    @PostMapping
    public ResponseEntity<PanicResponseDTO> triggerPanic(@RequestBody PanicRequestDTO request) {
        System.out.println("==========================================");
        System.out.println("PANIC REQUEST RECEIVED!");
        System.out.println("Ride ID: " + request.getRideId());
        System.out.println("User ID: " + request.getUserId());
        System.out.println("Is Guest: " + request.isGuest());
        System.out.println("==========================================");

        PanicResponseDTO response = panicService.triggerPanic(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<PanicResponseDTO>> getAllPanicNotifications() {
        List<PanicResponseDTO> panics = panicService.getAllPanics();
        return ResponseEntity.ok(panics);
    }

    @GetMapping("/unresolved")
    public ResponseEntity<List<PanicResponseDTO>> getUnresolvedPanics() {
        List<PanicResponseDTO> panics = panicService.getUnresolvedPanics();
        return ResponseEntity.ok(panics);
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Void> resolvePanic(@PathVariable Long id) {
        panicService.resolvePanic(id);
        return ResponseEntity.ok().build();
    }
}