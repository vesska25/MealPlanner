const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  body: string

  constructor(status: number, body: string) {
    super(body || `Request failed with status ${status}`)
    this.status = status
    this.body = body
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('authToken')
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (response.status === 401) {
    localStorage.removeItem('authToken')
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Unauthorized')
  }

  if (!response.ok) {
    const body = await response.text()
    throw new ApiError(response.status, body)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/**
 * `AuthExceptionHandler`'s domain errors return a plain-text body, but validation failures
 * (`@Valid` on the request DTO) fall through to Spring's default `ProblemDetail` JSON body
 * instead — this normalizes both into a single displayable string.
 */
export function extractErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return 'Something went wrong. Please try again.'
  }
  if (!error.body) {
    return error.message
  }
  try {
    const parsed = JSON.parse(error.body) as { detail?: string; title?: string }
    return parsed.detail ?? parsed.title ?? error.body
  } catch {
    return error.body
  }
}
