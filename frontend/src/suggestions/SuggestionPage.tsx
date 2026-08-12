import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError, extractErrorMessage } from '../api/client'
import { useSendMessage } from '../chat/useSendMessage'
import { AgentStatusBadge } from '../chat/AgentStatusBadge'
import { ConfirmCookingForm } from '../cooking/ConfirmCookingForm'
import type { AgentRunStatus } from '../api/types'
import { useActiveSuggestionQuery } from './useActiveSuggestionQuery'

export function SuggestionPage() {
  const { data: suggestion, isLoading, isError, error } = useActiveSuggestionQuery()
  const sendToMealPlanning = useSendMessage('MEAL_PLANNING')
  const queryClient = useQueryClient()
  const [rejectReason, setRejectReason] = useState('')
  const [showRejectForm, setShowRejectForm] = useState(false)
  const [reply, setReply] = useState<{ text: string; status?: AgentRunStatus } | null>(null)

  function handleAccept() {
    setReply(null)
    sendToMealPlanning.mutate('I accept the suggested recipe.', {
      onSuccess: (response) => {
        setReply({ text: response.message, status: response.status })
        queryClient.invalidateQueries({ queryKey: ['active-suggestion'] })
      },
      onError: (err) => setReply({ text: extractErrorMessage(err) }),
    })
  }

  function handleReject() {
    if (!rejectReason.trim()) return
    setReply(null)
    sendToMealPlanning.mutate(`I reject the suggested recipe because: ${rejectReason.trim()}`, {
      onSuccess: (response) => {
        setReply({ text: response.message, status: response.status })
        setShowRejectForm(false)
        setRejectReason('')
        queryClient.invalidateQueries({ queryKey: ['active-suggestion'] })
      },
      onError: (err) => setReply({ text: extractErrorMessage(err) }),
    })
  }

  if (isLoading) return <p className="text-gray-500">Loading suggestion…</p>

  const isNoActiveSuggestion = isError && error instanceof ApiError && error.status === 404
  if (isNoActiveSuggestion) {
    return (
      <p className="text-gray-500">
        No active suggestion.{' '}
        <Link to="/chat/meal-planning" className="text-emerald-700 underline">
          Ask the meal planning assistant for one
        </Link>
        .
      </p>
    )
  }
  if (isError) return <p className="text-red-600">{extractErrorMessage(error)}</p>
  if (!suggestion) return null

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">{suggestion.recipeName}</h1>
        <p className="text-sm text-gray-600">
          {suggestion.cookTimeMinutes} min · {suggestion.basePortions} portions · score {suggestion.score.toFixed(2)}
        </p>
      </div>

      {suggestion.requiredEquipment.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {suggestion.requiredEquipment.map((equipment) => (
            <span key={equipment} className="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-700">
              {equipment}
            </span>
          ))}
        </div>
      )}

      <ul className="list-inside list-disc text-sm text-gray-800">
        {suggestion.ingredients.map((ingredient, index) => (
          <li key={index}>
            {ingredient.productName} — {ingredient.quantity} {ingredient.unit.toLowerCase()}
          </li>
        ))}
      </ul>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={handleAccept}
          disabled={sendToMealPlanning.isPending}
          className="rounded bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          Accept
        </button>
        <button
          type="button"
          onClick={() => setShowRejectForm((prev) => !prev)}
          className="rounded border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
        >
          Reject
        </button>
      </div>

      {showRejectForm && (
        <div className="flex flex-col gap-2">
          <textarea
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="Why are you rejecting this suggestion?"
            className="rounded border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            rows={2}
          />
          <button
            type="button"
            onClick={handleReject}
            disabled={sendToMealPlanning.isPending || !rejectReason.trim()}
            className="self-start rounded bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            Submit rejection
          </button>
        </div>
      )}

      {reply && (
        <div className="rounded border border-gray-200 bg-gray-50 p-3 text-sm">
          <p>{reply.text}</p>
          {reply.status && (
            <div className="mt-1">
              <AgentStatusBadge status={reply.status} />
            </div>
          )}
        </div>
      )}

      <div>
        <h2 className="mb-2 text-sm font-semibold text-gray-900">Cooked it?</h2>
        <ConfirmCookingForm recipeId={suggestion.recipeId} basePortions={suggestion.basePortions} />
      </div>
    </div>
  )
}
