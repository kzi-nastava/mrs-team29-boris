package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.dto.VehiclePriceDTO;

public interface VehiclePriceService {
    public VehiclePriceDTO getPrices();
    public VehiclePriceDTO updatePrices(VehiclePriceDTO dto);

}
