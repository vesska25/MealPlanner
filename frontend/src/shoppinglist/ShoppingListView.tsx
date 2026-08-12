import { ApiError, extractErrorMessage } from '../api/client'
import { useShoppingListQuery } from './useShoppingListQuery'

const BLOCK_LABELS = {
  DEFINITELY_NEED: 'Definitely need',
  CHECK_MAYBE_OUT: 'Check — might be out',
} as const

export function ShoppingListView() {
  const { data: list, isLoading, isError, error } = useShoppingListQuery()

  if (isLoading) return <p className="text-gray-500">Loading shopping list…</p>

  const isNoListYet = isError && error instanceof ApiError && error.status === 404
  if (isNoListYet) {
    return <p className="text-gray-500">No shopping list yet — ask below to generate one.</p>
  }
  if (isError) return <p className="text-red-600">{extractErrorMessage(error)}</p>
  if (!list) return null

  if (list.items.length === 0) {
    return <p className="text-gray-500">Your last generated list had nothing on it.</p>
  }

  const blocks = ['DEFINITELY_NEED', 'CHECK_MAYBE_OUT'] as const

  return (
    <div className="flex flex-col gap-3 rounded border border-gray-200 bg-white p-4">
      {blocks.map((block) => {
        const items = list.items.filter((item) => item.block === block)
        if (items.length === 0) return null
        return (
          <div key={block}>
            <h3 className="mb-1 text-sm font-semibold text-gray-900">{BLOCK_LABELS[block]}</h3>
            <ul className="list-inside list-disc text-sm text-gray-800">
              {items.map((item) => (
                <li key={item.id} className={item.status !== 'PENDING' ? 'text-gray-400 line-through' : undefined}>
                  {item.productName} — {item.quantity} {item.unit.toLowerCase()}
                </li>
              ))}
            </ul>
          </div>
        )
      })}
    </div>
  )
}
