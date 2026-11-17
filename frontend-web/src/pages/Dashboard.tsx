import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import {
  Activity,
  ArrowRight,
  CalendarCheck,
  CalendarDays,
  CreditCard,
  FileText,
  HeartPulse,
  Package,
  Sparkles,
  Stethoscope,
  TrendingUp,
  UserPlus,
  Users,
} from 'lucide-react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Badge, Card, CardHead, EmptyState, ErrorBox, Loading } from '../components/ui';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

interface Patient {
  id: number;
  first_name: string;
  last_name: string;
  is_active: boolean;
}
interface Appointment {
  id: number;
  patient_id: number;
  doctor_id: number;
  start_time: string;
  status: string;
  reason?: string;
  patient?: { first_name: string; last_name: string };
  doctor?: { full_name: string };
}
interface Invoice {
  id: number;
  number: string;
  status: string;
  total: string;
  issue_date: string;
  payments: { amount: string }[];
  patient?: { first_name: string; last_name: string };
}

const STATUS_COLORS: Record<string, string> = {
  pendiente: '#F59E0B',
  confirmada: '#0878F9',
  atendida: '#10B981',
  cancelada: '#EF4444',
};

const STATUS_ORDER = ['pendiente', 'confirmada', 'atendida', 'cancelada'];
const STATUS_LABEL: Record<string, string> = {
  pendiente: 'Pendiente',
  confirmada: 'Confirmada',
  atendida: 'Atendida',
  cancelada: 'Cancelada',
};

const QUICK_LINKS = [
  { to: '/patients', label: 'Pacientes', icon: Users, tone: '' },
  { to: '/appointments', label: 'Nueva cita', icon: CalendarDays, tone: 'cyan' },
  { to: '/clinical', label: 'Historial', icon: HeartPulse, tone: 'green' },
  { to: '/billing', label: 'Facturación', icon: CreditCard, tone: 'amber' },
  { to: '/doctors', label: 'Médicos', icon: Stethoscope, tone: 'violet' },
  { to: '/inventory', label: 'Inventario', icon: Package, tone: 'red' },
  { to: '/reports', label: 'Reportes', icon: TrendingUp, tone: '' },
  { to: '/users', label: 'Usuarios', icon: FileText, tone: 'cyan' },
];

function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');
}

