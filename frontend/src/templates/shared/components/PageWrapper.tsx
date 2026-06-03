import type { ReactNode, CSSProperties } from 'react'

type PageWrapperProps = {
  children: ReactNode
  exportRootId?: string
  watermark?: string
  style?: CSSProperties
  className?: string
}

export function PageWrapper({ children, exportRootId, watermark, style, className = '' }: PageWrapperProps) {
  return (
    <div
      id={exportRootId}
      className={`relative bg-white text-black shadow-md ${className}`}
      style={style}
    >
      {watermark && (
        <div
          className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center text-4xl font-bold text-red-100/40"
          style={{ transform: 'rotate(-28deg)' }}
          aria-hidden
        >
          {watermark}
        </div>
      )}
      {children}
    </div>
  )
}
