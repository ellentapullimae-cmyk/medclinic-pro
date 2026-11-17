import type { ReactNode } from 'react';
import {
  Activity,
  BarChart3,
  Building2,
  CalendarDays,
  Check,
  Database,
  ShieldCheck,
  Stethoscope,
  Users,
} from 'lucide-react';

const FEATURES = [
  { icon: Users, text: 'Gestión completa de pacientes e historial clínico' },
  { icon: CalendarDays, text: 'Agenda de citas con confirmación en tiempo real' },
  { icon: Database, text: 'Facturación, cobros e inventario integrados' },
] as const;

interface AuthBrandPanelProps {
  headline: ReactNode;
  description: string;
}

export default function AuthBrandPanel({ headline, description }: AuthBrandPanelProps) {
  return (
    <div className="auth__brand">
      <div className="auth__fx" aria-hidden>
        <span className="auth__fx-blob auth__fx-blob--a" />
        <span className="auth__fx-blob auth__fx-blob--b" />
        <span className="auth__fx-grid" />
        <svg className="auth__ecg" viewBox="0 0 720 140" preserveAspectRatio="none">
          <path
            d="M0 78 H150 L172 78 L188 34 L206 118 L224 58 L240 78 H400 L424 78 L442 26 L460 122 L478 64 L494 78 H720"
            fill="none"
            stroke="url(#mcEcgGrad)"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <defs>
            <linearGradient id="mcEcgGrad" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#087bff" stopOpacity="0" />
              <stop offset="35%" stopColor="#087bff" stopOpacity="0.75" />
              <stop offset="70%" stopColor="#00d9ff" stopOpacity="0.95" />
              <stop offset="100%" stopColor="#00d9ff" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>
        <span className="auth__cross auth__cross--1">+</span>
        <span className="auth__cross auth__cross--2">+</span>
      </div>

      <header className="auth__logo" style={{ animationDelay: '0.05s' }}>
        <span className="auth__logo-mark">
          <Activity size={24} strokeWidth={2.4} />
        </span>
        <span className="auth__logo-text">
          <strong>MedClinic Pro</strong>
          <small>SISTEMA DE GESTIÓN CLÍNICA</small>
        </span>
      </header>

      <section className="auth__headline" style={{ animationDelay: '0.12s' }}>
        <h1>{headline}</h1>
        <p>{description}</p>
      </section>

      <section className="auth__features" style={{ animationDelay: '0.2s' }}>
        {FEATURES.map(({ icon: Icon, text }) => (
          <article className="auth__feature" key={text}>
            <span className="auth__feature-icon">
              <Icon size={18} strokeWidth={2.2} />
            </span>
            <p className="auth__feature-text">{text}</p>
            <span className="auth__feature-check" aria-label="Incluido">
              <Check size={13} strokeWidth={3} />
            </span>
          </article>
        ))}
      </section>

      <section className="auth__gallery" style={{ animationDelay: '0.28s' }} aria-hidden>
        <figure className="auth__shot auth__shot--portrait">
          <img src="/auth/doctor-female.jpg" alt="" loading="lazy" decoding="async" />
        </figure>
        <figure className="auth__shot auth__shot--portrait">
          <img src="/auth/doctor-male.jpg" alt="" loading="lazy" decoding="async" />
        </figure>
        <figure className="auth__shot auth__shot--building">
          <img src="/auth/clinic-building-alt.jpg" alt="" loading="lazy" decoding="async" />
          <figcaption className="auth__shot-caption">
            <Building2 size={15} strokeWidth={2.2} />
            CLÍNICA <strong>MedClinic Pro</strong>
          </figcaption>
        </figure>
      </section>

      <footer className="auth__footer" style={{ animationDelay: '0.36s' }}>
        <div className="auth__indicators">
          <span>
            <ShieldCheck size={14} /> JWT seguro
          </span>
          <span>
            <Stethoscope size={14} /> Roles clínicos
          </span>
          <span>
            <BarChart3 size={14} /> Reportes
          </span>
        </div>
        <div className="auth__uptime" title="Soporte y operatividad continua">
          <span className="auth__uptime-ring">
            <strong>24/7</strong>
          </span>
          <span className="auth__uptime-text">
            SOPORTE Y
            <br />
            OPERATIVIDAD CONTINUA
          </span>
        </div>
      </footer>
    </div>
  );
}
