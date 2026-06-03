import { cn } from '@/lib/utils'
import type { StatementStatus } from '@/types'

const styles: Record<StatementStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-800',
  PREVIEWED: 'bg-blue-100 text-blue-800',
  GENERATED: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-green-100 text-green-800',
}

export function StatusBadge({ status }: { status: StatementStatus }) {
  return (
    <span className={cn('inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium', styles[status])}>
      {status}
    </span>
  )
}
