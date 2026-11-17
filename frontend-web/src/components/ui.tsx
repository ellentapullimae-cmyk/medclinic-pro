import type { ReactNode } from 'react';
import { Icon } from './Icons';

/* --------------------------------- Card ---------------------------------- */

export function Card({
  children,
  className = '',
  pad = false,
  hover = false,
}: {
  children: ReactNode;
  className?: string;
  pad?: boolean;
  hover?: boolean;
}) {
  return (
    <div className={`card ${pad ? 'card--pad' : ''} ${hover ? 'card--hover' : ''} ${className}`}>
      {children}
    </div>
  );
}

export function CardHead({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="card__head">
      <div>
        <h3>{title}</h3>
        {subtitle && <span>{subtitle}</span>}
      </div>
      {actions && <div className="spacer">{actions}</div>}
    </div>
  );
}

/* ------------------------------- Page head ------------------------------- */

export function PageHead({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="page-head">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="page-head__actions">{actions}</div>}
    </div>
  );
}

/* -------------------------------- Stats ---------------------------------- */

const STAT_TONES = ['blue', 'green', 'amber', 'violet', 'red'] as const;
type StatTone = (typeof STAT_TONES)[number];

export function StatCard({
  label,
  value,
  hint,
  icon,
  tone = 'blue',
  index = 0,
}: {
  label: string;
  value: string | number;
  hint?: string;
  icon: Parameters<typeof Icon>[0]['name'];
  tone?: StatTone;
  index?: number;
}) {
  return (
    <div className="stat" style={{ animationDelay: `${index * 60}ms` }}>
      <div className={`stat__icon stat__icon--${tone}`}>
        <Icon name={icon} size={22} />
      </div>
      <div>
        <div className="stat__label">{label}</div>
        <div className="stat__value">{value}</div>
        {hint && <div className="stat__trend">{hint}</div>}
      </div>
    </div>
  );
}

/* -------------------------------- Badges --------------------------------- */

const BADGE_TONES: Record<string, string> = {
  pendiente: 'amber',
  confirmada: 'blue',
  atendida: 'green',
  cancelada: 'red',
  anulada: 'red',
  pagada: 'green',
  admin: 'violet',
  medico: 'blue',
  recepcion: 'green',
  activo: 'green',
  inactivo: 'gray',
};

export function Badge({ label, tone }: { label: string; tone?: string }) {
  const cls = tone ?? BADGE_TONES[label] ?? 'gray';
  return <span className={`badge badge--${cls}`}>{label}</span>;
}

/* Estados de chart reutilizables (Chart.js / recharts) */
export const chartPalette = {
  navy: '#061B3A',
  blue: '#0878F9',
  cyan: '#16C7F5',
  green: '#10B981',
  amber: '#F59E0B',
  red: '#EF4444',
  violet: '#7C3AED',
  muted: '#64748B',
  grid: 'rgba(226,234,244,.9)',
} as const;

/* ------------------------------- States ---------------------------------- */

export function Loading({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="loading-row">
      <div className="spinner" />
      {label}
    </div>
  );
}

export function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="empty">
      <Icon name="file" size={40} />
      <h4>{title}</h4>
      {hint && <p>{hint}</p>}
    </div>
  );
}

export function ErrorBox({ message }: { message: string }) {
  return <div className="error-box">{message}</div>;
}

export function SuccessBox({ message }: { message: string }) {
  return <div className="success-box">{message}</div>;
}

/* -------------------------------- Modal ---------------------------------- */

export function Modal({
  title,
  onClose,
  children,
  footer,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div className="modal__head">
          <h3>{title}</h3>
          <button className="modal__close" onClick={onClose} aria-label="Cerrar">
            <Icon name="close" size={16} />
          </button>
        </div>
        <div className="modal__body">{children}</div>
        {footer && <div className="modal__foot">{footer}</div>}
      </div>
    </div>
  );
}

/* -------------------------------- Forms ---------------------------------- */

export function Field({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="field">
      <label>
        {label} {required && <span>*</span>}
      </label>
      {children}
    </div>
  );
}
