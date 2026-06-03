type RedSectionBarProps = {
  title: string
  backgroundColor?: string
  height?: string
  fontSize?: string
  insetH?: string
  className?: string
}

export function RedSectionBar({
  title,
  backgroundColor = '#ed1c24',
  height = '24px',
  fontSize = '9px',
  insetH = '0px',
  className = '',
}: RedSectionBarProps) {
  return (
    <div style={{ paddingLeft: insetH, paddingRight: insetH }} className={className}>
      <div
        className="flex items-center justify-center font-bold text-white"
        style={{ backgroundColor, minHeight: height, fontSize }}
      >
        {title}
      </div>
    </div>
  )
}
