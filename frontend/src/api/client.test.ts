import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError, extractErrorMessage } from './client'

describe('apiFetch', () => {
  beforeEach(() => {
    localStorage.clear()
    Object.defineProperty(window, 'location', {
      value: { ...window.location, href: '', pathname: '/pantry' },
      writable: true,
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('attaches the bearer token when one is present', async () => {
    localStorage.setItem('authToken', 'test-token')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/pantry')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer test-token')
  })

  it('clears the token and redirects to /login on a 401', async () => {
    localStorage.setItem('authToken', 'stale-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })))

    await expect(apiFetch('/api/pantry')).rejects.toThrow(ApiError)

    expect(localStorage.getItem('authToken')).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('throws an ApiError carrying the status and body on other non-2xx responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('invalid invite code', { status: 400 })))

    const error = await apiFetch('/api/auth/register').catch((e) => e as ApiError)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.body).toBe('invalid invite code')
  })

  it('returns undefined for a 204 response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

    const result = await apiFetch('/api/cooked-dishes/1')

    expect(result).toBeUndefined()
  })
})

describe('extractErrorMessage', () => {
  it('returns the plain-text body for domain errors', () => {
    expect(extractErrorMessage(new ApiError(400, 'Invalid invite code'))).toBe('Invalid invite code')
  })

  it('extracts the detail field from a Spring ProblemDetail JSON body', () => {
    const body = JSON.stringify({ title: 'Bad Request', detail: 'password must be at least 8 characters' })
    expect(extractErrorMessage(new ApiError(400, body))).toBe('password must be at least 8 characters')
  })

  it('falls back to a generic message for non-ApiError values', () => {
    expect(extractErrorMessage(new Error('network down'))).toBe('Something went wrong. Please try again.')
  })
})
