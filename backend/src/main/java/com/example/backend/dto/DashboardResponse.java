package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(BigDecimal totalThisMonth, List<ExpenseResponse> recentExpenses) {
}
