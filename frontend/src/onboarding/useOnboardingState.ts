import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import type { OnboardingStateResponse } from '../api/types'

export function useOnboardingState() {
  return useQuery({
    queryKey: ['onboarding', 'state'],
    queryFn: () => apiFetch<OnboardingStateResponse>('/api/onboarding/state'),
  })
}
