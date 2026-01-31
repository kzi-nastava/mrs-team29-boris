package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.request.PanicRequestDTO;
import com.example.backendspringboot.dto.response.PanicResponseDTO;

import java.util.List;

public interface PanicService {
    PanicResponseDTO triggerPanic(PanicRequestDTO request);
    List<PanicResponseDTO> getAllPanics();
    List<PanicResponseDTO> getUnresolvedPanics();
    void resolvePanic(Long panicId);
}