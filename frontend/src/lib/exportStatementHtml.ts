/**
 * Captures rendered statement HTML for backend PDF conversion.
 * Includes inline styles from the preview root + minimal print CSS.
 */
export function captureStatementHtml(exportRootId: string): string {
  const root = document.getElementById(exportRootId)
  if (!root) {
    throw new Error(`Export root #${exportRootId} not found`)
  }

  const clone = root.cloneNode(true) as HTMLElement
  // Resolve img src to absolute URLs for backend renderer
  clone.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src')
    if (src && src.startsWith('/')) {
      img.setAttribute('src', `${window.location.origin}${src}`)
    }
  })

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <style>
    @page { size: A4; margin: 0; }
    body { margin: 0; padding: 0; background: #fff; }
    * { box-sizing: border-box; }
  </style>
</head>
<body>${clone.outerHTML}</body>
</html>`
}
