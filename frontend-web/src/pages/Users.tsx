import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/Icons';
import { Badge, Card, EmptyState, ErrorBox, Field, Loading, Modal, PageHead, SuccessBox } from '../components/ui';

interface Doctor {
  id: number;
  full_name: string;
  specialty?: string;
}
interface UserRow {
  id: number;
  email: string;
  full_name: string;
  role: string;
  specialty?: string;
  is_active: boolean;
}

const EMPTY_USER = { email: '', password: '', full_name: '', role: 'recepcion', specialty: '' };

export default function Users() {
  const { user: me } = useAuth();
  const isAdmin = me?.role === 'admin';

  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ ...EMPTY_USER });
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    const doctorsReq = api.get('/api/v1/users/doctors');
    const usersReq = isAdmin ? api.get('/api/v1/users') : Promise.resolve(null);

    Promise.all([doctorsReq, usersReq])
      .then(([d, u]) => {
        setDoctors(d.data ?? []);
        setUsers(u?.data ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar los usuarios.')))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  useEffect(() => {
    load();
  }, [load]);

  const createUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post('/api/v1/users', {
        email: form.email,
        password: form.password,
        full_name: form.full_name,
        role: form.role,
        specialty: form.specialty || null,
      });
      setNotice('Usuario creado correctamente.');
      setShowCreate(false);
      setForm({ ...EMPTY_USER });
      load();
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo crear el usuario.');
    } finally {
      setSaving(false);
    }
  };

  const updateRole = async (u: UserRow, role: string) => {
    setError('');
    try {
      await api.patch(`/api/v1/users/${u.id}`, { role });
      setNotice(`Rol de ${u.full_name} actualizado.`);
      load();
    } catch {
      setError('No se pudo actualizar el rol.');
    }
  };

  const toggleActive = async (u: UserRow) => {
    setError('');
    try {
      await api.patch(`/api/v1/users/${u.id}`, { is_active: !u.is_active });
      setNotice(u.is_active ? 'Usuario desactivado.' : 'Usuario activado.');
      load();
    } catch {
      setError('No se pudo actualizar el estado.');
    }
  };

  const set = (key: keyof typeof EMPTY_USER) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm({ ...form, [key]: e.target.value });

  const specialties = Array.from(new Set(doctors.map((d) => d.specialty).filter(Boolean))) as string[];

  return (
    <>
      <PageHead
        title="Médicos y usuarios"
        subtitle="Equipo de atención y permisos de acceso"
        actions={
          isAdmin && (
            <button className="btn btn--primary" onClick={() => setShowCreate(true)}>
              <Icon name="plus" size={16} />
              Nuevo usuario
            </button>
          )
        }
      />

      {error && <ErrorBox message={error} />}
      {notice && <SuccessBox message={notice} />}

      {loading ? (
        <Loading label="Cargando equipo…" />
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat">
              <div className="stat__icon stat__icon--blue">
                <Icon name="stethoscope" size={22} />
              </div>
              <div>
                <div className="stat__label">Médicos activos</div>
                <div className="stat__value">{doctors.length}</div>
              </div>
            </div>
            <div className="stat">
              <div className="stat__icon stat__icon--violet">
                <Icon name="users" size={22} />
              </div>
              <div>
                <div className="stat__label">Especialidades</div>
                <div className="stat__value">{specialties.length}</div>
              </div>
            </div>
            {isAdmin && (
              <div className="stat">
                <div className="stat__icon stat__icon--green">
                  <Icon name="shield" size={22} />
                </div>
                <div>
                  <div className="stat__label">Usuarios con acceso</div>
                  <div className="stat__value">{users.length}</div>
                </div>
              </div>
            )}
          </div>

          <Card>
            <div className="card__head">
              <div>
                <h3>Médicos</h3>
                <span>Profesionales disponibles para agendar citas</span>
              </div>
            </div>
            {doctors.length === 0 ? (
              <EmptyState title="Sin médicos registrados" />
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Nombre</th>
                      <th>Especialidad</th>
                      <th>ID</th>
                    </tr>
                  </thead>
                  <tbody>
                    {doctors.map((d) => (
                      <tr key={d.id}>
                        <td className="cell-strong">{d.full_name}</td>
                        <td className="cell-muted">{d.specialty || '—'}</td>
                        <td className="cell-muted">#{d.id}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <div style={{ height: 18 }} />

          <Card>
            <div className="card__head">
              <div>
                <h3>Usuarios y permisos</h3>
                <span>{isAdmin ? 'Roles administrables por el usuario admin' : 'Requiere rol administrador'}</span>
              </div>
            </div>
            {!isAdmin ? (
              <EmptyState title="Sin permisos" hint="Solo un administrador puede gestionar usuarios y roles." />
            ) : users.length === 0 ? (
              <EmptyState title="Sin usuarios" />
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Usuario</th>
                      <th>Correo</th>
                      <th>Especialidad</th>
                      <th>Rol</th>
                      <th>Estado</th>
                      <th style={{ textAlign: 'right' }}>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id}>
                        <td className="cell-strong">{u.full_name}</td>
                        <td className="cell-muted">{u.email}</td>
                        <td className="cell-muted">{u.specialty || '—'}</td>
                        <td>
                          <Badge label={u.role} />
                        </td>
                        <td>
                          <Badge label={u.is_active ? 'activo' : 'inactivo'} />
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                            <select
                              className="select"
                              style={{ width: 140, padding: '7px 30px 7px 12px', fontSize: 12.5 }}
                              value={u.role}
                              onChange={(e) => updateRole(u, e.target.value)}
                              disabled={u.id === me?.id}
                              title={u.id === me?.id ? 'No puedes cambiar tu propio rol' : 'Cambiar rol'}
                            >
                              <option value="admin">admin</option>
                              <option value="medico">medico</option>
                              <option value="recepcion">recepcion</option>
                            </select>
                            <button
                              className={`btn btn--sm ${u.is_active ? 'btn--danger-soft' : 'btn--soft'}`}
                              onClick={() => toggleActive(u)}
                              disabled={u.id === me?.id}
                            >
                              {u.is_active ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}

      {showCreate && (
        <Modal
          title="Nuevo usuario"
          onClose={() => setShowCreate(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowCreate(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="user-form" disabled={saving}>
                {saving ? 'Creando…' : 'Crear usuario'}
              </button>
            </>
          }
        >
          <form id="user-form" onSubmit={createUser} className="form-grid">
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Nombre completo" required>
                <input className="input" value={form.full_name} onChange={set('full_name')} required />
              </Field>
            </div>
            <Field label="Correo" required>
              <input className="input" type="email" value={form.email} onChange={set('email')} required />
            </Field>
            <Field label="Contraseña" required>
              <input className="input" type="password" value={form.password} onChange={set('password')} minLength={6} required />
            </Field>
            <Field label="Rol" required>
              <select className="select" value={form.role} onChange={set('role')}>
                <option value="recepcion">Recepción</option>
                <option value="medico">Médico</option>
                <option value="admin">Administrador</option>
              </select>
            </Field>
            <Field label="Especialidad">
              <input
                className="input"
                value={form.specialty}
                onChange={set('specialty')}
                placeholder="Cardiología…"
                list="specialty-list"
              />
              <datalist id="specialty-list">
                {specialties.map((s) => (
                  <option key={s} value={s} />
                ))}
              </datalist>
            </Field>
          </form>
        </Modal>
      )}
    </>
  );
}
