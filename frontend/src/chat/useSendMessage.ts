import { useMutation } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { AgentScenario, ChatResponse } from '../api/types'

export function useSendMessage(scenario: AgentScenario) {
  return useMutation({
    mutationFn: (message: string) =>
      apiFetch<ChatResponse>(`/api/agent/${scenario}/messages`, {
        method: 'POST',
        body: JSON.stringify({ message }),
      }),
  })
}
