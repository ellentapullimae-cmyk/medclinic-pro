import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { Icon } from '../components/Icons';
import { Card, EmptyState, ErrorBox, Loading, PageHead, StatCard } from '../components/ui';

interface Doctor {
  id: number;
  full_name: string;
  specialty?: string;
}

function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');
}

const GRADIENTS = [
  'linear-gradient(135deg,#0878f9,#16c7f5)',
  'linear-gradient(135deg,#7c3aed,#a78bfa)',
  'linear-gradient(135deg,#0d9488,#2dd4bf)',
  'linear-gradient(135deg,#f59e0b,#fbbf24)',
  'linear-gradient(135deg,#e11d48,#fb7185)',
];

export default function Doctors() {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    api
      .get('/api/v1/users/doctors')
      .then((r) => {
        setDoctors(r.data ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar los médicos.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const specialties = Array.from(new Set(doctors.map((d) => d.specialty).filter(Boolean))) as string[];

  return (
    <>
      <PageHead
        title="Médicos"
        subtitle="Equipo profesional de la clínica"
        actions={
          <button className="btn btn--soft" onClick={load}>
            <Icon name="trend" size={16} />
            Actualizar
          </button>
        }
      />

      {error && <ErrorBox message={error} />}

      <div className="stat-grid">
        <StatCard label="Médicos activos" value={doctors.length} icon="doctor" tone="blue" />
        <StatCard label="Especialidades" value={specialties.length} icon="stethoscope" tone="violet" />
        <StatCard label="Disponibles para citas" value={doctors.length} icon="calendar" tone="green" />
      </div>

      <Card>
        <div className="card__head">
          <div>
            <h3>Directorio médico</h3>
            <span>Profesionales disponibles para agendar y atender citas</span>
          </div>
        </div>

        {loading ? (
          <Loading label="Cargando médicos…" />
        ) : doctors.length === 0 ? (
          <EmptyState title="Sin médicos registrados" hint="Crea un usuario con rol médico desde Usuarios." />
        ) : (
          <div className="doctor-grid">
            {doctors.map((d, i) => (
              <div key={d.id} className="doctor-card" style={{ animationDelay: `${i * 50}ms` }}>
                <div className="doctor-card__avatar" style={{ background: GRADIENTS[i % GRADIENTS.length] }}>
                  {initials(d.full_name) || '?'}
                </div>
                <div className="doctor-card__body">
                  <strong>{d.full_name}</strong>
                  <span>{d.specialty || 'Medicina general'}</span>
                  <em>ID #{d.id}</em>
                </div>
                <span className="badge badge--green doctor-card__badge">Activo</span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  );
}
