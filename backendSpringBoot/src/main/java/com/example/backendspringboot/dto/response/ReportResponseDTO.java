package com.example.demo.dto.response;

import com.example.demo.dto.DailyStatsDTO;
import com.example.demo.dto.SummaryStatsDTO;
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
}
