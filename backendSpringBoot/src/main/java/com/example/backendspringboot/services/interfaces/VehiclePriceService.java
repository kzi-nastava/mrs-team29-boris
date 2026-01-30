package com.example.demo.services.interfaces;

import com.example.demo.dto.VehiclePriceDTO;

public interface VehiclePriceService {
    public VehiclePriceDTO getPrices();
    public VehiclePriceDTO updatePrices(VehiclePriceDTO dto);

}
