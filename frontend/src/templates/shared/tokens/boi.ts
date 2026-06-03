/**
 * BOI layout tokens — tuned to match original PDF reference (523pt content grid).
 */
export const boiTokens = {
  colors: {
    primary: '#f47b20',
    textDark: '#000000',
    textMuted: '#555555',
    border: '#000000',
    white: '#ffffff',
  },
  page: {
    marginTop: '28px',
    marginLeft: '36px',
    marginRight: '36px',
    width: '794px',
    minHeight: '1123px',
    fontFamily: 'Arial, Helvetica, sans-serif',
  },
  logo: {
    height: '26px',
  },
  filter: {
    width: '523pt',
    fontSize: '8px',
    lineHeight: 1.12,
    /** Label column — Transaction Date / Amount / Cheque */
    labelWidth: '100pt',
    /** From column — fits "from: DD-MM-YYYY" without excess trailing space */
    fromWidth: '128pt',
    /** Horizontal gap before "to:" — reduced vs wide 198pt backend columns */
    toGap: '12pt',
    rowGap: '4.5px',
    sectionMarginTop: '10px',
  },
  table: {
    width: '523pt',
    colWidths: ['37pt', '58pt', '188pt', '78pt', '78pt', '84pt'] as const,
    borderWidth: '1px',
    fontSize: '8px',
    lineHeight: 1.18,
    /** Reference PDF breaks remarks after 42 characters on line 1 */
    remarksLine1Chars: 42,
    headerMinHeight: '22px',
    headerPadV: '6px',
    headerPadH: '4px',
    rowMinHeight: '30px',
    bodyPadV: '5px',
    bodyPadH: '4px',
    amountPadRight: '4px',
  },
} as const
