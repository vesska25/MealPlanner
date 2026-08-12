import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, extractErrorMessage } from '../api/client'
import { ConfirmCookingForm } from '../cooking/ConfirmCookingForm'
import type { RejectionReason } from '../api/types'
import { useActiveSuggestionQuery } from './useActiveSuggestionQuery'
import { useAcceptSuggestion } from './useAcceptSuggestion'
import { useRejectSuggestion } from './useRejectSuggestion'

const REJECTION_REASONS: { value: RejectionReason; label: string }[] = [
  { value: 'DISLIKE_DISH', label: "Don't like this dish" },
  { value: 'NOT_TODAY', label: 'Not today' },
  { value: 'TAKES_TOO_LONG', label: 'Takes too long' },
  { value: 'DONT_WANT_CATEGORY', label: "Don't want this category" },
  { value: 'TIRED_OF_INGREDIENT', label: 'Tired of an ingredient' },
]

export function SuggestionPage() {
  const { data: suggestion, isLoading, isError, error } = useActiveSuggestionQuery()
  const acceptSuggestion = useAcceptSuggestion()
  const rejectSuggestion = useRejectSuggestion()
  const [showRejectForm, setShowRejectForm] = useState(false)
  const [rejectReason, setRejectReason] = useState<RejectionReason>('DISLIKE_DISH')
  const [feedback, setFeedback] = useState<string | null>(null)

  function handleAccept(recipeId: number) {
    setFeedback(null)
    acceptSuggestion.mutate(recipeId, {
      onSuccess: () => setFeedback('Suggestion accepted.'),
      onError: (err) => setFeedback(extractErrorMessage(err)),
    })
  }

  function handleReject(recipeId: number) {
    setFeedback(null)
    rejectSuggestion.mutate(
      { recipeId, reason: rejectReason },
      {
        onSuccess: () => {
          setFeedback('Suggestion rejected.')
          setShowRejectForm(false)
        },
        onError: (err) => setFeedback(extractErrorMessage(err)),
      },
    )
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

  const isPending = acceptSuggestion.isPending || rejectSuggestion.isPending

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
          onClick={() => handleAccept(suggestion.recipeId)}
          disabled={isPending}
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
          <select
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value as RejectionReason)}
            className="rounded border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          >
            {REJECTION_REASONS.map((reason) => (
              <option key={reason.value} value={reason.value}>
                {reason.label}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => handleReject(suggestion.recipeId)}
            disabled={isPending}
            className="self-start rounded bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            Submit rejection
          </button>
        </div>
      )}

      {feedback && <p className="text-sm text-gray-700">{feedback}</p>}

      <div>
        <h2 className="mb-2 text-sm font-semibold text-gray-900">Cooked it?</h2>
        <ConfirmCookingForm recipeId={suggestion.recipeId} basePortions={suggestion.basePortions} />
      </div>
    </div>
  )
}
