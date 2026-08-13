import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { AppShell } from './layout/AppShell'
import { ProtectedRoute } from './layout/ProtectedRoute'
import { PantryPage } from './pantry/PantryPage'
import { PantryAssistantPage } from './chat/PantryAssistantPage'
import { MealPlanningPage } from './chat/MealPlanningPage'
import { ShoppingListPage } from './chat/ShoppingListPage'
import { SuggestionPage } from './suggestions/SuggestionPage'
import { CookedDishesPage } from './cooking/CookedDishesPage'
import { AccountPage } from './account/AccountPage'
import { OnboardingPage } from './onboarding/OnboardingPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/pantry" replace />} />
          <Route path="/pantry" element={<PantryPage />} />
          <Route path="/chat/pantry-assistant" element={<PantryAssistantPage />} />
          <Route path="/chat/meal-planning" element={<MealPlanningPage />} />
          <Route path="/chat/shopping-list" element={<ShoppingListPage />} />
          <Route path="/suggestions" element={<SuggestionPage />} />
          <Route path="/cooked-dishes" element={<CookedDishesPage />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/onboarding" element={<OnboardingPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
