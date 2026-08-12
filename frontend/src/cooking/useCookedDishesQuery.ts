import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { CookedDishResponse } from '../api/types'

export function useCookedDishesQuery() {
  return useQuery({
    queryKey: ['cooked-dishes'],
    queryFn: () => apiFetch<CookedDishResponse[]>('/api/cooked-dishes'),
  })
}