function formatMoney(n: number) {
  return `$${n.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function greetingByHour() {
  const h = new Date().getHours();
  if (h < 12) return 'Buenos días';
  if (h < 19) return 'Buenas tardes';
  return 'Buenas noches';
}

export default function Dashboard() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [patients, setPatients] = useState<Patient[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/api/v1/patients', { params: { page_size: 100 } }),
      api.get('/api/v1/appointments', { params: { page_size: 100 } }),
      api.get('/api/v1/invoices', { params: { page_size: 100 } }),
    ])
      .then(([p, a, i]) => {
        setPatients(p.data.items ?? []);
        setAppointments(a.data.items ?? []);
        setInvoices(i.data.items ?? []);
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron cargar los datos.')))
      .finally(() => setLoading(false));
  }, []);

  const todayIso = useMemo(() => new Date().toISOString().slice(0, 10), []);

  const stats = useMemo(() => {
    const activePatients = patients.filter((p) => p.is_active).length;
    const todayAppts = appointments.filter((a) => a.start_time.slice(0, 10) === todayIso).length;
    const attended = appointments.filter((a) => a.status === 'atendida').length;
    const pending = invoices.filter((i) => i.status === 'pendiente').length;
    const collected = invoices.reduce(
      (sum, inv) => sum + inv.payments.reduce((s, p) => s + (Number(p.amount) || 0), 0),
      0,
    );
    const totalInvoiced = invoices.reduce((s, inv) => s + (Number(inv.total) || 0), 0);
    return { activePatients, todayAppts, attended, pending, collected, totalInvoiced };
  }, [patients, appointments, invoices, todayIso]);

  const statusData = useMemo(
    () =>
      STATUS_ORDER.map((status) => ({
        name: STATUS_LABEL[status] ?? status,
        cantidad: appointments.filter((a) => a.status === status).length,
        status,
      })),
    [appointments],
  );

  const invoiceData = useMemo(() => {
    const map = new Map<string, number>();
    invoices.forEach((inv) => map.set(inv.status, (map.get(inv.status) ?? 0) + 1));
    return Array.from(map, ([name, value]) => ({ name: STATUS_LABEL[name] ?? name, value, status: name }));
  }, [invoices]);

  const recent = useMemo(
    () => [...appointments].sort((a, b) => b.start_time.localeCompare(a.start_time)).slice(0, 8),
    [appointments],
  );

  const barChart = {
    labels: statusData.map((d) => d.name),
    datasets: [
      {
        label: 'Citas',
        data: statusData.map((d) => d.cantidad),
        backgroundColor: statusData.map((d) => STATUS_COLORS[d.status] ?? '#0878F9'),
        borderRadius: 8,
        maxBarThickness: 48,
      },
    ],
  };

  const doughnutChart = {
    labels: invoiceData.map((d) => d.name),
    datasets: [
      {
        data: invoiceData.map((d) => d.value),
        backgroundColor: invoiceData.map((d) =>
          d.status === 'pagada' ? '#10B981' : d.status === 'pendiente' ? '#F59E0B' : '#EF4444',
        ),
        borderWidth: 0,
        hoverOffset: 6,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: { usePointStyle: true, boxWidth: 8, font: { size: 12 }, color: '#64748B' },
      },
      tooltip: {
        backgroundColor: '#061B3A',
        titleFont: { size: 13 },
        bodyFont: { size: 12 },
        padding: 10,
        cornerRadius: 10,
      },
    },
  };

  const barOptions = {
    ...chartOptions,
    plugins: {
      ...chartOptions.plugins,
      legend: { display: false },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#64748B', font: { size: 12 } },
        border: { display: false },
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(226,234,244,.9)' },
        ticks: { color: '#64748B', font: { size: 12 }, precision: 0 },
        border: { display: false },
      },
    },
  };

  const todayStr = useMemo(
    () =>
      new Date().toLocaleDateString('es-AR', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      }),
    [],
  );

  if (loading) return <Loading label="Cargando indicadores…" />;

  return (
    <>
      {/* Banner de bienvenida */}
      <section className="hero-banner">
        <div className="hero-banner__content">
          <div className="hero-banner__eyebrow">
            <Sparkles size={14} />
            Panel clínico en tiempo real
          </div>
          <h2>
            {greetingByHour()}, {user?.full_name?.split(' ')[0] ?? 'equipo'}
          </h2>
          <p>
            {todayStr.charAt(0).toUpperCase() + todayStr.slice(1)}. Supervise pacientes, agenda,
            atenciones y facturación desde un único panel profesional.
          </p>
          <div className="hero-banner__actions">
            <button className="btn btn--primary" onClick={() => nav('/appointments')}>
              <CalendarCheck size={16} />
              Abrir agenda
            </button>
            <button className="btn btn--soft" onClick={() => nav('/patients')}>
              <UserPlus size={16} />
              Nuevo paciente
            </button>
          </div>
        </div>
        <div className="hero-banner__visual" aria-hidden>
          <Activity size={64} strokeWidth={1.6} />
        </div>
      </section>

      {error && <ErrorBox message={error} />}

      {/* KPIs */}
      <div className="stat-grid">
        <div className="stat" style={{ animationDelay: '0ms' }}>
          <div className="stat__icon stat__icon--blue">
            <Users size={24} />
          </div>
          <div>
            <div className="stat__label">Pacientes activos</div>
            <div className="stat__value">{stats.activePatients}</div>
            <div className="stat__trend">{patients.length} fichas en total</div>
          </div>
        </div>

        <div className="stat" style={{ animationDelay: '60ms' }}>
          <div className="stat__icon stat__icon--cyan">
            <CalendarDays size={24} />
          </div>
          <div>
            <div className="stat__label">Citas de hoy</div>
            <div className="stat__value">{stats.todayAppts}</div>
            <div className="stat__trend">{appointments.length} citas registradas</div>
          </div>
        </div>

        <div className="stat" style={{ animationDelay: '120ms' }}>
          <div className="stat__icon stat__icon--green">
            <HeartPulse size={24} />
          </div>
          <div>
            <div className="stat__label">Atenciones</div>
            <div className="stat__value">{stats.attended}</div>
            <div className="stat__trend">Citas con estado atendida</div>
          </div>
        </div>

        <div className="stat" style={{ animationDelay: '180ms' }}>
          <div className="stat__icon stat__icon--amber">
            <CreditCard size={24} />
          </div>
          <div>
            <div className="stat__label">Ingresos cobrados</div>
            <div className="stat__value" style={{ fontSize: stats.collected > 999999 ? 20 : 26 }}>
              {formatMoney(stats.collected)}
            </div>
            <div className="stat__trend">
              {stats.pending} facturas pendientes · {formatMoney(stats.totalInvoiced)} emitidas
            </div>
          </div>
        </div>
      </div>

      {/* Gráficos */}
      <div className="grid-2">
        <Card>
          <CardHead title="Citas por estado" subtitle="Distribución actual de la agenda" />
          <div className="card__body">
            {appointments.length === 0 ? (
              <EmptyState title="Sin citas registradas" hint="Crea una cita desde la agenda." />
            ) : (
              <div className="chart-box">
                <Bar data={barChart} options={barOptions} />
              </div>
            )}
          </div>
        </Card>

        <Card>
          <CardHead title="Estado de facturación" subtitle="Facturas emitidas" />
          <div className="card__body">
            {invoiceData.length === 0 ? (
              <EmptyState title="Sin facturas" hint="Emite una factura desde Pagos." />
            ) : (
              <div className="chart-box">
                <Doughnut data={doughnutChart} options={chartOptions} />
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Tabla + accesos rápidos */}
      <div className="grid-2">
        <Card>
          <CardHead
            title="Últimas citas"
            subtitle="Ordenadas por fecha de inicio"
            actions={
              <Link to="/appointments" className="btn btn--ghost btn--sm">
                Ver todas
                <ArrowRight size={14} />
              </Link>
            }
          />
          {recent.length === 0 ? (
            <EmptyState title="Aún no hay citas" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Paciente</th>
                    <th>Médico</th>
                    <th>Fecha y hora</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.map((a) => {
                    const name = a.patient
                      ? `${a.patient.last_name}, ${a.patient.first_name}`
                      : `#${a.patient_id}`;
                    return (
                      <tr key={a.id}>
                        <td>
                          <div className="cell-user">
                            <span className="avatar-sm">
                              {a.patient ? initials(`${a.patient.first_name} ${a.patient.last_name}`) : '—'}
                            </span>
                            <div>
                              <strong>{name}</strong>
                              <span>{a.reason || 'Sin motivo declarado'}</span>
                            </div>
                          </div>
                        </td>
                        <td className="cell-muted">{a.doctor?.full_name ?? `#${a.doctor_id}`}</td>
                        <td className="cell-muted">{a.start_time.replace('T', ' ').slice(0, 16)}</td>
                        <td>
                          <Badge label={a.status} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card>
          <CardHead title="Accesos rápidos" subtitle="Módulos del sistema" />
          <div className="card__body">
            <div className="quick-grid">
              {QUICK_LINKS.map((q) => (
                <button
                  key={q.to}
                  type="button"
                  className={`quick-item ${q.tone ? `quick-item--${q.tone}` : ''}`}
                  onClick={() => nav(q.to)}
                >
                  <span className="quick-item__icon">
                    <q.icon size={22} strokeWidth={2} />
                  </span>
                  {q.label}
                </button>
              ))}
            </div>
          </div>
        </Card>
      </div>
    </>
  );
}
