import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../api/client'

export function useUnlinkTelegram() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => apiFetch<void>('/api/telegram', { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['telegram', 'status'] })
    },
  })
}
