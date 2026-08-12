import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import type { ShoppingListResponse } from '../api/types'
import { ShoppingListView } from './ShoppingListView'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

beforeEach(() => {
  apiFetchMock.mockReset()
})

function renderView() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ShoppingListView />
    </QueryClientProvider>,
  )
}

describe('ShoppingListView', () => {
  it('shows an empty-state message on a 404 (no list generated yet)', async () => {
    apiFetchMock.mockRejectedValue(new ApiError(404, ''))
    renderView()

    expect(await screen.findByText(/no shopping list yet/i)).toBeInTheDocument()
  })

  it('groups items by block', async () => {
    const list: ShoppingListResponse = {
      id: 1,
      createdAt: '2026-08-12T00:00:00Z',
      items: [
        { id: 1, productName: 'milk', quantity: 500, unit: 'MILLILITER', block: 'DEFINITELY_NEED', status: 'PENDING' },
        { id: 2, productName: 'salt', quantity: 1, unit: 'PIECE', block: 'CHECK_MAYBE_OUT', status: 'PENDING' },
      ],
    }
    apiFetchMock.mockResolvedValue(list)
    renderView()

    expect(await screen.findByText('Definitely need')).toBeInTheDocument()
    expect(screen.getByText('Check — might be out')).toBeInTheDocument()
    expect(screen.getByText(/milk/)).toBeInTheDocument()
    expect(screen.getByText(/salt/)).toBeInTheDocument()
  })
})
