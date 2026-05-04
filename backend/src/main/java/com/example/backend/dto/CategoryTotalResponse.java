package com.example.backend.dto;

import java.math.BigDecimal;

public record CategoryTotalResponse(String category, BigDecimal total) {
}
