import { useMutation } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { GenerateLinkCodeResponse } from '../api/types'

export function useGenerateLinkCode() {
  return useMutation({
    mutationFn: () => apiFetch<GenerateLinkCodeResponse>('/api/telegram/link-code', { method: 'POST' }),
  })
}
