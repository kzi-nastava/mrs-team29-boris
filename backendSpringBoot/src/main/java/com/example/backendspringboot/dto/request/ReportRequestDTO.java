package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Long userId; // null for when admin wants a report for all users of the app
    private String userType;
}
