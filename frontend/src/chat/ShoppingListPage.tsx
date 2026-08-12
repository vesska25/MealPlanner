import { ChatPanel } from './ChatPanel'

export function ShoppingListPage() {
  return (
    <div className="flex flex-col gap-3">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Shopping List</h1>
        <p className="text-sm text-gray-600">Ask for a shopping list, or report what you bought.</p>
      </div>
      <ChatPanel scenario="SHOPPING_LIST" />
    </div>
  )
}
