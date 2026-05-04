package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.backend.entity.Expense;

public record ExpenseResponse(Long id, BigDecimal amount, String category, LocalDate date, String description) {

	public static ExpenseResponse from(Expense expense) {
		return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getCategory(), expense.getExpenseDate(),
				expense.getDescription());
	}
}
