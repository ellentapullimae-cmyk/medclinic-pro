import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Activity,
  Bell,
  CalendarDays,
  CreditCard,
  FileText,
  HeartPulse,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  Search,
  Stethoscope,
  TrendingUp,
  Users,
  UserRound,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';

interface NavEntry {
  to: string;
  label: string;
  icon: React.ComponentType<{ size?: number; strokeWidth?: number }>;
}

const NAV: { section: string; items: NavEntry[] }[] = [
  {
    section: 'General',
    items: [
      { to: '/', label: 'Dashboard', icon: LayoutDashboard },
      { to: '/reports', label: 'Reportes', icon: TrendingUp },
    ],
  },
  {
    section: 'Atención',
    items: [
      { to: '/patients', label: 'Pacientes', icon: Users },
      { to: '/doctors', label: 'Médicos', icon: Stethoscope },
      { to: '/appointments', label: 'Citas médicas', icon: CalendarDays },
      { to: '/clinical', label: 'Historial clínico', icon: HeartPulse },
    ],
  },
  {
    section: 'Administración',
    items: [
      { to: '/billing', label: 'Pagos y facturas', icon: CreditCard },
      { to: '/expenses', label: 'Gastos', icon: FileText },
      { to: '/inventory', label: 'Inventario', icon: Package },
      { to: '/users', label: 'Usuarios', icon: UserRound },
    ],
  },
];

const TITLES: Record<string, { title: string; subtitle: string }> = {
  '/': { title: 'Dashboard', subtitle: 'Resumen general de la clínica' },
  '/patients': { title: 'Pacientes', subtitle: 'Gestión de fichas e historial' },
  '/doctors': { title: 'Médicos', subtitle: 'Directorio del equipo profesional' },
  '/appointments': { title: 'Citas médicas', subtitle: 'Agenda y seguimiento' },
  '/clinical': { title: 'Historial clínico', subtitle: 'Atenciones y consultas' },
  '/billing': { title: 'Pagos y facturas', subtitle: 'Facturación y cobros' },
  '/expenses': { title: 'Gastos', subtitle: 'Consumo de insumos' },
  '/inventory': { title: 'Inventario', subtitle: 'Stock e insumos médicos' },
  '/users': { title: 'Usuarios', subtitle: 'Equipos y permisos' },
  '/reports': { title: 'Reportes', subtitle: 'Indicadores en tiempo real' },
};

function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');
}

interface SearchPatient {
  id: number;
  first_name: string;
  last_name: string;
  dni?: string;
}

export default function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchPatient[]>([]);
  const [searching, setSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  const meta = TITLES[location.pathname] ?? { title: 'MedClinic Pro', subtitle: '' };

  const handleLogout = () => {
    logout();
    nav('/login');
  };

  useEffect(() => {
    if (query.trim().length < 2) {
      setResults([]);
      setShowResults(false);
      return;
    }
    const t = setTimeout(() => {
      setSearching(true);
      api
        .get('/api/v1/patients', { params: { q: query.trim(), page_size: 6 } })
        .then((r) => {
          setResults(r.data.items ?? []);
          setShowResults(true);
        })
        .catch(() => {
          setResults([]);
          setShowResults(false);
        })
        .finally(() => setSearching(false));
    }, 280);
    return () => clearTimeout(t);
  }, [query]);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const goPatient = (id: number) => {
    setShowResults(false);
    setQuery('');
    nav(`/patients?focus=${id}`);
  };

  return (
    <div className="app">
      <div className={`sidebar-overlay ${open ? 'show' : ''}`} onClick={() => setOpen(false)} />

      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="sidebar__brand">
          <span
            style={{
              width: 40,
              height: 40,
              borderRadius: 12,
              background: 'linear-gradient(135deg,#0878f9,#16c7f5)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 8px 20px rgba(8,120,249,.35)',
            }}
          >
            <Activity size={22} strokeWidth={2.4} />
          </span>
          <span>
            MedClinic Pro
            <small>Sistema médico</small>
          </span>
        </div>

        <nav className="sidebar__nav">
          {NAV.map((group) => (
            <div key={group.section}>
              <div className="sidebar__section">{group.section}</div>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                  onClick={() => {
                    setOpen(false);
                    setQuery('');
                    setShowResults(false);
                  }}
                >
                  <item.icon size={18} strokeWidth={2} />
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar__footer">
          <button className="nav-item" onClick={handleLogout}>
            <LogOut size={18} strokeWidth={2} />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="header">
          <button className="header__hamburger" onClick={() => setOpen(true)} aria-label="Abrir menú">
            <Menu size={19} />
          </button>

          <div className="header__title">
            <h1>{meta.title}</h1>
            <span>{meta.subtitle}</span>
          </div>

          <div className="header__search" ref={searchRef}>
            <Search size={17} />
            <input
              className="input"
              type="search"
              placeholder="Buscar pacientes por nombre o DNI…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onFocus={() => results.length > 0 && setShowResults(true)}
              aria-label="Buscar pacientes"
            />
            {showResults && (
              <div className="search-results">
                {searching && results.length === 0 ? (
                  <div className="sr-empty">Buscando…</div>
                ) : results.length === 0 ? (
                  <div className="sr-empty">Sin resultados para “{query}”</div>
                ) : (
                  results.map((p) => (
                    <button key={p.id} type="button" onClick={() => goPatient(p.id)}>
                      <span className="sr-avatar">{initials(`${p.first_name} ${p.last_name}`)}</span>
                      <span className="sr-meta">
                        <strong>
                          {p.last_name}, {p.first_name}
                        </strong>
                        <span>{p.dni ? `DNI ${p.dni}` : `Ficha #${p.id}`}</span>
                      </span>
                    </button>
                  ))
                )}
              </div>
            )}
          </div>

          <div className="header__actions">
            <button className="header__icon-btn" aria-label="Notificaciones" title="Sin notificaciones nuevas">
              <Bell size={18} />
              <span className="header__dot" />
            </button>

            <div className="userchip" title={user?.email ?? ''}>
              <span className="avatar">{user ? initials(user.full_name) : 'MC'}</span>
              <span className="userchip__meta">
                <strong>{user?.full_name ?? 'Invitado'}</strong>
                <span>{user?.role ?? ''}</span>
              </span>
            </div>
          </div>
        </header>

        <main className="content" key={location.pathname}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
