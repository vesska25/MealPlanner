import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { GenerateLinkCodeResponse, TelegramLinkStatusResponse } from '../api/types'
import { TelegramSection } from './TelegramSection'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return { ...actual, apiFetch: apiFetchMock }
})

beforeEach(() => {
  apiFetchMock.mockReset()
})

function renderSection() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <TelegramSection />
    </QueryClientProvider>,
  )
}

describe('TelegramSection', () => {
  it('shows a Connect button when not linked, and the deep link after generating a code', async () => {
    const notLinked: TelegramLinkStatusResponse = { linked: false, telegramUserId: null, linkedAt: null }
    const generated: GenerateLinkCodeResponse = {
      code: 'abc123',
      deepLink: 'https://t.me/meal_planner_bot?start=abc123',
      expiresAt: '2026-08-12T10:10:00Z',
    }
    apiFetchMock.mockResolvedValueOnce(notLinked).mockResolvedValueOnce(generated)
    renderSection()

    await userEvent.click(await screen.findByRole('button', { name: /connect telegram/i }))

    expect(await screen.findByRole('link', { name: /open in telegram/i })).toHaveAttribute('href', generated.deepLink)
    expect(screen.getByText('abc123')).toBeInTheDocument()
  })

  it('shows Connected + Disconnect when linked, and unlinking posts to the correct endpoint', async () => {
    const linked: TelegramLinkStatusResponse = { linked: true, telegramUserId: 123, linkedAt: '2026-08-12T09:00:00Z' }
    apiFetchMock.mockResolvedValueOnce(linked).mockResolvedValueOnce(undefined)
    renderSection()

    expect(await screen.findByText('Connected')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /disconnect/i }))

    expect(apiFetchMock).toHaveBeenNthCalledWith(2, '/api/telegram', { method: 'DELETE' })
  })
})
