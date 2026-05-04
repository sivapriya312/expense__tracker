package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(BigDecimal amount, String category, LocalDate date, String description) {
}
