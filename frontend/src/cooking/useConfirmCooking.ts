import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { CookedDishResponse, ConfirmCookingRequest } from '../api/types'

export function useConfirmCooking() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ConfirmCookingRequest) =>
      apiFetch<CookedDishResponse>('/api/cooking/confirm', {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cooked-dishes'] })
      queryClient.invalidateQueries({ queryKey: ['pantry'] })
    },
  })
}
