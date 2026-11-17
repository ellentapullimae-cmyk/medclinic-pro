import type { ReactElement, SVGProps } from 'react';

type IconName =
  | 'logo'
  | 'dashboard'
  | 'users'
  | 'calendar'
  | 'stethoscope'
  | 'wallet'
  | 'box'
  | 'report'
  | 'logout'
  | 'bell'
  | 'menu'
  | 'search'
  | 'plus'
  | 'close'
  | 'check'
  | 'clock'
  | 'trend'
  | 'patient'
  | 'file'
  | 'shield'
  | 'doctor'
  | 'receipt';

const PATHS: Record<IconName, ReactElement> = {
  logo: (
    <>
      <path d="M12 3v18M3 12h18" strokeWidth="3.2" strokeLinecap="round" />
      <circle cx="12" cy="12" r="9.2" strokeWidth="2" />
    </>
  ),
  dashboard: (
    <>
      <rect x="3" y="3" width="7.5" height="7.5" rx="2" />
      <rect x="13.5" y="3" width="7.5" height="5" rx="2" />
      <rect x="13.5" y="10.5" width="7.5" height="10.5" rx="2" />
      <rect x="3" y="13" width="7.5" height="8" rx="2" />
    </>
  ),
  users: (
    <>
      <circle cx="9" cy="8" r="3.4" />
      <path d="M3.5 19.5c.6-3.3 2.9-5 5.5-5s4.9 1.7 5.5 5" />
      <path d="M16.5 6.6a3.4 3.4 0 0 1 0 6.6M17.5 14.9c2 .7 3.4 2.3 3.9 4.6" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="3" />
      <path d="M3 10h18M8 3v4M16 3v4" />
      <path d="M8 14h.01M12 14h.01M16 14h.01M8 17.5h.01M12 17.5h.01" strokeWidth="2.6" strokeLinecap="round" />
    </>
  ),
  stethoscope: (
    <>
      <path d="M6 3v5a4 4 0 0 0 8 0V3" />
      <path d="M4.5 3h3M12.5 3h3" />
      <path d="M10 12v2.5a5 5 0 0 0 10 0V13" />
      <circle cx="20" cy="11" r="2" />
    </>
  ),
  wallet: (
    <>
      <rect x="3" y="6" width="18" height="14" rx="3" />
      <path d="M3 10h18" />
      <circle cx="17" cy="15" r="1.4" fill="currentColor" stroke="none" />
      <path d="M6 6V4.8A1.8 1.8 0 0 1 7.8 3h9.4A2.8 2.8 0 0 1 20 5.8" />
    </>
  ),
  box: (
    <>
      <path d="M3.5 7.5 12 3l8.5 4.5v9L12 21l-8.5-4.5v-9Z" />
      <path d="M3.5 7.5 12 12l8.5-4.5M12 12v9" />
    </>
  ),
  report: (
    <>
      <path d="M6 3h9l4 4v14H6z" />
      <path d="M14 3v5h5" />
      <path d="M9.5 17v-3.5M12 17v-6M14.5 17v-2" strokeWidth="2.2" strokeLinecap="round" />
    </>
  ),
  logout: (
    <>
      <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
      <path d="M10 8l-4 4 4 4M6 12h10" />
    </>
  ),
  bell: (
    <>
      <path d="M6 9a6 6 0 1 1 12 0c0 4.5 1.5 6 1.5 6h-15S6 13.5 6 9Z" />
      <path d="M10 19a2.2 2.2 0 0 0 4 0" />
    </>
  ),
  menu: <path d="M4 7h16M4 12h16M4 17h16" strokeWidth="2.2" strokeLinecap="round" />,
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m16.5 16.5 4 4" strokeLinecap="round" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" strokeWidth="2.4" strokeLinecap="round" />,
  close: <path d="m6 6 12 12M18 6 6 18" strokeWidth="2.2" strokeLinecap="round" />,
  check: <path d="m5 12.5 4.5 4.5L19 7" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />,
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5.2l3.2 2" strokeLinecap="round" />
    </>
  ),
  trend: <path d="M3 17 9.5 10.5l4 4L21 7M15 7h6v6" strokeLinecap="round" strokeLinejoin="round" />,
  patient: (
    <>
      <circle cx="12" cy="8" r="3.6" />
      <path d="M5 20c.7-3.8 3.6-5.6 7-5.6s6.3 1.8 7 5.6" />
    </>
  ),
  file: (
    <>
      <path d="M6 3h9l4 4v14H6z" />
      <path d="M14 3v5h5M9 13h6M9 16.5h4" />
    </>
  ),
  shield: (
    <>
      <path d="M12 3 5 6v5.5c0 4.4 3 8 7 9.5 4-1.5 7-5.1 7-9.5V6l-7-3Z" />
      <path d="m9 12 2.2 2.2L15.5 10" />
    </>
  ),
  doctor: (
    <>
      <circle cx="12" cy="7.5" r="3.5" />
      <path d="M5 20c.8-3.6 3.7-5.5 7-5.5s6.2 1.9 7 5.5" />
      <path d="M12 12v3M10.5 13.5h3" strokeWidth="2.2" />
    </>
  ),
  receipt: (
    <>
      <path d="M6 3h12v18l-2.5-1.5L13 21l-2.5-1.5L8 21l-2-1.5V3Z" />
      <path d="M9 8h6M9 12h6M9 16h4" />
    </>
  ),
};

export type IconNameKey = IconName;

export function Icon({
  name,
  size = 20,
  ...rest
}: { name: IconName; size?: number } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {PATHS[name]}
    </svg>
  );
}
