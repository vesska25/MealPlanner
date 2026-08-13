import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { OnboardingStateResponse } from '../api/types'
import { OnboardingPage } from './OnboardingPage'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

function renderOnboardingPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/onboarding']}>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/chat/meal-planning" element={<p>Meal planning page</p>} />
          <Route path="/chat/shopping-list" element={<p>Shopping list page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('OnboardingPage', () => {
  it('hydrates the chat transcript from previously saved turns (FR-10b)', async () => {
    const state: OnboardingStateResponse = {
      profileFinalized: false,
      recentTurns: [
        { role: 'agent', text: 'How many people do you cook for?' },
        { role: 'user', text: 'Two' },
      ],
    }
    apiFetchMock.mockResolvedValueOnce(state)
    renderOnboardingPage()

    expect(await screen.findByText('How many people do you cook for?')).toBeInTheDocument()
    expect(screen.getByText('Two')).toBeInTheDocument()
  })

  it('shows the scenario picker instead of chat once the profile is finalized, and navigates on choice', async () => {
    const state: OnboardingStateResponse = { profileFinalized: true, recentTurns: [] }
    apiFetchMock.mockResolvedValueOnce(state)
    renderOnboardingPage()

    const mealPlanningButton = await screen.findByRole('button', { name: /cook from what i have at home/i })
    expect(screen.queryByPlaceholderText('Type a message…')).not.toBeInTheDocument()

    await userEvent.click(mealPlanningButton)

    await waitFor(() => expect(screen.getByText('Meal planning page')).toBeInTheDocument())
  })
})
