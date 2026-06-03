import { kotakTokens } from '@/templates/shared/tokens/kotak'

/** Dual-logo row — compact logos, wide centre gutter (38 / 24 / 38). */
export function KotakLogoRow() {
  const t = kotakTokens.logo
  const [leftCol, gutterCol, rightCol] = t.colSplit

  return (
    <div
      className="grid items-end"
      style={{
        gridTemplateColumns: `${leftCol} ${gutterCol} ${rightCol}`,
        marginBottom: t.rowMarginBottom,
      }}
    >
      <img
        src="/image-left.png"
        alt=""
        style={{ height: t.leftHeight, width: 'auto', justifySelf: 'start' }}
        className="object-contain object-left"
      />
      <div aria-hidden />
      <img
        src="/kotak-logo.png"
        alt="Kotak Mahindra Bank"
        style={{ height: t.rightHeight, width: 'auto', justifySelf: 'end' }}
        className="object-contain object-right"
      />
    </div>
  )
}
