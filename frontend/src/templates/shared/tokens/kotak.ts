/** Kotak layout tokens — mirror backend KotakPdfStyles (pt → px at 96dpi ≈ 1.333, use px for CSS). */
export const kotakTokens = {
  colors: {
    red: '#ed1c24',
    headerGray: '#8e8e8e',
    textDark: '#111111',
    textMuted: '#969696',
    border: '#bdbdbd',
    boxBg: '#f5f5f5',
    iconBlue: '#374767',
    white: '#ffffff',
  },
  page: {
    marginTop: '10px',
    marginLeft: '38px',
    marginRight: '38px',
    marginBottom: '34px',
    width: '794px', // A4 @ 96dpi
    minHeight: '1123px',
    fontFamily: 'Arial, Helvetica, sans-serif',
  },
  logo: {
    /** Reference PDF — compact logos with wide centre gutter */
    leftHeight: '36px',
    rightHeight: '34px',
    rowMarginBottom: '10px',
    /** Left logo | centre gap | right logo — matches KotakPdfService 38/24/38 split */
    colSplit: ['34%', '32%', '34%'] as const,
  },
  title: {
    fontSize: '26px',
    fontWeight: 700,
    dateSize: '9px',
    titleToDateGap: '2px',
  },
  body: {
    fontSize: '8px',
    tableFontSize: '7px',
    tableHeaderSize: '8.5px',
  },
  customer: {
    columnGap: '54px',
    rowGap: '6px',
    labelValueGap: '14px',
  },
  redBar: {
    height: '24px',
    fontSize: '9px',
  },
  table: {
    headerHeight: '22px',
    rowMinHeight: '15px',
    cellPadH: '4px',
    cellPadV: '1px',
    colWidths: ['34px', '92px', '235px', '128px', '95px', '95px', '95px'],
  },
  summary: {
    insetH: '12px',
  },
} as const
