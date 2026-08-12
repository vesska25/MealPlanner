import { useState } from 'react'
import { usePantryQuery } from './usePantryQuery'
import { useDiscardPantryItem } from './useDiscardPantryItem'
import { extractErrorMessage } from '../api/client'
import type { DiscardReason } from '../api/types'

const DISCARD_REASONS: { value: DiscardReason; label: string }[] = [
  { value: 'EXPIRED_EARLY', label: 'Expired early' },
  { value: 'DIDNT_COOK_IN_TIME', label: "Didn't cook in time" },
  { value: 'BOUGHT_TOO_MUCH', label: 'Bought too much' },
]

export function PantryPage() {
  const { data: items, isLoading, isError, error } = usePantryQuery()
  const discardItem = useDiscardPantryItem()
  const [openItemId, setOpenItemId] = useState<number | null>(null)
  const [reason, setReason] = useState<DiscardReason>('EXPIRED_EARLY')
  const [feedback, setFeedback] = useState<string | null>(null)

  function handleDiscard(itemId: number) {
    setFeedback(null)
    discardItem.mutate(
      { itemId, reason },
      {
        onSuccess: () => setOpenItemId(null),
        onError: (err) => setFeedback(extractErrorMessage(err)),
      },
    )
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
                  {openItemId === item.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <select
                        value={reason}
                        onChange={(event) => setReason(event.target.value as DiscardReason)}
                        className="rounded border border-gray-300 px-2 py-1 text-sm"
                      >
                        {DISCARD_REASONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      <button
                        type="button"
                        onClick={() => handleDiscard(item.id)}
                        disabled={discardItem.isPending}
                        className="rounded bg-red-600 px-2 py-1 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                      >
                        {discardItem.isPending ? 'Discarding…' : 'Confirm'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setOpenItemId(null)}
                        className="text-sm text-gray-500 hover:underline"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setOpenItemId(item.id)}
                      className="text-sm text-red-600 hover:underline"
                    >
                      Mark spoiled
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {feedback && <p className="text-sm text-red-600">{feedback}</p>}
    </div>
  )
}
