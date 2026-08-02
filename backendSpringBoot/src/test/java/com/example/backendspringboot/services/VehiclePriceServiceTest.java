package com.example.backendspringboot.services;

import com.example.backendspringboot.dto.VehiclePriceDTO;
import com.example.backendspringboot.model.VehiclePrice;
import com.example.backendspringboot.repositories.VehiclePriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehiclePriceServiceTest {
    private final VehiclePriceRepository repository = mock(VehiclePriceRepository.class);
    private final VehiclePriceServiceImpl service = new VehiclePriceServiceImpl(repository);

    @Test
    void updatesSingletonPriceList() {
        VehiclePrice existing = new VehiclePrice(1L, 150, 500, 250, 120);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        VehiclePriceDTO request = new VehiclePriceDTO(200, 600, 350, 130);

        VehiclePriceDTO result = service.updatePrices(request);

        assertEquals(200, result.getStandardBasePrice());
        assertEquals(200, existing.getStandard());
        assertEquals(600, existing.getLuxury());
        assertEquals(350, existing.getVan());
        assertEquals(130, existing.getPerKm());
        verify(repository).save(existing);
    }

    @Test
    void rejectsZeroNegativeNonFiniteAndExcessivePrices() {
        assertThrows(ResponseStatusException.class,
                () -> service.updatePrices(new VehiclePriceDTO(0, 500, 250, 120)));
        assertThrows(ResponseStatusException.class,
                () -> service.updatePrices(new VehiclePriceDTO(150, -1, 250, 120)));
        assertThrows(ResponseStatusException.class,
                () -> service.updatePrices(new VehiclePriceDTO(150, 500, Double.NaN, 120)));
        assertThrows(ResponseStatusException.class,
                () -> service.updatePrices(new VehiclePriceDTO(150, 500, 250, 1_000_001)));
        verify(repository, never()).save(any());
    }
}
