import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'

export function useAcceptSuggestion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (recipeId: number) =>
      apiFetch<void>(`/api/recipes/suggestions/${recipeId}/accept`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['active-suggestion'] })
    },
  })
}
