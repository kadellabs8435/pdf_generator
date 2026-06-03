import { Info } from 'lucide-react'
import type { ReactNode } from 'react'

type InstructionBoxProps = {
  title?: string
  items: string[]
  className?: string
}

export function InstructionBox({ title = 'Important Information', items, className = '' }: InstructionBoxProps) {
  return (
    <div className={`rounded-2xl border border-[#a8a8a8] bg-white p-6 ${className}`}>
      <div className="mb-4 flex items-center gap-2 font-bold text-[#111111]" style={{ fontSize: '9px' }}>
        <Info className="h-4 w-4 text-[#374767]" aria-hidden />
        {title}
      </div>
      <ul className="space-y-3">
        {items.map((item, i) => (
          <InstructionItem key={i} text={item} />
        ))}
      </ul>
    </div>
  )
}

function InstructionItem({ text }: { text: string }) {
  return (
    <li className="flex gap-3" style={{ fontSize: '8px', lineHeight: 1.35, color: '#222222' }}>
      <span
        className="mt-1 shrink-0 rounded-full bg-[#b8b8b8]"
        style={{ width: '7px', height: '7px' }}
        aria-hidden
      />
      <span>{text}</span>
    </li>
  )
}

export function RememberBox({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-lg border border-[#bdbdbd] bg-[#f5f5f5] p-3">{children}</div>
  )
}
