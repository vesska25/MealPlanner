import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { ShoppingListResponse } from '../api/types'

export function useShoppingListQuery() {
  return useQuery({
    queryKey: ['shopping-list'],
    queryFn: () => apiFetch<ShoppingListResponse>('/api/shopping-list'),
    // A 404 ("no list generated yet") is a real, expected state, not a transient failure worth
    // retrying — the component distinguishes it from other errors and renders an empty state.
    retry: false,
  })
}
