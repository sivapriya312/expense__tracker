package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponse(BigDecimal totalThisMonth, List<CategoryTotalResponse> categoryTotals) {
}
