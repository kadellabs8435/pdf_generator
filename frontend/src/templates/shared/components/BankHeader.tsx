import type { ReactNode } from 'react'

type BankHeaderProps = {
  title: string
  subtitle?: string
  titleSize?: string
  titleWeight?: number
  subtitleSize?: string
  subtitleColor?: string
  className?: string
  children?: ReactNode
}

export function BankHeader({
  title,
  subtitle,
  titleSize = '22px',
  titleWeight = 700,
  subtitleSize = '9px',
  subtitleColor = '#969696',
  className = '',
  children,
}: BankHeaderProps) {
  return (
    <div className={className}>
      {children}
      <h1
        className="leading-tight text-[#111111]"
        style={{ fontSize: titleSize, fontWeight: titleWeight, marginBottom: '2px' }}
      >
        {title}
      </h1>
      {subtitle && (
        <p style={{ fontSize: subtitleSize, color: subtitleColor, margin: 0 }}>{subtitle}</p>
      )}
    </div>
  )
}
