import { extractErrorMessage } from '../api/client'
import { useTelegramStatusQuery } from './useTelegramStatusQuery'
import { useGenerateLinkCode } from './useGenerateLinkCode'
import { useUnlinkTelegram } from './useUnlinkTelegram'

export function TelegramSection() {
  const { data: status, isLoading, isError, error } = useTelegramStatusQuery()
  const generateLinkCode = useGenerateLinkCode()
  const unlinkTelegram = useUnlinkTelegram()

  return (
    <section className="flex flex-col gap-2">
      <h2 className="font-semibold text-gray-900">Telegram</h2>
      <p className="text-sm text-gray-600">
        Link your Telegram account to get notifications and confirm cooking without opening the app.
      </p>

      {isLoading && <p className="text-sm text-gray-500">Loading…</p>}
      {isError && <p className="text-sm text-red-600">{extractErrorMessage(error)}</p>}

      {status && status.linked && (
        <div className="flex items-center gap-3">
          <span className="rounded bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">Connected</span>
          <button
            type="button"
            onClick={() => unlinkTelegram.mutate()}
            disabled={unlinkTelegram.isPending}
            className="text-sm text-red-600 hover:underline disabled:opacity-50"
          >
            {unlinkTelegram.isPending ? 'Disconnecting…' : 'Disconnect'}
          </button>
        </div>
      )}

      {status && !status.linked && !generateLinkCode.data && (
        <button
          type="button"
          onClick={() => generateLinkCode.mutate()}
          disabled={generateLinkCode.isPending}
          className="self-start rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
        >
          {generateLinkCode.isPending ? 'Generating…' : 'Connect Telegram'}
        </button>
      )}

      {generateLinkCode.isError && (
        <p className="text-sm text-red-600">{extractErrorMessage(generateLinkCode.error)}</p>
      )}

      {generateLinkCode.data && status && !status.linked && (
        <div className="flex flex-col gap-2 rounded border border-gray-200 bg-gray-50 p-3 text-sm">
          <a
            href={generateLinkCode.data.deepLink}
            target="_blank"
            rel="noreferrer"
            className="self-start rounded bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700"
          >
            Open in Telegram
          </a>
          <p className="text-gray-600">
            Or send this code to the bot manually: <span className="font-mono font-semibold">{generateLinkCode.data.code}</span>
          </p>
          <p className="text-xs text-gray-500">Valid for 10 minutes.</p>
        </div>
      )}
    </section>
  )
}
