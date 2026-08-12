import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ActiveSuggestionResponse } from '../api/types'
import { SuggestionPage } from './SuggestionPage'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

beforeEach(() => {
  apiFetchMock.mockReset()
})

const suggestion: ActiveSuggestionResponse = {
  suggestionId: 1,
  recipeId: 42,
  recipeName: 'Simple Omelette',
  cookTimeMinutes: 10,
  basePortions: 1,
  requiredEquipment: ['pan'],
  ingredients: [{ productName: 'eggs', quantity: 2, unit: 'PIECE' }],
  score: 0.68,
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <SuggestionPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SuggestionPage', () => {
  it('accepting posts to the accept endpoint for the suggestion\'s recipeId', async () => {
    apiFetchMock.mockResolvedValueOnce(suggestion).mockResolvedValueOnce(undefined)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: /^accept$/i }))

    expect(apiFetchMock).toHaveBeenNthCalledWith(2, '/api/recipes/suggestions/42/accept', { method: 'POST' })
  })

  it('rejecting posts the selected reason to the reject endpoint', async () => {
    apiFetchMock.mockResolvedValueOnce(suggestion).mockResolvedValueOnce(undefined)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: /^reject$/i }))
    await userEvent.selectOptions(screen.getByRole('combobox'), 'TAKES_TOO_LONG')
    await userEvent.click(screen.getByRole('button', { name: /submit rejection/i }))

    expect(apiFetchMock).toHaveBeenNthCalledWith(2, '/api/recipes/suggestions/42/reject', {
      method: 'POST',
      body: JSON.stringify({ reason: 'TAKES_TOO_LONG' }),
    })
  })
})
