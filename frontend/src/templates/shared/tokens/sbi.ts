/** SBI layout tokens — scaffold; tune during SBI migration phase. */
export const sbiTokens = {
  colors: {
    primary: '#1f4f9a',
    bannerBg: '#e8eef7',
    textDark: '#000000',
    border: '#999999',
    white: '#ffffff',
  },
  page: {
    marginTop: '0px',
    marginLeft: '20px',
    marginRight: '20px',
    width: '794px',
    minHeight: '1123px',
    fontFamily: 'Arial, Helvetica, sans-serif',
  },
  title: {
    fontSize: '13px',
    branchSize: '10px',
  },
  table: {
    headerMinHeight: '36px',
    rowMinHeight: '44px',
    colWidths: ['54px', '54px', '154px', '54px', '77px', '77px', '85px'],
  },
} as const
