package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InconsistencyReportRequestDTO {

    @NotBlank(message = "Reason for inconsistency must be provided.")
    @Size(max = 500, message = "The report reason must not exceed 500 characters.")
    private String reason;
}