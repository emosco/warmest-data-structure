package com.example.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutRequest {

    @NotNull(message = "value is required")
    private Integer value;
}