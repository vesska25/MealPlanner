import type { AgentRunStatus } from '../api/types'

const STATUS_STYLES: Record<AgentRunStatus, { label: string; className: string }> = {
  RUNNING: { label: 'Thinking…', className: 'bg-gray-100 text-gray-700' },
  FINAL_RESPONSE: { label: 'Answered', className: 'bg-gray-100 text-gray-700' },
  FALLBACK_RESPONSE: { label: 'Fallback pick', className: 'bg-amber-100 text-amber-800' },
  TOOL_ERROR_FATAL: { label: 'Tool error', className: 'bg-red-100 text-red-800' },
  PERMISSION_DENIED: { label: 'Not permitted', className: 'bg-red-100 text-red-800' },
  VALIDATION_FAILED: { label: 'Invalid request', className: 'bg-red-100 text-red-800' },
  ITERATION_LIMIT: { label: 'Ran out of steps', className: 'bg-red-100 text-red-800' },
  LLM_TIMEOUT: { label: 'Timed out', className: 'bg-red-100 text-red-800' },
  BUDGET_EXCEEDED: { label: 'Budget exceeded', className: 'bg-red-100 text-red-800' },
}

export function AgentStatusBadge({ status }: { status: AgentRunStatus }) {
  const { label, className } = STATUS_STYLES[status]
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}>
      {label}
    </span>
  )
}
