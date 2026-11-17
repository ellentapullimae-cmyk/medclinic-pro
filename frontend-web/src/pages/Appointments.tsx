import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/Icons';
import { Badge, Card, EmptyState, ErrorBox, Field, Loading, Modal, PageHead, SuccessBox } from '../components/ui';

interface Patient {
  id: number;
  first_name: string;
  last_name: string;
}
interface Doctor {
  id: number;
  full_name: string;
  specialty?: string;
}
interface Appointment {
  id: number;
  patient_id: number;
  doctor_id: number;
  start_time: string;
  end_time: string;
  status: string;
  reason?: string;
  notes?: string;
  patient?: Patient;
  doctor?: Doctor;
}

const STATUS_FILTERS = ['', 'pendiente', 'confirmada', 'atendida', 'cancelada'];

const EMPTY_FORM = { patient_id: '', doctor_id: '', start_time: '', end_time: '', reason: '' };

export default function Appointments() {
  const { user: me } = useAuth();
  const canCreate = me?.role === 'admin' || me?.role === 'recepcion';
  const canCancel = canCreate;
  const canUpdateStatus = me?.role === 'admin' || me?.role === 'medico' || me?.role === 'recepcion';
  const [list, setList] = useState<Appointment[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [status, setStatus] = useState('');
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback((s: string) => {
    setLoading(true);
    api
      .get('/api/v1/appointments', { params: { status: s || undefined, page_size: 100 } })
      .then((r) => {
        setList(r.data.items ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar las citas.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(status);
    api.get('/api/v1/patients', { params: { page_size: 100 } }).then((r) => setPatients(r.data.items ?? [])).catch(() => {});
    api.get('/api/v1/users/doctors').then((r) => setDoctors(r.data ?? [])).catch(() => {});
  }, [status, load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post('/api/v1/appointments', {
        patient_id: Number(form.patient_id),
        doctor_id: Number(form.doctor_id),
        start_time: form.start_time,
        end_time: form.end_time,
        reason: form.reason || null,
      });
      setNotice('Cita creada correctamente.');
      setShowForm(false);
      setForm({ ...EMPTY_FORM });
      load(status);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo crear la cita.');
    } finally {
      setSaving(false);
    }
  };

  const setStatusAction = async (id: number, next: string) => {
    setError('');
    try {
      await api.patch(`/api/v1/appointments/${id}`, { status: next });
      setNotice(`Cita marcada como ${next}.`);
      load(status);
    } catch {
      setError('No se pudo actualizar el estado de la cita.');
    }
  };

  const cancel = async (id: number) => {
    if (!confirm('¿Cancelar esta cita?')) return;
    try {
      await api.delete(`/api/v1/appointments/${id}`);
      setNotice('Cita cancelada.');
      load(status);
    } catch {
      setError('No se pudo cancelar la cita.');
    }
  };

  const set = (key: keyof typeof EMPTY_FORM) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm({ ...form, [key]: e.target.value });

  const today = new Date().toISOString().slice(0, 10);

  return (
    <>
      <PageHead
        title="Citas médicas"
        subtitle={`${list.length} citas en la vista actual`}
        actions={
          canCreate ? (
            <button className="btn btn--primary" onClick={() => setShowForm(true)}>
              <Icon name="plus" size={16} />
              Nueva cita
            </button>
          ) : null
        }
      />

      {error && <ErrorBox message={error} />}
      {notice && <SuccessBox message={notice} />}

      <div className="toolbar">
        <div className="chips">
          {STATUS_FILTERS.map((s) => (
            <button key={s || 'all'} className={`chip ${status === s ? 'active' : ''}`} onClick={() => setStatus(s)}>
              {s || 'Todas'}
            </button>
          ))}
        </div>
      </div>

      <Card>
        {loading ? (
          <Loading label="Cargando agenda…" />
        ) : list.length === 0 ? (
          <EmptyState title="Sin citas" hint="No hay citas para el filtro seleccionado." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Paciente</th>
                  <th>Médico</th>
                  <th>Fecha y hora</th>
                  <th>Motivo</th>
                  <th>Estado</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {[...list]
                  .sort((a, b) => a.start_time.localeCompare(b.start_time))
                  .map((a) => {
                    const isToday = a.start_time.slice(0, 10) === today;
                    return (
                      <tr key={a.id}>
                        <td className="cell-strong">
                          {a.patient ? `${a.patient.last_name}, ${a.patient.first_name}` : `#${a.patient_id}`}
                          {isToday && (
                            <span className="badge badge--blue" style={{ marginLeft: 8 }}>
                              hoy
                            </span>
                          )}
                        </td>
                        <td className="cell-muted">{a.doctor?.full_name ?? `#${a.doctor_id}`}</td>
                        <td className="cell-muted">{a.start_time.replace('T', ' ').slice(0, 16)}</td>
                        <td className="cell-muted">{a.reason || '—'}</td>
                        <td>
                          <Badge label={a.status} />
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                            {canUpdateStatus && a.status === 'pendiente' && (
                              <button className="btn btn--soft btn--sm" onClick={() => setStatusAction(a.id, 'confirmada')}>
                                Confirmar
                              </button>
                            )}
                            {canUpdateStatus && a.status === 'confirmada' && (
                              <button className="btn btn--soft btn--sm" onClick={() => setStatusAction(a.id, 'atendida')}>
                                Atender
                              </button>
                            )}
                            {canCancel && (a.status === 'pendiente' || a.status === 'confirmada') && (
                              <button className="btn btn--danger-soft btn--sm" onClick={() => cancel(a.id)}>
                                Cancelar
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {showForm && (
        <Modal
          title="Nueva cita"
          onClose={() => setShowForm(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowForm(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="appt-form" disabled={saving}>
                {saving ? 'Creando…' : 'Crear cita'}
              </button>
            </>
          }
        >
          <form id="appt-form" onSubmit={create} className="form-grid">
            <Field label="Paciente" required>
              <select className="select" value={form.patient_id} onChange={set('patient_id')} required>
                <option value="">Seleccionar…</option>
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.last_name}, {p.first_name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Médico" required>
              <select className="select" value={form.doctor_id} onChange={set('doctor_id')} required>
                <option value="">Seleccionar…</option>
                {doctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.full_name}
                    {d.specialty ? ` — ${d.specialty}` : ''}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Inicio" required>
              <input className="input" type="datetime-local" value={form.start_time} onChange={set('start_time')} required />
            </Field>
            <Field label="Fin" required>
              <input className="input" type="datetime-local" value={form.end_time} onChange={set('end_time')} required />
            </Field>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Motivo">
                <input className="input" value={form.reason} onChange={set('reason')} placeholder="Consulta de control…" />
              </Field>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
