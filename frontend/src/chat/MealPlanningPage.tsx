import { ChatPanel } from './ChatPanel'

export function MealPlanningPage() {
  return (
    <div className="flex flex-col gap-3">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Meal Planning</h1>
        <p className="text-sm text-gray-600">Ask for a recipe suggestion based on what's in your pantry.</p>
      </div>
      <ChatPanel scenario="MEAL_PLANNING" />
    </div>
  )
}
