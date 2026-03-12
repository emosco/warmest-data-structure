package com.example.dto;

import jakarta.validation.constraints.NotNull;

public record PutRequest(
        @NotNull(message = "value is required") Integer value
) {}