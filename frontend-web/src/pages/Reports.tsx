import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api, apiErrorText } from '../api/client';
import { Card, CardHead, EmptyState, ErrorBox, Loading, PageHead, StatCard } from '../components/ui';

interface Patient {
  id: number;
  is_active: boolean;
  insurance?: string;
}
interface Appointment {
  id: number;
  status: string;
  start_time: string;
}
interface Invoice {
  id: number;
  status: string;
  total: string;
  payments: { amount: string; method: string }[];
}
interface Item {
  id: number;
  stock: number;
  min_stock: number;
  low_stock: boolean;
  unit_cost: string;
}

const METHOD_LABELS: Record<string, string> = {
  efectivo: 'Efectivo',
  tarjeta: 'Tarjeta',
  transferencia: 'Transferencia',
  seguro: 'Seguro',
};

export default function Reports() {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/api/v1/patients', { params: { page_size: 200 } }),
      api.get('/api/v1/appointments', { params: { page_size: 200 } }),
      api.get('/api/v1/invoices', { params: { page_size: 200 } }),
      api.get('/api/v1/inventory', { params: { page_size: 200 } }),
    ])
      .then(([p, a, i, inv]) => {
        setPatients(p.data.items ?? []);
        setAppointments(a.data.items ?? []);
        setInvoices(i.data.items ?? []);
        setItems(inv.data.items ?? []);
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron generar los reportes.')))
      .finally(() => setLoading(false));
  }, []);

  const collected = invoices.reduce(
    (sum, inv) => sum + inv.payments.reduce((s, p) => s + (Number(p.amount) || 0), 0),
    0,
  );
  const receivable = invoices
    .filter((i) => i.status === 'pendiente')
    .reduce((sum, inv) => sum + (Number(inv.total) || 0), 0);
  const inventoryValue = items.reduce((s, it) => s + (Number(it.unit_cost) || 0) * it.stock, 0);
  const lowStock = items.filter((i) => i.low_stock).length;

  const apptByStatus = ['pendiente', 'confirmada', 'atendida', 'cancelada'].map((status) => ({
    name: status,
    cantidad: appointments.filter((a) => a.status === status).length,
  }));

  const revenueByMethod = Object.keys(METHOD_LABELS).map((method) => ({
    name: METHOD_LABELS[method],
    monto: invoices.reduce(
      (sum, inv) =>
        sum + inv.payments.filter((p) => p.method === method).reduce((s, p) => s + (Number(p.amount) || 0), 0),
      0,
    ),
  }));

  const byMonth = (() => {
    const map = new Map<string, number>();
    appointments.forEach((a) => {
      const key = a.start_time.slice(0, 7);
      if (key) map.set(key, (map.get(key) ?? 0) + 1);
    });
    return Array.from(map)
      .sort(([x], [y]) => x.localeCompare(y))
      .slice(-8)
      .map(([name, cantidad]) => ({ name, cantidad }));
  })();

  const insuranceTop = (() => {
    const map = new Map<string, number>();
    patients.forEach((p) => {
      const key = p.insurance?.trim();
      if (key) map.set(key, (map.get(key) ?? 0) + 1);
    });
    return Array.from(map)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([name, value]) => ({ name, value }));
  })();

  const money = (n: number) => `$${n.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  if (loading) return <Loading label="Generando reportes…" />;

  const STATUS_COLORS: Record<string, string> = {
    pendiente: '#F59E0B',
    confirmada: '#0878F9',
    atendida: '#10B981',
    cancelada: '#EF4444',
  };

  return (
    <>
      <PageHead title="Reportes" subtitle="Indicadores calculados con datos reales del servidor" />

      {error && <ErrorBox message={error} />}

      <div className="stat-grid">
        <StatCard label="Ingresos cobrados" value={money(collected)} hint="Pagos registrados" icon="trend" tone="green" index={0} />
        <StatCard label="Por cobrar" value={money(receivable)} hint="Facturas pendientes" icon="wallet" tone="amber" index={1} />
        <StatCard label="Valor de inventario" value={money(inventoryValue)} hint={`${lowStock} ítems con stock bajo`} icon="box" tone="violet" index={2} />
        <StatCard
          label="Tasa de atención"
          value={
            appointments.length
              ? `${Math.round((appointments.filter((a) => a.status === 'atendida').length / appointments.length) * 100)}%`
              : '—'
          }
          hint="Citas atendidas / total"
          icon="check"
          tone="blue"
          index={3}
        />
      </div>

      <div className="grid-equal">
        <Card>
          <CardHead title="Citas por estado" subtitle="Distribución de la agenda" />
          <div className="card__body">
            {appointments.length === 0 ? (
              <EmptyState title="Sin datos de citas" />
            ) : (
              <div className="chart-box">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={apptByStatus} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E2EAF4" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#7b8494' }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 12, fill: '#7b8494' }} axisLine={false} tickLine={false} allowDecimals={false} />
                    <Tooltip
                      cursor={{ fill: 'rgba(8,120,249,.10)' }}
                      contentStyle={{ borderRadius: 12, border: '1px solid #E2EAF4', fontSize: 13 }}
                    />
                    <Bar dataKey="cantidad" radius={[8, 8, 0, 0]} maxBarSize={52}>
                      {apptByStatus.map((s) => (
                        <Cell key={s.name} fill={STATUS_COLORS[s.name] ?? '#0878F9'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </Card>

        <Card>
          <CardHead title="Ingresos por método de pago" subtitle="Suma de pagos registrados" />
          <div className="card__body">
            {revenueByMethod.every((r) => r.monto === 0) ? (
              <EmptyState title="Sin pagos registrados" hint="Los cobros aparecerán aquí." />
            ) : (
              <div className="chart-box">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={revenueByMethod} margin={{ top: 8, right: 8, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E2EAF4" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#7b8494' }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 11, fill: '#7b8494' }} axisLine={false} tickLine={false} />
                    <Tooltip
                      cursor={{ fill: 'rgba(22,163,74,.06)' }}
                      contentStyle={{ borderRadius: 12, border: '1px solid #E2EAF4', fontSize: 13 }}
                      formatter={(value) => [money(Number(value ?? 0)), 'Monto']}
                    />
                    <Bar dataKey="monto" fill="#10B981" radius={[8, 8, 0, 0]} maxBarSize={52} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </Card>
      </div>

      <div className="grid-equal">
        <Card>
          <CardHead title="Citas por mes" subtitle="Volumen de atención" />
          <div className="card__body">
            {byMonth.length === 0 ? (
              <EmptyState title="Sin histórico de citas" />
            ) : (
              <div className="chart-box">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={byMonth} margin={{ top: 8, right: 8, left: -22, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E2EAF4" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#7b8494' }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 12, fill: '#7b8494' }} axisLine={false} tickLine={false} allowDecimals={false} />
                    <Tooltip
                      cursor={{ fill: 'rgba(8,120,249,.10)' }}
                      contentStyle={{ borderRadius: 12, border: '1px solid #E2EAF4', fontSize: 13 }}
                    />
                    <Bar dataKey="cantidad" fill="#0878F9" radius={[8, 8, 0, 0]} maxBarSize={44} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </Card>

        <Card>
          <CardHead title="Cobertura de obra social" subtitle="Pacientes con obra social declarada" />
          <div className="card__body">
            {insuranceTop.length === 0 ? (
              <EmptyState title="Sin datos de cobertura" hint="Completa el campo obra social en las fichas." />
            ) : (
              <>
                <div className="chart-box" style={{ height: 236 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={insuranceTop} dataKey="value" nameKey="name" innerRadius={54} outerRadius={90} paddingAngle={4} stroke="none">
                        {insuranceTop.map((_, idx) => (
                          <Cell
                            key={idx}
                            fill={['#0878F9', '#10B981', '#F59E0B', '#7C3AED', '#16C7F5', '#EF4444'][idx % 6]}
                          />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #E2EAF4', fontSize: 13 }} />
                      <Legend
                        verticalAlign="bottom"
                        formatter={(value: string) => (
                          <span style={{ fontSize: 12, color: '#7b8494' }}>{value}</span>
                        )}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </>
            )}
          </div>
        </Card>
      </div>
    </>
  );
}
