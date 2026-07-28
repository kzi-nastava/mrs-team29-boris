package com.example.backendspringboot.dto.response;

import com.example.backendspringboot.dto.DailyStatsDTO;
import com.example.backendspringboot.dto.SummaryStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ReportResponseDTO {
    private List<DailyStatsDTO> dailyStats;
    private SummaryStatsDTO summary;
    private boolean earnings;
}
