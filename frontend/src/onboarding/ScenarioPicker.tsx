import { useNavigate } from 'react-router-dom'

/** FR-15: the two starting scenarios map directly onto the existing chat routes. */
export function ScenarioPicker() {
  const navigate = useNavigate()

  return (
    <div className="flex flex-col gap-3 rounded border border-emerald-200 bg-emerald-50 p-4">
      <p className="text-sm font-medium text-emerald-900">
        You're all set. What would you like to do first?
      </p>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => navigate('/chat/meal-planning')}
          className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
        >
          Cook from what I have at home
        </button>
        <button
          type="button"
          onClick={() => navigate('/chat/shopping-list')}
          className="rounded border border-emerald-600 px-4 py-2 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
        >
          I'm going shopping — what should I buy?
        </button>
      </div>
    </div>
  )
}
