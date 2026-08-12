import { useQueryClient } from '@tanstack/react-query'
import { ChatPanel } from './ChatPanel'
import { ShoppingListView } from '../shoppinglist/ShoppingListView'

export function ShoppingListPage() {
  const queryClient = useQueryClient()

  return (
    <div className="flex flex-col gap-3">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Shopping List</h1>
        <p className="text-sm text-gray-600">Ask for a shopping list, or report what you bought.</p>
      </div>
      <ShoppingListView />
      <ChatPanel
        scenario="SHOPPING_LIST"
        onMessageSent={() => queryClient.invalidateQueries({ queryKey: ['shopping-list'] })}
      />
    </div>
  )
}
