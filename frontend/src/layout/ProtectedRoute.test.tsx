import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../auth/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

function renderWithAuth(isAuthenticated: boolean) {
  return render(
    <AuthContext.Provider value={{ token: isAuthenticated ? 'token' : null, isAuthenticated, login: () => {}, logout: () => {} }}>
      <MemoryRouter initialEntries={['/pantry']}>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/pantry" element={<p>Pantry page</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects to /login when not authenticated', () => {
    renderWithAuth(false)
    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('renders the nested route when authenticated', () => {
    renderWithAuth(true)
    expect(screen.getByText('Pantry page')).toBeInTheDocument()
  })
})
