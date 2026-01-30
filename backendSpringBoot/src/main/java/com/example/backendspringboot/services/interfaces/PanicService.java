package com.example.demo.services.interfaces;

import com.example.demo.dto.request.PanicRequestDTO;
import com.example.demo.dto.response.PanicResponseDTO;

import java.util.List;

public interface PanicService {
    PanicResponseDTO triggerPanic(PanicRequestDTO request);
    List<PanicResponseDTO> getAllPanics();
    List<PanicResponseDTO> getUnresolvedPanics();
    void resolvePanic(Long panicId);
}