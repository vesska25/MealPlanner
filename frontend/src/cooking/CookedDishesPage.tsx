import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, extractErrorMessage } from '../api/client'
import { useCookedDishesQuery } from './useCookedDishesQuery'
import type { CookedDishResponse } from '../api/types'

function DishCard({ dish }: { dish: CookedDishResponse }) {
  const queryClient = useQueryClient()
  const [portionsEaten, setPortionsEaten] = useState(1)
  const [consumeError, setConsumeError] = useState<string | null>(null)

  const consume = useMutation({
    mutationFn: (portions: number) =>
      apiFetch<void>(`/api/cooked-dishes/${dish.id}/consume`, {
        method: 'POST',
        body: JSON.stringify({ portionsEaten: portions }),
      }),
    onSuccess: () => {
      setConsumeError(null)
      queryClient.invalidateQueries({ queryKey: ['cooked-dishes'] })
    },
    onError: (error) => setConsumeError(extractErrorMessage(error)),
  })

  const discard = useMutation({
    mutationFn: () => apiFetch<void>(`/api/cooked-dishes/${dish.id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cooked-dishes'] }),
  })

  return (
    <div className="flex flex-col gap-2 rounded border border-gray-200 bg-white p-4">
      <div className="flex items-baseline justify-between">
        <h2 className="font-semibold text-gray-900">{dish.recipeName}</h2>
        <span className="text-xs text-gray-500">{dish.category}</span>
      </div>
      <p className="text-sm text-gray-600">
        {dish.portionsRemaining} / {dish.totalPortions} portions remaining
      </p>
      {dish.kcalPerPortion !== null && (
        <p className="text-xs text-gray-500">
          {dish.kcalPerPortion} kcal · {dish.proteinPerPortion}g protein · {dish.fatPerPortion}g fat ·{' '}
          {dish.carbsPerPortion}g carbs (per portion)
        </p>
      )}
      <p className="text-xs text-gray-500">
        Cooked {dish.cookedAt} · Expires {dish.expiresAt}
      </p>

      <div className="flex items-center gap-2">
        <input
          type="number"
          min={0.1}
          step={0.1}
          value={portionsEaten}
          onChange={(event) => setPortionsEaten(Number(event.target.value))}
          className="w-20 rounded border border-gray-300 px-2 py-1 text-sm"
        />
        <button
          type="button"
          onClick={() => consume.mutate(portionsEaten)}
          disabled={consume.isPending}
          className="rounded bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          Consume
        </button>
        <button
          type="button"
          onClick={() => {
            if (window.confirm(`Discard the rest of ${dish.recipeName}?`)) {
              discard.mutate()
            }
          }}
          disabled={discard.isPending}
          className="rounded border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
        >
          Discard
        </button>
      </div>
      {consumeError && <p className="text-sm text-red-600">{consumeError}</p>}
    </div>
  )
}

export function CookedDishesPage() {
  const { data: dishes, isLoading, isError, error } = useCookedDishesQuery()

  if (isLoading) return <p className="text-gray-500">Loading cooked dishes…</p>
  if (isError) return <p className="text-red-600">{extractErrorMessage(error)}</p>

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-gray-900">Cooked Dishes</h1>
      {dishes && dishes.length === 0 && <p className="text-gray-500">No cooked dishes yet.</p>}
      <div className="flex flex-col gap-3">
        {dishes?.map((dish) => (
          <DishCard key={dish.id} dish={dish} />
        ))}
      </div>
    </div>
  )
}
