import { ChatPanel } from './ChatPanel'

export function PantryAssistantPage() {
  return (
    <div className="flex flex-col gap-3">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Pantry Assistant</h1>
        <p className="text-sm text-gray-600">Add stock, discard spoiled items, or ask what you have.</p>
      </div>
      <ChatPanel scenario="PANTRY_ASSISTANT" />
    </div>
  )
}
