package com.example.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.dto.CategoryTotalResponse;
import com.example.backend.dto.DashboardResponse;
import com.example.backend.dto.ExpenseRequest;
import com.example.backend.dto.ExpenseResponse;
import com.example.backend.dto.ReportResponse;
import com.example.backend.entity.Expense;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.ExpenseRepository;

@Service
public class ExpenseService {

	private final ExpenseRepository expenseRepository;

	public ExpenseService(ExpenseRepository expenseRepository) {
		this.expenseRepository = expenseRepository;
	}

	public DashboardResponse dashboard(User user) {
		YearMonth month = YearMonth.now();
		BigDecimal total = expenseRepository.totalForDateRange(user, month.atDay(1), month.atEndOfMonth());
		List<ExpenseResponse> recent = expenseRepository.findTop5ByUserOrderByExpenseDateDescIdDesc(user).stream()
				.map(ExpenseResponse::from)
				.toList();
		return new DashboardResponse(total, recent);
	}

	public ExpenseResponse create(User user, ExpenseRequest request) {
		validateExpense(request);

		Expense expense = new Expense();
		expense.setUser(user);
		applyRequest(expense, request);
		return ExpenseResponse.from(expenseRepository.save(expense));
	}

	public List<ExpenseResponse> findAll(User user, String category, LocalDate startDate, LocalDate endDate) {
		List<Expense> expenses;
		if (category != null && !category.isBlank() && startDate != null && endDate != null) {
			expenses = expenseRepository.findByUserAndCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
					user, category.trim(), startDate, endDate);
		} else if (category != null && !category.isBlank()) {
			expenses = expenseRepository.findByUserAndCategoryIgnoreCaseOrderByExpenseDateDescIdDesc(user, category.trim());
		} else if (startDate != null && endDate != null) {
			expenses = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(user, startDate, endDate);
		} else {
			expenses = expenseRepository.findByUserOrderByExpenseDateDescIdDesc(user);
		}

		return expenses.stream().map(ExpenseResponse::from).toList();
	}

	public ExpenseResponse update(User user, Long expenseId, ExpenseRequest request) {
		validateExpense(request);
		Expense expense = getOwnedExpense(user, expenseId);
		applyRequest(expense, request);
		return ExpenseResponse.from(expenseRepository.save(expense));
	}

	public void delete(User user, Long expenseId) {
		Expense expense = getOwnedExpense(user, expenseId);
		expenseRepository.delete(expense);
	}

	public ReportResponse report(User user) {
		YearMonth month = YearMonth.now();
		LocalDate start = month.atDay(1);
		LocalDate end = month.atEndOfMonth();
		BigDecimal total = expenseRepository.totalForDateRange(user, start, end);
		List<CategoryTotalResponse> categoryTotals = expenseRepository.categoryTotalsForDateRange(user, start, end).stream()
				.map(row -> new CategoryTotalResponse((String) row[0], (BigDecimal) row[1]))
				.toList();
		return new ReportResponse(total, categoryTotals);
	}

	private Expense getOwnedExpense(User user, Long expenseId) {
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		if (!expense.getUser().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Expense not found");
		}
		return expense;
	}

	private void validateExpense(ExpenseRequest request) {
		if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
		}
		if (request.category() == null || request.category().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Category is required");
		}
		if (request.date() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Date is required");
		}
	}

	private void applyRequest(Expense expense, ExpenseRequest request) {
		expense.setAmount(request.amount());
		expense.setCategory(request.category().trim());
		expense.setExpenseDate(request.date());
		expense.setDescription(request.description());
	}
}
