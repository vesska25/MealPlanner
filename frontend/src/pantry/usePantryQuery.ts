import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { PantryItemResponse } from '../api/types'

export function usePantryQuery() {
  return useQuery({
    queryKey: ['pantry'],
    queryFn: () => apiFetch<PantryItemResponse[]>('/api/pantry'),
  })
}
