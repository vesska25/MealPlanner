import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { AgentRunStatus } from '../api/types'
import { AgentStatusBadge } from './AgentStatusBadge'

const ALL_STATUSES: AgentRunStatus[] = [
  'RUNNING',
  'FINAL_RESPONSE',
  'FALLBACK_RESPONSE',
  'TOOL_ERROR_FATAL',
  'PERMISSION_DENIED',
  'VALIDATION_FAILED',
  'ITERATION_LIMIT',
  'LLM_TIMEOUT',
  'BUDGET_EXCEEDED',
]

describe('AgentStatusBadge', () => {
  it.each(ALL_STATUSES)('renders a label for every AgentRunStatus value (%s)', (status) => {
    render(<AgentStatusBadge status={status} />)
    expect(screen.getByText(/.+/)).toBeInTheDocument()
  })

  it('gives FALLBACK_RESPONSE a visually distinct (amber) treatment, per AI-20c', () => {
    render(<AgentStatusBadge status="FALLBACK_RESPONSE" />)
    const badge = screen.getByText('Fallback pick')
    expect(badge.className).toContain('amber')
  })

  it('gives error statuses a red treatment distinct from the neutral final-response badge', () => {
    render(<AgentStatusBadge status="TOOL_ERROR_FATAL" />)
    expect(screen.getByText('Tool error').className).toContain('red')
  })
})
