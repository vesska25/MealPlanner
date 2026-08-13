import { useGoalsQuery } from './useGoalsQuery'

/**
 * FR-74: renders nothing at all — not a "disabled" placeholder, not zeroed numbers — whenever
 * goals are off or the profile is incomplete. `useGoalsQuery` maps that case to `data: null`.
 */
export function GoalsSection() {
  const goals = useGoalsQuery()

  if (!goals.data) {
    return null
  }

  return (
    <section className="flex flex-col gap-2">
      <h2 className="font-semibold text-gray-900">Daily targets</h2>
      {goals.data.flooredToMinimum && (
        <p className="text-sm text-amber-700">
          Your goal would have gone below a safe minimum, so this is a moderated target instead.
        </p>
      )}
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded border border-gray-200 p-3">
          <dt className="text-xs text-gray-500">Calories</dt>
          <dd className="text-lg font-semibold text-gray-900">{Math.round(goals.data.dailyKcal)} kcal</dd>
        </div>
        <div className="rounded border border-gray-200 p-3">
          <dt className="text-xs text-gray-500">Protein</dt>
          <dd className="text-lg font-semibold text-gray-900">{Math.round(goals.data.proteinGrams)} g</dd>
        </div>
        <div className="rounded border border-gray-200 p-3">
          <dt className="text-xs text-gray-500">Fat</dt>
          <dd className="text-lg font-semibold text-gray-900">{Math.round(goals.data.fatGrams)} g</dd>
        </div>
        <div className="rounded border border-gray-200 p-3">
          <dt className="text-xs text-gray-500">Carbs</dt>
          <dd className="text-lg font-semibold text-gray-900">{Math.round(goals.data.carbsGrams)} g</dd>
        </div>
      </dl>
    </section>
  )
}
