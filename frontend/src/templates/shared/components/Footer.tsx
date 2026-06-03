type StatementFooterProps = {
  generatedOn: string
  pageNumber?: number
  totalPages?: number
  insetH?: string
  fontSize?: string
}

export function StatementFooter({
  generatedOn,
  pageNumber = 1,
  totalPages = 1,
  insetH = '0px',
  fontSize = '7px',
}: StatementFooterProps) {
  return (
    <div
      className="flex justify-between text-[#969696]"
      style={{ paddingLeft: insetH, paddingRight: insetH, fontSize, marginTop: '12px' }}
    >
      <span>Statement Generated on {generatedOn}</span>
      <span>Page {pageNumber} of {totalPages}</span>
    </div>
  )
}
