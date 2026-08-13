import { useState, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { apiFetch, extractErrorMessage } from '../api/client'
import type { AuthResponse, RegisterRequest } from '../api/types'
import { useAuth } from './useAuth'

export function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [inviteCode, setInviteCode] = useState('')

  const mutation = useMutation({
    mutationFn: (request: RegisterRequest) =>
      apiFetch<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    onSuccess: (data) => {
      login(data.token)
      navigate('/onboarding')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    mutation.mutate({ email, password, inviteCode })
  }

  return (
    <div className="mx-auto mt-24 max-w-sm">
      <h1 className="mb-6 text-2xl font-semibold text-gray-900">Register</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-gray-700">Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="rounded border border-gray-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-gray-700">Password</span>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="rounded border border-gray-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium text-gray-700">Invite code</span>
          <input
            type="text"
            required
            value={inviteCode}
            onChange={(event) => setInviteCode(event.target.value)}
            className="rounded border border-gray-300 px-3 py-2 focus:border-emerald-500 focus:outline-none"
          />
        </label>
        {mutation.isError && (
          <p className="text-sm text-red-600">{extractErrorMessage(mutation.error)}</p>
        )}
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Registering…' : 'Register'}
        </button>
      </form>
      <p className="mt-4 text-sm text-gray-600">
        Already have an account? <Link to="/login" className="text-emerald-700 underline">Log in</Link>
      </p>
    </div>
  )
}
