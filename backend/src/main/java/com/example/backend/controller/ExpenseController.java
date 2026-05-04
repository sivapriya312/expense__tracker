package com.example.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.DashboardResponse;
import com.example.backend.dto.ExpenseRequest;
import com.example.backend.dto.ExpenseResponse;
import com.example.backend.dto.ReportResponse;
import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import com.example.backend.service.ExpenseService;

@RestController
@RequestMapping("/api")
public class ExpenseController {

	private final AuthService authService;
	private final ExpenseService expenseService;

	public ExpenseController(AuthService authService, ExpenseService expenseService) {
		this.authService = authService;
		this.expenseService = expenseService;
	}

	@GetMapping("/dashboard")
	public DashboardResponse dashboard(@RequestHeader("Authorization") String token) {
		User user = authService.requireUser(token);
		return expenseService.dashboard(user);
	}

	@PostMapping("/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	public ExpenseResponse create(@RequestHeader("Authorization") String token, @RequestBody ExpenseRequest request) {
		User user = authService.requireUser(token);
		return expenseService.create(user, request);
	}

	@GetMapping("/expenses")
	public List<ExpenseResponse> findAll(@RequestHeader("Authorization") String token, String category,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		User user = authService.requireUser(token);
		return expenseService.findAll(user, category, startDate, endDate);
	}

	@PutMapping("/expenses/{expenseId}")
	public ExpenseResponse update(@RequestHeader("Authorization") String token, @PathVariable Long expenseId,
			@RequestBody ExpenseRequest request) {
		User user = authService.requireUser(token);
		return expenseService.update(user, expenseId, request);
	}

	@DeleteMapping("/expenses/{expenseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@RequestHeader("Authorization") String token, @PathVariable Long expenseId) {
		User user = authService.requireUser(token);
		expenseService.delete(user, expenseId);
	}

	@GetMapping("/reports/monthly")
	public ReportResponse monthlyReport(@RequestHeader("Authorization") String token) {
		User user = authService.requireUser(token);
		return expenseService.report(user);
	}
}
