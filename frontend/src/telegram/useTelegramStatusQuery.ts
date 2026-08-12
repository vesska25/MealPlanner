import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { TelegramLinkStatusResponse } from '../api/types'

export function useTelegramStatusQuery() {
  return useQuery({
    queryKey: ['telegram', 'status'],
    queryFn: () => apiFetch<TelegramLinkStatusResponse>('/api/telegram/status'),
  })
}
