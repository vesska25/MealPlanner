import { useState, type FormEvent } from 'react'
import type { AgentScenario, AgentRunStatus } from '../api/types'
import { extractErrorMessage } from '../api/client'
import { useSendMessage } from './useSendMessage'
import { AgentStatusBadge } from './AgentStatusBadge'

interface ChatMessage {
  role: 'user' | 'agent'
  text: string
  status?: AgentRunStatus
}

/**
 * Reused across all three chat-driven scenarios. Each send is a self-contained agent run
 * (AgentChatController never threads conversation history server-side), so the transcript here
 * is purely local UI state — never persisted or resent.
 */
export function ChatPanel({ scenario }: { scenario: AgentScenario }) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const sendMessage = useSendMessage(scenario)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim()) return

    const userMessage = draft.trim()
    setMessages((prev) => [...prev, { role: 'user', text: userMessage }])
    setDraft('')

    sendMessage.mutate(userMessage, {
      onSuccess: (response) => {
        setMessages((prev) => [...prev, { role: 'agent', text: response.message, status: response.status }])
      },
      onError: (error) => {
        setMessages((prev) => [...prev, { role: 'agent', text: extractErrorMessage(error) }])
      },
    })
  }

  return (
    <div className="flex h-[70vh] flex-col rounded border border-gray-200 bg-white">
      <div className="flex-1 space-y-3 overflow-y-auto p-4">
        {messages.length === 0 && (
          <p className="text-sm text-gray-500">Send a message to get started.</p>
        )}
        {messages.map((message, index) => (
          <div key={index} className={message.role === 'user' ? 'text-right' : 'text-left'}>
            <div
              className={`inline-block max-w-[80%] rounded-lg px-3 py-2 text-sm ${
                message.role === 'user' ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-900'
              }`}
            >
              {message.text}
            </div>
            {message.status && (
              <div className="mt-1">
                <AgentStatusBadge status={message.status} />
              </div>
            )}
          </div>
        ))}
        {sendMessage.isPending && (
          <div className="text-left">
            <div className="inline-block animate-pulse rounded-lg bg-gray-100 px-3 py-2 text-sm text-gray-500">
              Thinking… this can take up to 30 seconds
            </div>
          </div>
        )}
      </div>
      <form onSubmit={handleSubmit} className="flex gap-2 border-t border-gray-200 p-3">
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Type a message…"
          className="flex-1 rounded border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
        />
        <button
          type="submit"
          disabled={sendMessage.isPending || !draft.trim()}
          className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </div>
  )
}
