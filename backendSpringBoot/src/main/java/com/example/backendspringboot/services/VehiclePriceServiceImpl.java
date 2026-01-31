package com.example.backendspringboot.services;
import com.example.backendspringboot.dto.VehiclePriceDTO;
import com.example.backendspringboot.model.VehiclePrice;
import com.example.backendspringboot.repositories.VehiclePriceRepository;
import com.example.backendspringboot.services.interfaces.VehiclePriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehiclePriceServiceImpl implements VehiclePriceService {
    private final VehiclePriceRepository vehiclePriceRepository;

    public VehiclePriceDTO getPrices() {
        // Always ID 1 only
        VehiclePrice prices = vehiclePriceRepository.findById(1L)
                .orElseGet(() -> vehiclePriceRepository.save(new VehiclePrice(1L, 150.0, 500.0, 250.0, 120.0)));

        return new VehiclePriceDTO(
                prices.getStandard(),
                prices.getLuxury(),
                prices.getVan(),
                prices.getPerKm()
        );
    }

    public VehiclePriceDTO updatePrices(VehiclePriceDTO dto) {
        VehiclePrice prices = new VehiclePrice(
                1L,
                dto.getStandardBasePrice(),
                dto.getLuxuryBasePrice(),
                dto.getVanBasePrice(),
                dto.getPricePerKm()
        );
        vehiclePriceRepository.save(prices);
        return dto;
    }

}
