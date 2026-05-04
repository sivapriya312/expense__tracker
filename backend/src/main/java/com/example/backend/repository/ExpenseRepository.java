package com.example.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.backend.entity.Expense;
import com.example.backend.entity.User;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	List<Expense> findByUserOrderByExpenseDateDescIdDesc(User user);

	List<Expense> findTop5ByUserOrderByExpenseDateDescIdDesc(User user);

	List<Expense> findByUserAndCategoryIgnoreCaseOrderByExpenseDateDescIdDesc(User user, String category);

	List<Expense> findByUserAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(User user, LocalDate startDate,
			LocalDate endDate);

	List<Expense> findByUserAndCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(User user,
			String category, LocalDate startDate, LocalDate endDate);

	@Query("select coalesce(sum(e.amount), 0) from Expense e where e.user = ?1 and e.expenseDate between ?2 and ?3")
	BigDecimal totalForDateRange(User user, LocalDate startDate, LocalDate endDate);

	@Query("select e.category, coalesce(sum(e.amount), 0) from Expense e where e.user = ?1 and e.expenseDate between ?2 and ?3 group by e.category order by e.category")
	List<Object[]> categoryTotalsForDateRange(User user, LocalDate startDate, LocalDate endDate);
}
