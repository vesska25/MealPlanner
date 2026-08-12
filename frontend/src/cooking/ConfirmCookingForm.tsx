import { useState, type FormEvent } from 'react'
import { ApiError, extractErrorMessage } from '../api/client'
import type { CookingInfeasibleResponse } from '../api/types'
import { useConfirmCooking } from './useConfirmCooking'

function mintIdempotencyKey(): string {
  return crypto.randomUUID()
}

export function ConfirmCookingForm({ recipeId, basePortions }: { recipeId: number; basePortions: number }) {
  const [portions, setPortions] = useState(basePortions)
  // Minted once per logically-new confirm attempt, then reused across retries of that same
  // attempt (e.g. after a dropped connection) — per AI-15a, the same key must be resent on
  // retry, never a fresh one, or the server can't tell a retry from a second real confirmation.
  const [idempotencyKey, setIdempotencyKey] = useState<string>(mintIdempotencyKey)
  const [missingIngredients, setMissingIngredients] = useState<CookingInfeasibleResponse | null>(null)
  const confirmCooking = useConfirmCooking()

  function handlePortionsChange(value: number) {
    setPortions(value)
    setIdempotencyKey(mintIdempotencyKey())
    setMissingIngredients(null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMissingIngredients(null)
    confirmCooking.mutate(
      { recipeId, actualPortions: portions, idempotencyKey },
      {
        onError: (error) => {
          if (error instanceof ApiError && error.status === 400) {
            try {
              const parsed = JSON.parse(error.body) as CookingInfeasibleResponse
              if (Array.isArray(parsed.missingIngredients)) {
                setMissingIngredients(parsed)
                return
              }
            } catch {
              // not the structured cooking-infeasible shape, fall through to the generic message
            }
          }
        },
      },
    )
  }

  if (confirmCooking.isSuccess) {
    return <p className="text-sm text-emerald-700">Cooking confirmed — added to your cooked dishes.</p>
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-2 rounded border border-gray-200 bg-gray-50 p-3">
      <label className="flex items-center gap-2 text-sm">
        <span className="font-medium text-gray-700">Actual portions cooked</span>
        <input
          type="number"
          min={0.1}
          step={0.1}
          value={portions}
          onChange={(event) => handlePortionsChange(Number(event.target.value))}
          className="w-24 rounded border border-gray-300 px-2 py-1"
        />
      </label>
      {missingIngredients && (
        <div className="rounded border border-red-200 bg-red-50 p-2 text-sm text-red-800">
          <p className="font-medium">Not enough stock to cook this:</p>
          <ul className="list-inside list-disc">
            {missingIngredients.missingIngredients.map((m, index) => (
              <li key={index}>
                Product {m.productId}: need {m.needed} {m.unit.toLowerCase()}, have {m.available} {m.unit.toLowerCase()}
              </li>
            ))}
          </ul>
        </div>
      )}
      {confirmCooking.isError && !missingIngredients && (
        <p className="text-sm text-red-600">{extractErrorMessage(confirmCooking.error)}</p>
      )}
      <button
        type="submit"
        disabled={confirmCooking.isPending}
        className="self-start rounded bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
      >
        {confirmCooking.isPending ? 'Confirming…' : 'Confirm cooking'}
      </button>
    </form>
  )
}
