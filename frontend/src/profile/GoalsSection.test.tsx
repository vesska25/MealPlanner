import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import type { GoalsResponse } from '../api/types'
import { GoalsSection } from './GoalsSection'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

function renderSection() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <GoalsSection />
    </QueryClientProvider>,
  )
}

describe('GoalsSection', () => {
  it('renders nothing when goals are disabled (FR-74: the backend 404s, not zeroed numbers)', async () => {
    apiFetchMock.mockRejectedValueOnce(new ApiError(404, ''))
    const { container } = renderSection()

    await vi.waitFor(() => expect(apiFetchMock).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })

  it('shows the computed targets when goals are enabled', async () => {
    const goals: GoalsResponse = {
      dailyKcal: 2136,
      proteinGrams: 128,
      fatGrams: 71.2,
      carbsGrams: 245.8,
      flooredToMinimum: false,
    }
    apiFetchMock.mockResolvedValueOnce(goals)
    renderSection()

    expect(await screen.findByText('2136 kcal')).toBeInTheDocument()
    expect(screen.getByText('128 g')).toBeInTheDocument()
  })

  it('shows a moderated-target note when the floor was applied', async () => {
    const goals: GoalsResponse = {
      dailyKcal: 1200,
      proteinGrams: 72,
      fatGrams: 40,
      carbsGrams: 138,
      flooredToMinimum: true,
    }
    apiFetchMock.mockResolvedValueOnce(goals)
    renderSection()

    expect(await screen.findByText(/moderated target/i)).toBeInTheDocument()
  })
})
