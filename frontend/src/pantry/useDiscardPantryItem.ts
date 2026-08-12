import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { DiscardReason } from '../api/types'

export function useDiscardPantryItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ itemId, reason }: { itemId: number; reason: DiscardReason }) =>
      apiFetch<void>(`/api/pantry/${itemId}/discard`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pantry'] })
    },
  })
}
