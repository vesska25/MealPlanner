import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { RejectionReason } from '../api/types'

export function useRejectSuggestion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ recipeId, reason }: { recipeId: number; reason: RejectionReason }) =>
      apiFetch<void>(`/api/recipes/suggestions/${recipeId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['active-suggestion'] })
    },
  })
}
