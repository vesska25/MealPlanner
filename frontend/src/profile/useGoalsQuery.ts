import { useQuery } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../api/client'
import type { GoalsResponse } from '../api/types'

/**
 * FR-74: the backend returns 404 (not zeroed numbers) whenever goals are off or the profile is
 * incomplete — that's an expected, non-error state here, not a query failure.
 */
export function useGoalsQuery() {
  return useQuery({
    queryKey: ['profile', 'goals'],
    queryFn: async (): Promise<GoalsResponse | null> => {
      try {
        return await apiFetch<GoalsResponse>('/api/profile/goals')
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null
        }
        throw error
      }
    },
  })
}
