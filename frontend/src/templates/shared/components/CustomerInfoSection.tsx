type LabelValueProps = { label: string; value: string; boldValue?: boolean; gap?: string }

function LabelValue({ label, value, boldValue, gap = '14px' }: LabelValueProps) {
  return (
    <div className="flex" style={{ gap, marginBottom: '3px', fontSize: '8px' }}>
      <span className="shrink-0 text-[#969696]">{label}</span>
      <span className={boldValue ? 'font-bold text-[#111111]' : 'text-[#111111]'}>{value}</span>
    </div>
  )
}

type KotakCustomerProps = {
  customerName: string
  crn: string
  address: string
  city: string
  state: string
  pincode: string
  micr: string
  ifsc: string
  accountNumber: string
  accountType: string
  branchName: string
  columnGap?: string
}

export function KotakCustomerInfoSection({
  customerName,
  crn,
  address,
  city,
  state,
  pincode,
  micr,
  ifsc,
  accountNumber,
  accountType,
  branchName,
  columnGap = '54px',
}: KotakCustomerProps) {
  return (
    <div className="grid grid-cols-2" style={{ gap: columnGap, marginBottom: '10px' }}>
      <div>
        <p className="font-bold text-[#111111]" style={{ fontSize: '10.5px', marginBottom: '2px' }}>
          {customerName}
        </p>
        <p className="text-[#969696]" style={{ fontSize: '7.5px', marginBottom: '6px' }}>CRN {crn}</p>
        <p className="text-[#969696]" style={{ fontSize: '8px', lineHeight: 1.15, marginBottom: '2px' }}>{address}</p>
        <p className="text-[#969696]" style={{ fontSize: '8px', marginBottom: '2px' }}>{city} - {pincode}</p>
        <p className="text-[#969696]" style={{ fontSize: '8px', marginBottom: '6px' }}>{state} - India</p>
        <p style={{ fontSize: '8px' }}>
          <span className="text-[#969696]">MICR {micr} IFSC Code </span>
          <span className="text-[#111111]">{ifsc}</span>
        </p>
      </div>
      <div>
        <LabelValue label="Account No." value={accountNumber} boldValue />
        <LabelValue label="Account Type" value={accountType} />
        <LabelValue label="Branch" value={branchName} />
        <div style={{ marginBottom: '10px' }} />
        <LabelValue label="Branch Phone Number" value="9522262613" />
        <div style={{ marginBottom: '10px' }} />
        <LabelValue label="Account Status" value="Active" />
        <LabelValue label="Nominee Registered" value="No" />
        <div style={{ marginBottom: '10px' }} />
        <LabelValue label="Currency" value="INDIAN RUPEE" />
      </div>
    </div>
  )
}

/** Compact header for summary / continuation pages */
export function KotakContinuationHeader({
  customerName,
  accountNumber,
  periodLabel,
  insetH = '12px',
}: {
  customerName: string
  accountNumber: string
  periodLabel: string
  insetH?: string
}) {
  return (
    <div style={{ paddingLeft: insetH, paddingRight: insetH, marginBottom: '16px' }}>
      <p className="font-bold text-[#111111]" style={{ fontSize: '10.5px', marginBottom: '8px' }}>{customerName}</p>
      <p style={{ fontSize: '8px', marginBottom: '8px' }}>
        <span className="text-[#969696]">Account No. </span>
        <span className="text-[#111111]">{accountNumber}</span>
      </p>
      <p style={{ fontSize: '8px', margin: 0 }}>
        <span className="text-[#969696]">Account Statement </span>
        <span className="text-[#111111]">{periodLabel}</span>
      </p>
    </div>
  )
}
