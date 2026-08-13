import { useQueryClient } from '@tanstack/react-query'
import { ChatPanel } from '../chat/ChatPanel'
import { extractErrorMessage } from '../api/client'
import { useOnboardingState } from './useOnboardingState'
import { ScenarioPicker } from './ScenarioPicker'

/**
 * FR-10/FR-10b: unlike the other three chat pages, this one waits for the fetched draft state
 * before mounting ChatPanel — the panel's transcript is seeded once via an `initialMessages`
 * prop (read only on mount), so rendering it before the real turns arrive would freeze it on an
 * empty transcript even after the fetch resolves.
 */
export function OnboardingPage() {
  const queryClient = useQueryClient()
  const state = useOnboardingState()

  function refetchState() {
    queryClient.invalidateQueries({ queryKey: ['onboarding', 'state'] })
  }

  return (
    <div className="flex flex-col gap-3">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Getting set up</h1>
        <p className="text-sm text-gray-600">
          A few questions about how you cook, so suggestions actually fit you.
        </p>
      </div>

      {state.isLoading && <p className="text-sm text-gray-500">Loading…</p>}
      {state.isError && <p className="text-sm text-red-600">{extractErrorMessage(state.error)}</p>}

      {state.data?.profileFinalized && <ScenarioPicker />}

      {state.data && !state.data.profileFinalized && (
        <ChatPanel scenario="ONBOARDING" initialMessages={state.data.recentTurns} onMessageSent={refetchState} />
      )}
    </div>
  )
}
