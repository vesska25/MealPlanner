import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { apiFetch, extractErrorMessage } from '../api/client'
import type { AccountExportResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { TelegramSection } from '../telegram/TelegramSection'

const DELETE_CONFIRMATION_PHRASE = 'DELETE'

export function AccountPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [confirmationText, setConfirmationText] = useState('')

  const exportData = useMutation({
    mutationFn: () => apiFetch<AccountExportResponse>('/api/account/export'),
    onSuccess: (data) => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `meal-planner-export-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      URL.revokeObjectURL(url)
    },
  })

  const deleteAccount = useMutation({
    mutationFn: () => apiFetch<void>('/api/account', { method: 'DELETE' }),
    onSuccess: () => {
      logout()
      navigate('/login')
    },
  })

  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-xl font-semibold text-gray-900">Account</h1>

      <section className="flex flex-col gap-2">
        <h2 className="font-semibold text-gray-900">Export my data</h2>
        <p className="text-sm text-gray-600">
          Download everything stored about you (pantry, recipes, cooked dishes, agent runs) as JSON.
        </p>
        <button
          type="button"
          onClick={() => exportData.mutate()}
          disabled={exportData.isPending}
          className="self-start rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          {exportData.isPending ? 'Preparing export…' : 'Export my data'}
        </button>
        {exportData.isError && <p className="text-sm text-red-600">{extractErrorMessage(exportData.error)}</p>}
      </section>

      <TelegramSection />

      <section className="flex flex-col gap-2 rounded border border-red-200 bg-red-50 p-4">
        <h2 className="font-semibold text-red-900">Delete my account</h2>
        <p className="text-sm text-red-800">
          This permanently deletes your account and everything linked to it. This cannot be undone.
        </p>
        <label className="flex flex-col gap-1 text-sm text-red-800">
          Type <span className="font-mono font-semibold">{DELETE_CONFIRMATION_PHRASE}</span> to confirm
          <input
            type="text"
            value={confirmationText}
            onChange={(event) => setConfirmationText(event.target.value)}
            className="w-40 rounded border border-red-300 px-3 py-2"
          />
        </label>
        {deleteAccount.isError && <p className="text-sm text-red-700">{extractErrorMessage(deleteAccount.error)}</p>}
        <button
          type="button"
          onClick={() => deleteAccount.mutate()}
          disabled={confirmationText !== DELETE_CONFIRMATION_PHRASE || deleteAccount.isPending}
          className="self-start rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
        >
          {deleteAccount.isPending ? 'Deleting…' : 'Delete my account'}
        </button>
      </section>
    </div>
  )
}
