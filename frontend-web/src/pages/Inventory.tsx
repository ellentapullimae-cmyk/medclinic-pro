import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/Icons';
import { Badge, Card, EmptyState, ErrorBox, Field, Loading, Modal, PageHead, SuccessBox } from '../components/ui';

interface Item {
  id: number;
  sku: string;
  name: string;
  category: string;
  unit: string;
  stock: number;
  min_stock: number;
  low_stock: boolean;
  unit_cost: string;
  expiry_date?: string;
  is_active: boolean;
}

const EMPTY_FORM = { sku: '', name: '', category: 'medicamento', unit: 'unidad', stock: '0', min_stock: '0', unit_cost: '0.00' };

export default function Inventory() {
  const { user: me } = useAuth();
  const canCreate = me?.role === 'admin';
  const canMove = me?.role === 'admin' || me?.role === 'medico' || me?.role === 'recepcion';
  const [list, setList] = useState<Item[]>([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [movement, setMovement] = useState<Item | null>(null);
  const [moveForm, setMoveForm] = useState({ change: '1', reason: '', entry: true });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback((q: string) => {
    setLoading(true);
    api
      .get('/api/v1/inventory', { params: { q: q || undefined, page_size: 100 } })
      .then((r) => {
        setList(r.data.items ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudo cargar el inventario.')))
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
    setError('');
    setNotice('');
    try {
      await api.post('/api/v1/inventory', {
        ...form,
        stock: Number(form.stock) || 0,
        min_stock: Number(form.min_stock) || 0,
      });
      setNotice('Ítem creado correctamente.');
      setShowForm(false);
      setForm({ ...EMPTY_FORM });
      load(query);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo crear el ítem.');
    } finally {
      setSaving(false);
    }
  };

  const registerMovement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!movement) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const raw = Number(moveForm.change) || 0;
      const change = moveForm.entry ? Math.abs(raw) : -Math.abs(raw);
      await api.post(`/api/v1/inventory/${movement.id}/movements`, {
        change,
        reason: moveForm.reason || null,
      });
      setNotice('Movimiento de stock registrado.');
      setMovement(null);
      setMoveForm({ change: '1', reason: '', entry: true });
      load(query);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo registrar el movimiento.');
    } finally {
      setSaving(false);
    }
  };

  const set = (key: keyof typeof EMPTY_FORM) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm({ ...form, [key]: e.target.value });

  const lowCount = list.filter((i) => i.low_stock).length;

  return (
    <>
      <PageHead
        title="Inventario"
        subtitle={`${list.length} ítems · ${lowCount} con stock bajo`}
        actions={
          canCreate ? (
            <button className="btn btn--primary" onClick={() => setShowForm(true)}>
              <Icon name="plus" size={16} />
              Nuevo ítem
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
            placeholder="Buscar por nombre o SKU…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      <Card>
        {loading ? (
          <Loading label="Cargando inventario…" />
        ) : list.length === 0 ? (
          <EmptyState title="Sin ítems" hint="Registra el primer insumo o medicamento." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Ítem</th>
                  <th>Categoría</th>
                  <th>Stock</th>
                  <th>Mínimo</th>
                  <th>Costo</th>
                  <th>Estado</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id}>
                    <td className="cell-muted">{item.sku}</td>
                    <td className="cell-strong">{item.name}</td>
                    <td className="cell-muted" style={{ textTransform: 'capitalize' }}>
                      {item.category}
                    </td>
                    <td className="cell-strong">
                      {item.stock} <span className="cell-muted">{item.unit}</span>
                    </td>
                    <td className="cell-muted">{item.min_stock}</td>
                    <td className="cell-muted">${item.unit_cost}</td>
                    <td>
                      {item.low_stock ? <Badge label="Stock bajo" tone="red" /> : <Badge label="Disponible" tone="green" />}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {canMove && (
                        <button className="btn btn--soft btn--sm" onClick={() => setMovement(item)}>
                          Movimiento
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
          title="Nuevo ítem de inventario"
          onClose={() => setShowForm(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowForm(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="item-form" disabled={saving}>
                {saving ? 'Creando…' : 'Crear ítem'}
              </button>
            </>
          }
        >
          <form id="item-form" onSubmit={create} className="form-grid">
            <Field label="SKU" required>
              <input className="input" value={form.sku} onChange={set('sku')} placeholder="MED-001" required />
            </Field>
            <Field label="Nombre" required>
              <input className="input" value={form.name} onChange={set('name')} required />
            </Field>
            <Field label="Categoría">
              <select className="select" value={form.category} onChange={set('category')}>
                <option value="medicamento">Medicamento</option>
                <option value="insumo">Insumo</option>
              </select>
            </Field>
            <Field label="Unidad">
              <input className="input" value={form.unit} onChange={set('unit')} />
            </Field>
            <Field label="Stock inicial">
              <input className="input" type="number" min="0" value={form.stock} onChange={set('stock')} />
            </Field>
            <Field label="Stock mínimo">
              <input className="input" type="number" min="0" value={form.min_stock} onChange={set('min_stock')} />
            </Field>
            <Field label="Costo unitario">
              <input className="input" type="number" step="0.01" min="0" value={form.unit_cost} onChange={set('unit_cost')} />
            </Field>
          </form>
        </Modal>
      )}

      {movement && (
        <Modal
          title={`Movimiento — ${movement.name}`}
          onClose={() => setMovement(null)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setMovement(null)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="move-form" disabled={saving}>
                {saving ? 'Registrando…' : 'Registrar'}
              </button>
            </>
          }
        >
          <form id="move-form" onSubmit={registerMovement} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="detail-grid">
              <div className="detail-item">
                <span>Stock actual</span>
                <strong>
                  {movement.stock} {movement.unit}
                </strong>
              </div>
              <div className="detail-item">
                <span>Stock mínimo</span>
                <strong>{movement.min_stock}</strong>
              </div>
            </div>

            <Field label="Tipo de movimiento" required>
              <div className="chips">
                <button
                  type="button"
                  className={`chip ${moveForm.entry ? 'active' : ''}`}
                  onClick={() => setMoveForm({ ...moveForm, entry: true })}
                >
                  Entrada
                </button>
                <button
                  type="button"
                  className={`chip ${!moveForm.entry ? 'active' : ''}`}
                  onClick={() => setMoveForm({ ...moveForm, entry: false })}
                >
                  Salida
                </button>
              </div>
            </Field>

            <Field label="Cantidad" required>
              <input
                className="input"
                type="number"
                min="1"
                value={moveForm.change}
                onChange={(e) => setMoveForm({ ...moveForm, change: e.target.value })}
                required
              />
            </Field>

            <Field label="Motivo">
              <input
                className="input"
                value={moveForm.reason}
                onChange={(e) => setMoveForm({ ...moveForm, reason: e.target.value })}
                placeholder="Compra, uso en consulta…"
              />
            </Field>
          </form>
        </Modal>
      )}
    </>
  );
}
