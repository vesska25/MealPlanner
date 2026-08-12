import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { ActiveSuggestionResponse } from '../api/types'

export function useActiveSuggestionQuery() {
  return useQuery({
    queryKey: ['active-suggestion'],
    queryFn: () => apiFetch<ActiveSuggestionResponse>('/api/recipes/suggestions/active'),
    // A 404 ("no active suggestion") is a real, expected state, not a transient failure worth
    // retrying — the component distinguishes it from other errors and renders an empty state.
    retry: false,
  })
}
