import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PantryItemResponse } from '../api/types'
import { PantryPage } from './PantryPage'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

beforeEach(() => {
  apiFetchMock.mockReset()
})

const items: PantryItemResponse[] = [
  { id: 7, productName: 'milk', quantity: 500, unit: 'MILLILITER', purchasedAt: '2026-08-01', expiresAt: '2026-08-19', estimated: false },
]

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <PantryPage />
    </QueryClientProvider>,
  )
}

describe('PantryPage', () => {
  it('discarding an item posts the selected reason to the discard endpoint', async () => {
    apiFetchMock.mockResolvedValueOnce(items).mockResolvedValueOnce(undefined)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: /mark spoiled/i }))
    await userEvent.selectOptions(screen.getByRole('combobox'), 'BOUGHT_TOO_MUCH')
    await userEvent.click(screen.getByRole('button', { name: /^confirm$/i }))

    expect(apiFetchMock).toHaveBeenNthCalledWith(2, '/api/pantry/7/discard', {
      method: 'POST',
      body: JSON.stringify({ reason: 'BOUGHT_TOO_MUCH' }),
    })
  })
})
