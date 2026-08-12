import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { ConfirmCookingForm } from './ConfirmCookingForm'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

beforeEach(() => {
  apiFetchMock.mockReset()
})

function renderForm() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfirmCookingForm recipeId={1} basePortions={2} />
    </QueryClientProvider>,
  )
}

describe('ConfirmCookingForm', () => {
  it('reuses the same idempotency key when retrying after a failed attempt', async () => {
    apiFetchMock.mockRejectedValueOnce(new ApiError(500, 'network blip')).mockResolvedValueOnce({ id: 1 })
    renderForm()

    await userEvent.click(screen.getByRole('button', { name: /confirm cooking/i }))
    await userEvent.click(await screen.findByRole('button', { name: /confirm cooking/i }))

    expect(apiFetchMock).toHaveBeenCalledTimes(2)
    const firstBody = JSON.parse(apiFetchMock.mock.calls[0][1].body)
    const secondBody = JSON.parse(apiFetchMock.mock.calls[1][1].body)
    expect(secondBody.idempotencyKey).toBe(firstBody.idempotencyKey)
  })

  it('mints a fresh idempotency key when the portions value changes (a logically new attempt)', async () => {
    apiFetchMock.mockRejectedValueOnce(new ApiError(500, 'network blip')).mockResolvedValueOnce({ id: 1 })
    renderForm()

    await userEvent.click(screen.getByRole('button', { name: /confirm cooking/i }))
    expect(apiFetchMock).toHaveBeenCalledTimes(1)
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '3' } })
    expect(screen.getByRole('spinbutton')).toHaveValue(3)
    await userEvent.click(await screen.findByRole('button', { name: /confirm cooking/i }))

    expect(apiFetchMock).toHaveBeenCalledTimes(2)
    const firstBody = JSON.parse(apiFetchMock.mock.calls[0][1].body)
    const secondBody = JSON.parse(apiFetchMock.mock.calls[1][1].body)
    expect(secondBody.idempotencyKey).not.toBe(firstBody.idempotencyKey)
  })

  it('renders the structured missing-ingredients list on a 400 CookingInfeasibleResponse', async () => {
    const body = JSON.stringify({
      message: 'Not enough stock',
      missingIngredients: [{ productId: 5, unit: 'GRAM', needed: 200, available: 100 }],
    })
    apiFetchMock.mockRejectedValueOnce(new ApiError(400, body))
    renderForm()

    await userEvent.click(screen.getByRole('button', { name: /confirm cooking/i }))

    expect(await screen.findByText(/need 200 gram, have 100 gram/i)).toBeInTheDocument()
  })
})
