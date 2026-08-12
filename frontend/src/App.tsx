import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { AppShell } from './layout/AppShell'
import { ProtectedRoute } from './layout/ProtectedRoute'

function ComingSoon({ label }: { label: string }) {
  return <p className="text-gray-500">{label} — coming soon.</p>
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/pantry" replace />} />
          <Route path="/pantry" element={<ComingSoon label="Pantry" />} />
          <Route path="/chat/pantry-assistant" element={<ComingSoon label="Pantry Assistant chat" />} />
          <Route path="/chat/meal-planning" element={<ComingSoon label="Meal Planning chat" />} />
          <Route path="/chat/shopping-list" element={<ComingSoon label="Shopping List chat" />} />
          <Route path="/suggestions" element={<ComingSoon label="Suggestions" />} />
          <Route path="/cooked-dishes" element={<ComingSoon label="Cooked Dishes" />} />
          <Route path="/account" element={<ComingSoon label="Account" />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
