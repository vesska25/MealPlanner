import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const NAV_LINKS = [
  { to: '/pantry', label: 'Pantry' },
  { to: '/chat/pantry-assistant', label: 'Pantry Assistant' },
  { to: '/chat/meal-planning', label: 'Meal Planning' },
  { to: '/chat/shopping-list', label: 'Shopping List' },
  { to: '/suggestions', label: 'Suggestions' },
  { to: '/cooked-dishes', label: 'Cooked Dishes' },
  { to: '/account', label: 'Account' },
]

export function AppShell() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-200 bg-white px-4 py-3">
        <div className="flex flex-wrap gap-1">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `rounded px-3 py-1.5 text-sm font-medium ${
                  isActive ? 'bg-emerald-100 text-emerald-800' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="rounded px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-gray-100"
        >
          Log out
        </button>
      </nav>
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
