import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE = '/api'
const categories = ['Food', 'Travel', 'Bills', 'Shopping', 'Health', 'Education', 'Other']

const emptyExpense = {
  amount: '',
  category: 'Food',
  date: new Date().toISOString().slice(0, 10),
  description: '',
}

function App() {
  const [authMode, setAuthMode] = useState('login')
  const [authForm, setAuthForm] = useState({
    name: '',
    email: '',
    password: '',
  })
  const [session, setSession] = useState(() => {
    const savedSession = localStorage.getItem('expenseTrackerSession')
    return savedSession ? JSON.parse(savedSession) : null
  })
  const [dashboard, setDashboard] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [report, setReport] = useState(null)
  const [expenseForm, setExpenseForm] = useState(emptyExpense)
  const [editingId, setEditingId] = useState(null)
  const [filters, setFilters] = useState({
    category: '',
    startDate: '',
    endDate: '',
  })
  const [activeView, setActiveView] = useState('dashboard')
  const [message, setMessage] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const currency = useMemo(
    () =>
      new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
      }),
    [],
  )

  const request = useCallback(async (path, options = {}) => {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
        ...options.headers,
      },
    })

    if (!response.ok) {
      let errorMessage = 'Something went wrong'
      try {
        const error = await response.json()
        errorMessage = error.message || errorMessage
      } catch {
        errorMessage = response.statusText || errorMessage
      }
      throw new Error(errorMessage)
    }

    if (response.status === 204) {
      return null
    }

    return response.json()
  }, [session])

  const loadAppData = useCallback(async () => {
    setIsLoading(true)
    setMessage('')
    try {
      const query = new URLSearchParams()
      if (filters.category) query.set('category', filters.category)
      if (filters.startDate && filters.endDate) {
        query.set('startDate', filters.startDate)
        query.set('endDate', filters.endDate)
      }

      const [dashboardData, expensesData, reportData] = await Promise.all([
        request('/dashboard'),
        request(`/expenses${query.toString() ? `?${query}` : ''}`),
        request('/reports/monthly'),
      ])

      setDashboard(dashboardData)
      setExpenses(expensesData)
      setReport(reportData)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsLoading(false)
    }
  }, [filters, request])

  useEffect(() => {
    if (session) {
      localStorage.setItem('expenseTrackerSession', JSON.stringify(session))
      Promise.resolve().then(loadAppData)
    } else {
      localStorage.removeItem('expenseTrackerSession')
    }
  }, [session, loadAppData])

  async function handleAuth(event) {
    event.preventDefault()
    setIsLoading(true)
    setMessage('')
    try {
      const path = authMode === 'login' ? '/auth/login' : '/auth/register'
      const payload =
        authMode === 'login'
          ? { email: authForm.email, password: authForm.password }
          : authForm
      const data = await request(path, {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: {},
      })
      setSession(data)
      setAuthForm({ name: '', email: '', password: '' })
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsLoading(false)
    }
  }

  async function handleSaveExpense(event) {
    event.preventDefault()
    setIsLoading(true)
    setMessage('')
    try {
      const payload = {
        amount: Number(expenseForm.amount),
        category: expenseForm.category,
        date: expenseForm.date,
        description: expenseForm.description,
      }
      const path = editingId ? `/expenses/${editingId}` : '/expenses'
      await request(path, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
      setExpenseForm(emptyExpense)
      setEditingId(null)
      await loadAppData()
      setMessage(editingId ? 'Expense updated.' : 'Expense added.')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsLoading(false)
    }
  }

  async function handleDelete(expenseId) {
    const confirmed = window.confirm('Delete this expense?')
    if (!confirmed) return

    setIsLoading(true)
    setMessage('')
    try {
      await request(`/expenses/${expenseId}`, { method: 'DELETE' })
      await loadAppData()
      setMessage('Expense deleted.')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setIsLoading(false)
    }
  }

  function startEdit(expense) {
    setEditingId(expense.id)
    setExpenseForm({
      amount: expense.amount,
      category: expense.category,
      date: expense.date,
      description: expense.description || '',
    })
    setActiveView('add')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function logout() {
    setSession(null)
    setDashboard(null)
    setExpenses([])
    setReport(null)
    setMessage('')
  }

  if (!session) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div>
            <p className="eyebrow">Expense Tracker</p>
            <h1>Track spending without the spreadsheet fog.</h1>
            <p className="lede">
              Register or sign in to manage expenses, monthly totals, and category reports.
            </p>
          </div>

          <form className="auth-form" onSubmit={handleAuth}>
            <div className="segmented" aria-label="Authentication mode">
              <button
                type="button"
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => setAuthMode('login')}
              >
                Login
              </button>
              <button
                type="button"
                className={authMode === 'register' ? 'active' : ''}
                onClick={() => setAuthMode('register')}
              >
                Register
              </button>
            </div>

            {authMode === 'register' && (
              <label>
                Name
                <input
                  value={authForm.name}
                  onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                  required
                />
              </label>
            )}
            <label>
              Email
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                required
              />
            </label>
            <button className="primary-button" type="submit" disabled={isLoading}>
              {isLoading ? 'Please wait' : authMode === 'login' ? 'Login' : 'Create account'}
            </button>
            {message && <p className="notice">{message}</p>}
          </form>
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Expense Tracker</p>
          <h1>{session.name}</h1>
          <p>{session.email}</p>
        </div>
        <nav>
          {[
            ['dashboard', 'Dashboard'],
            ['add', editingId ? 'Edit Expense' : 'Add Expense'],
            ['expenses', 'Expenses'],
            ['reports', 'Reports'],
          ].map(([view, label]) => (
            <button
              key={view}
              className={activeView === view ? 'active' : ''}
              type="button"
              onClick={() => setActiveView(view)}
            >
              {label}
            </button>
          ))}
        </nav>
        <button className="ghost-button" type="button" onClick={logout}>
          Logout
        </button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Monthly Overview</p>
            <h2>{currency.format(Number(dashboard?.totalThisMonth || 0))}</h2>
          </div>
          <button className="primary-button compact" type="button" onClick={() => setActiveView('add')}>
            Add Expense
          </button>
        </header>

        {message && <p className="notice">{message}</p>}

        {activeView === 'dashboard' && (
          <section className="dashboard-grid">
            <article className="metric-card">
              <span>Total This Month</span>
              <strong>{currency.format(Number(dashboard?.totalThisMonth || 0))}</strong>
            </article>
            <article className="metric-card">
              <span>Transactions</span>
              <strong>{expenses.length}</strong>
            </article>
            <article className="panel wide">
              <div className="panel-heading">
                <h3>Recent Transactions</h3>
                <button type="button" onClick={() => setActiveView('expenses')}>
                  View All
                </button>
              </div>
              <ExpenseList
                currency={currency}
                expenses={dashboard?.recentExpenses || []}
                onEdit={startEdit}
                onDelete={handleDelete}
              />
            </article>
          </section>
        )}

        {activeView === 'add' && (
          <section className="panel">
            <div className="panel-heading">
              <h3>{editingId ? 'Edit Expense' : 'Add Expense'}</h3>
              {editingId && (
                <button
                  type="button"
                  onClick={() => {
                    setEditingId(null)
                    setExpenseForm(emptyExpense)
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
            <form className="expense-form" onSubmit={handleSaveExpense}>
              <label>
                Amount
                <input
                  min="1"
                  step="0.01"
                  type="number"
                  value={expenseForm.amount}
                  onChange={(event) =>
                    setExpenseForm({ ...expenseForm, amount: event.target.value })
                  }
                  required
                />
              </label>
              <label>
                Category
                <select
                  value={expenseForm.category}
                  onChange={(event) =>
                    setExpenseForm({ ...expenseForm, category: event.target.value })
                  }
                >
                  {categories.map((category) => (
                    <option key={category}>{category}</option>
                  ))}
                </select>
              </label>
              <label>
                Date
                <input
                  type="date"
                  value={expenseForm.date}
                  onChange={(event) => setExpenseForm({ ...expenseForm, date: event.target.value })}
                  required
                />
              </label>
              <label className="span-2">
                Description
                <textarea
                  value={expenseForm.description}
                  onChange={(event) =>
                    setExpenseForm({ ...expenseForm, description: event.target.value })
                  }
                  rows="4"
                />
              </label>
              <button className="primary-button" type="submit" disabled={isLoading}>
                {editingId ? 'Update Expense' : 'Save Expense'}
              </button>
            </form>
          </section>
        )}

        {activeView === 'expenses' && (
          <section className="panel">
            <div className="panel-heading">
              <h3>All Expenses</h3>
              <button type="button" onClick={loadAppData} disabled={isLoading}>
                Refresh
              </button>
            </div>
            <div className="filters">
              <select
                value={filters.category}
                onChange={(event) => setFilters({ ...filters, category: event.target.value })}
              >
                <option value="">All categories</option>
                {categories.map((category) => (
                  <option key={category}>{category}</option>
                ))}
              </select>
              <input
                type="date"
                value={filters.startDate}
                onChange={(event) => setFilters({ ...filters, startDate: event.target.value })}
              />
              <input
                type="date"
                value={filters.endDate}
                onChange={(event) => setFilters({ ...filters, endDate: event.target.value })}
              />
              <button type="button" onClick={loadAppData}>
                Apply
              </button>
              <button
                type="button"
                onClick={() => setFilters({ category: '', startDate: '', endDate: '' })}
              >
                Clear
              </button>
            </div>
            <ExpenseList
              currency={currency}
              expenses={expenses}
              onEdit={startEdit}
              onDelete={handleDelete}
            />
          </section>
        )}

        {activeView === 'reports' && (
          <section className="panel">
            <div className="panel-heading">
              <h3>Monthly Report</h3>
              <span>{currency.format(Number(report?.totalThisMonth || 0))}</span>
            </div>
            <div className="report-list">
              {(report?.categoryTotals || []).length === 0 ? (
                <p className="empty">No report data yet.</p>
              ) : (
                report.categoryTotals.map((item) => (
                  <div className="report-row" key={item.category}>
                    <span>{item.category}</span>
                    <strong>{currency.format(Number(item.total))}</strong>
                  </div>
                ))
              )}
            </div>
          </section>
        )}
      </section>
    </main>
  )
}

function ExpenseList({ currency, expenses, onEdit, onDelete }) {
  if (!expenses.length) {
    return <p className="empty">No expenses found.</p>
  }

  return (
    <div className="expense-list">
      {expenses.map((expense) => (
        <article className="expense-row" key={expense.id}>
          <div>
            <strong>{expense.category}</strong>
            <span>{expense.description || 'No description'}</span>
          </div>
          <div>
            <strong>{currency.format(Number(expense.amount))}</strong>
            <span>{expense.date}</span>
          </div>
          <div className="row-actions">
            <button type="button" onClick={() => onEdit(expense)}>
              Edit
            </button>
            <button type="button" onClick={() => onDelete(expense.id)}>
              Delete
            </button>
          </div>
        </article>
      ))}
    </div>
  )
}

export default App
