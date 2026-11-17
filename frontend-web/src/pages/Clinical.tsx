import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/Icons';
import { Badge, Card, EmptyState, ErrorBox, Field, Loading, Modal, PageHead, SuccessBox } from '../components/ui';

interface Patient {
  id: number;
  file_number: string;
  first_name: string;
  last_name: string;
}
interface Record {
  id: number;
  visit_date: string;
  diagnosis?: string;
  treatment?: string;
  notes?: string;
  vitals?: string;
  doctor?: { full_name: string; specialty?: string };
}

const EMPTY_FORM = { diagnosis: '', treatment: '', notes: '', vitals: '', visit_date: '' };

export default function Clinical() {
  const { user: me } = useAuth();
  const canCreate = me?.role === 'medico';
  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientId, setPatientId] = useState('');
  const [records, setRecords] = useState<Record[]>([]);
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .get('/api/v1/patients', { params: { page_size: 100 } })
      .then((r) => setPatients(r.data.items ?? []))
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar los pacientes.')));
  }, []);

  const load = useCallback((id: string) => {
    if (!id) {
      setRecords([]);
      return;
    }
    setLoading(true);
    api
      .get(`/api/v1/patients/${id}/records`, { params: { page_size: 100 } })
      .then((r) => {
        setRecords(r.data.items ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudo cargar el historial clínico.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(patientId);
  }, [patientId, load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!patientId) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post(`/api/v1/patients/${patientId}/records`, {
        diagnosis: form.diagnosis || null,
        treatment: form.treatment || null,
        notes: form.notes || null,
        vitals: form.vitals || null,
        visit_date: form.visit_date || null,
      });
      setNotice('Consulta registrada en el historial.');
      setShowForm(false);
      setForm({ ...EMPTY_FORM });
      load(patientId);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo registrar la consulta.');
    } finally {
      setSaving(false);
    }
  };

  const selected = patients.find((p) => String(p.id) === patientId);

  const set = (key: keyof typeof EMPTY_FORM) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [key]: e.target.value });

  return (
    <>
      <PageHead
        title="Historial clínico"
        subtitle="Consultas, diagnósticos y tratamientos"
        actions={
          canCreate ? (
            <button className="btn btn--primary" onClick={() => setShowForm(true)} disabled={!patientId}>
              <Icon name="plus" size={16} />
              Nueva consulta
            </button>
          ) : (
            <span className="badge badge--gray" style={{ padding: '8px 14px' }}>
              Lectura · crear requiere rol médico
            </span>
          )
        }
      />

      {error && <ErrorBox message={error} />}
      {notice && <SuccessBox message={notice} />}

      <div className="toolbar">
        <div className="search" style={{ maxWidth: 420 }}>
          <Icon name="patient" size={17} />
          <select
            className="select"
            style={{ paddingLeft: 40 }}
            value={patientId}
            onChange={(e) => setPatientId(e.target.value)}
          >
            <option value="">Selecciona un paciente…</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>
                {p.last_name}, {p.first_name} — {p.file_number}
              </option>
            ))}
          </select>
        </div>
        {selected && (
          <span className="badge badge--blue" style={{ padding: '7px 14px' }}>
            Historia {selected.file_number}
          </span>
        )}
      </div>

      <Card>
        {!patientId ? (
          <EmptyState title="Selecciona un paciente" hint="Elige un paciente para ver o registrar atenciones." />
        ) : loading ? (
          <Loading label="Cargando historial…" />
        ) : records.length === 0 ? (
          <EmptyState title="Sin consultas registradas" hint="Registra la primera consulta del paciente." />
        ) : (
          <div className="mini-list">
            {records
              .slice()
              .sort((a, b) => b.visit_date.localeCompare(a.visit_date))
              .map((r) => (
                <div className="mini-item" key={r.id}>
                  <div className="mini-item__icon">
                    <Icon name="stethoscope" size={17} />
                  </div>
                  <div className="mini-item__meta">
                    <strong>{r.diagnosis || 'Consulta médica'}</strong>
                    <span>
                      {r.visit_date ? r.visit_date.replace('T', ' ').slice(0, 16) : 'Sin fecha'} ·{' '}
                      {r.doctor?.full_name ?? 'Médico'}
                      {r.treatment ? ` · Tratamiento: ${r.treatment}` : ''}
                    </span>
                    {r.notes && (
                      <span style={{ display: 'block', marginTop: 3 }}>{r.notes}</span>
                    )}
                  </div>
                  <div className="mini-item__end">
                    <Badge label="atendida" />
                  </div>
                </div>
              ))}
          </div>
        )}
      </Card>

      {showForm && (
        <Modal
          title={`Nueva consulta${selected ? ` — ${selected.first_name} ${selected.last_name}` : ''}`}
          onClose={() => setShowForm(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowForm(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="clinical-form" disabled={saving}>
                {saving ? 'Guardando…' : 'Guardar consulta'}
              </button>
            </>
          }
        >
          <form id="clinical-form" onSubmit={create} className="form-grid">
            <Field label="Fecha de la consulta">
              <input className="input" type="date" value={form.visit_date} onChange={set('visit_date')} />
            </Field>
            <Field label="Signos vitales">
              <input className="input" value={form.vitals} onChange={set('vitals')} placeholder="TA 120/80 · FC 72" />
            </Field>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Diagnóstico">
                <input className="input" value={form.diagnosis} onChange={set('diagnosis')} placeholder="Diagnóstico principal…" />
              </Field>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Tratamiento">
                <input className="input" value={form.treatment} onChange={set('treatment')} placeholder="Indicaciones y medicación…" />
              </Field>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Notas">
                <textarea className="textarea" rows={3} value={form.notes} onChange={set('notes')} />
              </Field>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
