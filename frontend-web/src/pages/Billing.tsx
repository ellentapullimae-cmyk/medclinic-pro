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
interface InvoiceItem {
  id: number;
  concept: string;
  quantity: number;
  unit_price: string;
  subtotal: string;
}
interface Payment {
  id: number;
  amount: string;
  method: string;
  paid_at: string;
}
interface Invoice {
  id: number;
  number: string;
  patient_id: number;
  issue_date: string;
  status: string;
  total: string;
  patient?: Patient;
  items: InvoiceItem[];
  payments: Payment[];
}

const STATUS_FILTERS = ['', 'pendiente', 'pagada', 'anulada'];
const METHODS = ['efectivo', 'tarjeta', 'transferencia', 'seguro'];

const EMPTY_INVOICE = { patient_id: '', concept: '', quantity: '1', unit_price: '' };

export default function Billing() {
  const { user: me } = useAuth();
  const canCreatePay = me?.role === 'admin' || me?.role === 'recepcion';
  const canCancel = me?.role === 'admin';
  const [list, setList] = useState<Invoice[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [status, setStatus] = useState('');
  const [invoiceForm, setInvoiceForm] = useState({ ...EMPTY_INVOICE });
  const [payTarget, setPayTarget] = useState<Invoice | null>(null);
  const [payForm, setPayForm] = useState({ amount: '', method: 'efectivo' });
  const [showInvoice, setShowInvoice] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback((s: string) => {
    setLoading(true);
    api
      .get('/api/v1/invoices', { params: { status: s || undefined, page_size: 100 } })
      .then((r) => {
        setList(r.data.items ?? []);
        setError('');
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar las facturas.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(status);
    api.get('/api/v1/patients', { params: { page_size: 100 } }).then((r) => setPatients(r.data.items ?? [])).catch(() => {});
  }, [status, load]);

  const createInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post('/api/v1/invoices', {
        patient_id: Number(invoiceForm.patient_id),
        items: [
          {
            concept: invoiceForm.concept,
            quantity: Number(invoiceForm.quantity) || 1,
            unit_price: invoiceForm.unit_price,
          },
        ],
      });
      setNotice('Factura emitida correctamente.');
      setShowInvoice(false);
      setInvoiceForm({ ...EMPTY_INVOICE });
      load(status);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo emitir la factura.');
    } finally {
      setSaving(false);
    }
  };

  const openPay = (inv: Invoice) => {
    const total = Number(inv.total) || 0;
    const paid = inv.payments.reduce((s, p) => s + (Number(p.amount) || 0), 0);
    setPayTarget(inv);
    setPayForm({ amount: (total - paid).toFixed(2), method: 'efectivo' });
  };

  const pay = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!payTarget) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post(`/api/v1/invoices/${payTarget.id}/payments`, {
        amount: payForm.amount,
        method: payForm.method,
      });
      setNotice('Pago registrado correctamente.');
      setPayTarget(null);
      load(status);
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setError(typeof detail === 'string' ? detail : 'No se pudo registrar el pago.');
    } finally {
      setSaving(false);
    }
  };

  const cancelInvoice = async (inv: Invoice) => {
    if (!confirm(`¿Anular la factura ${inv.number}?`)) return;
    try {
      await api.post(`/api/v1/invoices/${inv.id}/cancel`);
      setNotice('Factura anulada.');
      load(status);
    } catch {
      setError('No se pudo anular la factura.');
    }
  };

  const pendingOf = (inv: Invoice) => {
    const total = Number(inv.total) || 0;
    const paid = inv.payments.reduce((s, p) => s + (Number(p.amount) || 0), 0);
    return total - paid;
  };

  const money = (n: number) => `$${n.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  const setInv = (key: keyof typeof EMPTY_INVOICE) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setInvoiceForm({ ...invoiceForm, [key]: e.target.value });

  return (
    <>
      <PageHead
        title="Pagos y facturas"
        subtitle={`${list.length} facturas en la vista actual`}
        actions={
          canCreatePay ? (
            <button className="btn btn--primary" onClick={() => setShowInvoice(true)}>
              <Icon name="plus" size={16} />
              Nueva factura
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
          <Loading label="Cargando facturas…" />
        ) : list.length === 0 ? (
          <EmptyState title="Sin facturas" hint="Emite la primera factura para empezar a cobrar." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Comprobante</th>
                  <th>Paciente</th>
                  <th>Fecha</th>
                  <th>Total</th>
                  <th>Saldo</th>
                  <th>Estado</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {list.map((inv) => {
                  const pending = pendingOf(inv);
                  return (
                    <tr key={inv.id}>
                      <td className="cell-strong">{inv.number}</td>
                      <td>
                        {inv.patient ? `${inv.patient.last_name}, ${inv.patient.first_name}` : `#${inv.patient_id}`}
                        <div className="cell-muted" style={{ fontSize: 12 }}>
                          {inv.items.map((i) => i.concept).join(' · ') || '—'}
                        </div>
                      </td>
                      <td className="cell-muted">{inv.issue_date.slice(0, 10)}</td>
                      <td className="cell-strong">{money(Number(inv.total) || 0)}</td>
                      <td className={pending > 0 ? 'cell-strong' : 'cell-muted'}>
                        {inv.status === 'pendiente' ? money(pending) : '—'}
                      </td>
                      <td>
                        <Badge label={inv.status} />
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                          {inv.status === 'pendiente' && (
                            <>
                              {canCreatePay && (
                                <button className="btn btn--soft btn--sm" onClick={() => openPay(inv)}>
                                  Cobrar
                                </button>
                              )}
                              {canCancel && (
                                <button className="btn btn--danger-soft btn--sm" onClick={() => cancelInvoice(inv)}>
                                  Anular
                                </button>
                              )}
                            </>
                          )}
                          {inv.payments.length > 0 && (
                            <span className="cell-muted" style={{ fontSize: 12 }}>
                              {inv.payments.length} pago{inv.payments.length > 1 ? 's' : ''}
                            </span>
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

      {showInvoice && (
        <Modal
          title="Nueva factura"
          onClose={() => setShowInvoice(false)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setShowInvoice(false)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="invoice-form" disabled={saving}>
                {saving ? 'Emitiendo…' : 'Emitir factura'}
              </button>
            </>
          }
        >
          <form id="invoice-form" onSubmit={createInvoice} className="form-grid">
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Paciente" required>
                <select className="select" value={invoiceForm.patient_id} onChange={setInv('patient_id')} required>
                  <option value="">Seleccionar…</option>
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.last_name}, {p.first_name}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Concepto" required>
                <input
                  className="input"
                  value={invoiceForm.concept}
                  onChange={setInv('concept')}
                  placeholder="Consulta médica, estudio…"
                  required
                />
              </Field>
            </div>
            <Field label="Cantidad" required>
              <input className="input" type="number" min="1" value={invoiceForm.quantity} onChange={setInv('quantity')} required />
            </Field>
            <Field label="Precio unitario" required>
              <input
                className="input"
                type="number"
                step="0.01"
                min="0"
                value={invoiceForm.unit_price}
                onChange={setInv('unit_price')}
                placeholder="0.00"
                required
              />
            </Field>
          </form>
        </Modal>
      )}

      {payTarget && (
        <Modal
          title={`Cobrar ${payTarget.number}`}
          onClose={() => setPayTarget(null)}
          footer={
            <>
              <button className="btn btn--ghost" onClick={() => setPayTarget(null)} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn--primary" form="pay-form" disabled={saving}>
                {saving ? 'Registrando…' : 'Registrar pago'}
              </button>
            </>
          }
        >
          <form id="pay-form" onSubmit={pay} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="detail-grid">
              <div className="detail-item">
                <span>Total</span>
                <strong>{money(Number(payTarget.total) || 0)}</strong>
              </div>
              <div className="detail-item">
                <span>Saldo pendiente</span>
                <strong>{money(pendingOf(payTarget))}</strong>
              </div>
            </div>
            <Field label="Importe a cobrar" required>
              <input
                className="input"
                type="number"
                step="0.01"
                min="0.01"
                value={payForm.amount}
                onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })}
                required
              />
            </Field>
            <Field label="Método de pago" required>
              <select
                className="select"
                value={payForm.method}
                onChange={(e) => setPayForm({ ...payForm, method: e.target.value })}
              >
                {METHODS.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </Field>
          </form>
        </Modal>
      )}
    </>
  );
}
