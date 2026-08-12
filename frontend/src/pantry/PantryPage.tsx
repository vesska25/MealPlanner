import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { usePantryQuery } from './usePantryQuery'
import { useSendMessage } from '../chat/useSendMessage'
import { AgentStatusBadge } from '../chat/AgentStatusBadge'
import { extractErrorMessage } from '../api/client'
import type { AgentRunStatus } from '../api/types'

export function PantryPage() {
  const { data: items, isLoading, isError, error } = usePantryQuery()
  const markSpoiled = useSendMessage('PANTRY_ASSISTANT')
  const queryClient = useQueryClient()
  const [activeItemId, setActiveItemId] = useState<number | null>(null)
  const [reply, setReply] = useState<{ text: string; status?: AgentRunStatus } | null>(null)

  function handleMarkSpoiled(productName: string, itemId: number) {
    setActiveItemId(itemId)
    setReply(null)
    markSpoiled.mutate(`Please mark my ${productName} as spoiled and discard it.`, {
      onSuccess: (response) => {
        setReply({ text: response.message, status: response.status })
        queryClient.invalidateQueries({ queryKey: ['pantry'] })
      },
      onError: (err) => setReply({ text: extractErrorMessage(err) }),
    })
  }

  if (isLoading) return <p className="text-gray-500">Loading pantry…</p>
  if (isError) return <p className="text-red-600">{extractErrorMessage(error)}</p>

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-gray-900">Pantry</h1>
      {items && items.length === 0 && <p className="text-gray-500">Your pantry is empty.</p>}
      {items && items.length > 0 && (
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-gray-200 text-gray-500">
              <th className="py-2">Product</th>
              <th className="py-2">Quantity</th>
              <th className="py-2">Expires</th>
              <th className="py-2"></th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id} className="border-b border-gray-100">
                <td className="py-2">
                  {item.productName}
                  {item.estimated && (
                    <span className="ml-2 rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">estimated</span>
                  )}
                </td>
                <td className="py-2">
                  {item.quantity} {item.unit.toLowerCase()}
                </td>
                <td className="py-2">{item.expiresAt}</td>
                <td className="py-2 text-right">
                  <button
                    type="button"
                    onClick={() => handleMarkSpoiled(item.productName, item.id)}
                    disabled={markSpoiled.isPending && activeItemId === item.id}
                    className="text-sm text-red-600 hover:underline disabled:opacity-50"
                  >
                    {markSpoiled.isPending && activeItemId === item.id ? 'Marking…' : 'Mark spoiled'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {reply && (
        <div className="rounded border border-gray-200 bg-gray-50 p-3 text-sm">
          <p>{reply.text}</p>
          {reply.status && (
            <div className="mt-1">
              <AgentStatusBadge status={reply.status} />
            </div>
          )}
        </div>
      )}
    </div>
  )
}
