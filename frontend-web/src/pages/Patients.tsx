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
  dni: string;
  phone?: string;
  email?: string;
  birth_date?: string;
  sex?: string;
  address?: string;
  insurance?: string;
  allergies?: string;
  notes?: string;
  is_active: boolean;
}

const EMPTY_FORM = {
  first_name: '',
  last_name: '',
  dni: '',
  phone: '',
  email: '',
  birth_date: '',
  sex: '',
  address: '',
  insurance: '',
  allergies: '',
  notes: '',
};

export default function Patients() {
  const { user: me } = useAuth();
  const canCreate = me?.role === 'admin' || me?.role === 'recepcion';
  const canDeactivate = me?.role === 'admin';
  const [list, setList] = useState<Patient[]>([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback((q: string) => {
    setLoading(true);
    api
      .get('/api/v1/patients', { params: { q: q || undefined, page_size: 100 } })
      .then((r) => {
        setList(r.data.items ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar los pacientes.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load('');
  }, [load]);

  useEffect(() => {
    const t = setTimeout(() => load(query), 320);
    return () => clearTimeout(t);
  }, [query, load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setNotice('');
    setError('');
    try {
      await api.post('/api/v1/patients', {
        ...form,
        birth_date: form.birth_date || null,
        sex: form.sex || null,
        phone: form.phone || null,
        email: form.email || null,
        address: form.address || null,
        insurance: form.insurance || null,
        allergies: form.allergies || null,
        notes: form.notes || null,
      });
      setNotice('Paciente registrado correctamente.');
      setShowForm(false);
      setForm({ ...EMPTY_FORM });
      load(query);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo crear el paciente.');
    } finally {
      setSaving(false);
    }
  };

  const deactivate = async (p: Patient) => {
    if (!confirm(`¿Dar de baja a ${p.first_name} ${p.last_name}?`)) return;
    try {
      await api.delete(`/api/v1/patients/${p.id}`);
      setNotice('Paciente dado de baja.');
      load(query);
    } catch {
      setError('No se pudo dar de baja al paciente.');
    }
  };

  const set = (key: keyof typeof EMPTY_FORM) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [key]: e.target.value });

  return (
    <>
      <PageHead
        title="Pacientes"
        subtitle={`${list.length} fichas encontradas`}
        actions={
          canCreate ? (
            <button className="btn btn--primary" onClick={() => setShowForm(true)}>
              <Icon name="plus" size={16} />
              Nuevo paciente
            </button>
          ) : null
        }
      />

      {error && <ErrorBox message={error} />}
      {notice && <SuccessBox message={notice} />}

      <div className="toolbar">
        <div className="search">
          <Icon name="search" size={17} />
          <input
            className="input"
            placeholder="Buscar por nombre, DNI o historia…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      <Card>
        {loading ? (
          <Loading label="Buscando pacientes…" />
        ) : list.length === 0 ? (
          <EmptyState title="Sin resultados" hint="Ajusta la búsqueda o registra un paciente nuevo." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Historia</th>
                  <th>Paciente</th>
                  <th>DNI</th>
                  <th>Teléfono</th>
                  <th>Obra social</th>
                  <th>Estado</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {list.map((p) => (
                  <tr key={p.id}>
                    <td className="cell-muted">{p.file_number}</td>
                    <td className="cell-strong">
                      {p.last_name}, {p.first_name}
                    </td>
                    <td className="cell-muted">{p.dni}</td>
                    <td className="cell-muted">{p.phone || '—'}</td>
                    <td className="cell-muted">{p.insurance || '—'}</td>
                    <td>
                      <Badge label={p.is_active ? 'activo' : 'inactivo'} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {p.is_active && canDeactivate && (
                        <button className="btn btn--danger-soft btn--sm" onClick={() => deactivate(p)}>
                          Dar de baja
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {showForm && (
        <Modal
          title="Nuevo paciente"
          onClose={() => setShowForm(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowForm(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="patient-form" disabled={saving}>
                {saving ? 'Guardando…' : 'Registrar'}
              </button>
            </>
          }
        >
          <form id="patient-form" onSubmit={create} className="form-grid">
            <Field label="Nombre" required>
              <input className="input" value={form.first_name} onChange={set('first_name')} required />
            </Field>
            <Field label="Apellido" required>
              <input className="input" value={form.last_name} onChange={set('last_name')} required />
            </Field>
            <Field label="DNI" required>
              <input className="input" value={form.dni} onChange={set('dni')} required />
            </Field>
            <Field label="Fecha de nacimiento">
              <input className="input" type="date" value={form.birth_date} onChange={set('birth_date')} />
            </Field>
            <Field label="Sexo">
              <select className="select" value={form.sex} onChange={set('sex')}>
                <option value="">Sin especificar</option>
                <option value="M">Masculino</option>
                <option value="F">Femenino</option>
                <option value="O">Otro</option>
              </select>
            </Field>
            <Field label="Teléfono">
              <input className="input" value={form.phone} onChange={set('phone')} />
            </Field>
            <Field label="Email">
              <input className="input" type="email" value={form.email} onChange={set('email')} />
            </Field>
            <Field label="Obra social">
              <input className="input" value={form.insurance} onChange={set('insurance')} />
            </Field>
            <Field label="Dirección">
              <input className="input" value={form.address} onChange={set('address')} />
            </Field>
            <Field label="Alergias">
              <input className="input" value={form.allergies} onChange={set('allergies')} placeholder="Ninguna" />
            </Field>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Notas">
                <textarea className="textarea" rows={2} value={form.notes} onChange={set('notes')} />
              </Field>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
