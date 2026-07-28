package com.example.backendspringboot.services;
import com.example.backendspringboot.dto.VehiclePriceDTO;
import com.example.backendspringboot.model.VehiclePrice;
import com.example.backendspringboot.repositories.VehiclePriceRepository;
import com.example.backendspringboot.services.interfaces.VehiclePriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        validate(dto);
        VehiclePrice prices = vehiclePriceRepository.findById(1L)
                .orElseGet(() -> new VehiclePrice(1L, 150.0, 500.0, 250.0, 120.0));
        prices.setStandard(dto.getStandardBasePrice());
        prices.setLuxury(dto.getLuxuryBasePrice());
        prices.setVan(dto.getVanBasePrice());
        prices.setPerKm(dto.getPricePerKm());
        vehiclePriceRepository.save(prices);
        return dto;
    }

    private static void validate(VehiclePriceDTO dto) {
        if (dto == null || !validPrice(dto.getStandardBasePrice())
                || !validPrice(dto.getLuxuryBasePrice())
                || !validPrice(dto.getVanBasePrice())
                || !validPrice(dto.getPricePerKm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sve cene moraju biti pozitivni konačni brojevi do 1.000.000 RSD.");
        }
    }

    private static boolean validPrice(double value) {
        return Double.isFinite(value) && value > 0 && value <= 1_000_000;
    }

}
