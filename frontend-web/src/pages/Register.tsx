import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import AuthBrandPanel from '../components/AuthBrandPanel';
import {
  ArrowLeft,
  Eye,
  EyeOff,
  FileText,
  IdCard,
  Lock,
  Mail,
  Phone,
  User,
  UserPlus,
} from 'lucide-react';

const DOC_TYPES = [
  { value: 'dni', label: 'DNI' },
  { value: 'cedula', label: 'Cédula' },
  { value: 'pasaporte', label: 'Pasaporte' },
  { value: 'otro', label: 'Otro' },
];

type Errors = Partial<Record<string, string>>;

function passwordScore(p: string): number {
  let s = 0;
  if (p.length >= 6) s++;
  if (p.length >= 10) s++;
  if (/[A-Z]/.test(p)) s++;
  if (/[0-9]/.test(p)) s++;
  if (/[^A-Za-z0-9]/.test(p)) s++;
  return Math.min(s, 4);
}

const SCORE_LABEL = ['Muy débil', 'Débil', 'Aceptable', 'Fuerte', 'Excelente'];
const SCORE_COLOR = ['#EF4444', '#EF4444', '#F59E0B', '#10B981', '#10B981'];

export default function Register() {
  const nav = useNavigate();
  const [form, setForm] = useState({
    first_name: '',
    last_name: '',
    document_type: 'dni',
    document_number: '',
    email: '',
    phone: '',
    password: '',
    password_confirm: '',
    accept_terms: false,
  });
  const [showPass, setShowPass] = useState(false);
  const [showPass2, setShowPass2] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [serverErr, setServerErr] = useState('');
  const [okMsg, setOkMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const score = useMemo(() => passwordScore(form.password), [form.password]);

  const validate = (f = form): Errors => {
    const e: Errors = {};
    if (f.first_name.trim().length < 2) e.first_name = 'Mínimo 2 caracteres';
    if (f.last_name.trim().length < 2) e.last_name = 'Mínimo 2 caracteres';
    if (f.document_number.trim().length < 5) e.document_number = 'Mínimo 5 caracteres';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) e.email = 'Correo inválido';
    if (f.phone && f.phone.trim().length < 7) e.phone = 'Teléfono inválido';
    if (f.password.length < 6) e.password = 'Mínimo 6 caracteres';
    if (f.password_confirm !== f.password) e.password_confirm = 'Las contraseñas no coinciden';
    if (!f.accept_terms) e.accept_terms = 'Debe aceptar los términos';
    return e;
  };

  const errors = validate();
  const showErr = (k: string) => (touched[k] || serverErr ? errors[k] : undefined);

  const set = (k: keyof typeof form, v: string | boolean) => {
    setForm((s) => ({ ...s, [k]: v }));
    setServerErr('');
    setOkMsg('');
  };

  const blur = (k: string) => setTouched((t) => ({ ...t, [k]: true }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerErr('');
    setOkMsg('');
    const all = validate();
    setTouched({
      first_name: true,
      last_name: true,
      document_number: true,
      email: true,
      phone: true,
      password: true,
      password_confirm: true,
      accept_terms: true,
    });
    if (Object.keys(all).length > 0) {
      setServerErr('Revise los campos marcados.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        first_name: form.first_name.trim(),
        last_name: form.last_name.trim(),
        document_type: form.document_type,
        document_number: form.document_number.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        password: form.password,
        password_confirm: form.password_confirm,
        accept_terms: form.accept_terms,
      };
      const r = await api.post('/api/v1/auth/register', payload);
      setOkMsg(r.data?.message || 'Cuenta creada correctamente.');
      setTimeout(() => nav('/login'), 1400);
    } catch (error: any) {
      const detail = error?.response?.data?.detail;
      if (typeof detail === 'string') setServerErr(detail);
      else if (Array.isArray(detail) && detail[0]?.msg) setServerErr(String(detail[0].msg));
      else if (error?.response?.status === 409) setServerErr('Correo o documento ya registrado.');
      else setServerErr('No se pudo crear la cuenta. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth">
      <AuthBrandPanel
        headline={
          <>
            Crea tu cuenta
            <br />
            en <span className="auth__accent">un solo lugar</span>.
          </>
        }
        description="Registro seguro para pacientes. Su cuenta se creará con rol paciente y ficha clínica asociada en el sistema."
      />

      <div className="auth__panel">
        <div className="auth__card auth__card--wide">
          <div className="auth__card-icon">
            <UserPlus size={20} />
          </div>
          <h2>Crear cuenta</h2>
          <p className="auth__subtitle">Completa tus datos para registrarte como paciente</p>

          <form onSubmit={submit} className="auth__form" noValidate>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="first_name">
                  Nombres <span>*</span>
                </label>
                <div className="input-icon">
                  <User size={16} />
                  <input
                    id="first_name"
                    className="input"
                    value={form.first_name}
                    onChange={(e) => set('first_name', e.target.value)}
                    onBlur={() => blur('first_name')}
                    placeholder="Ana"
                    autoComplete="given-name"
                  />
                </div>
                {showErr('first_name') && <span className="field-hint field-hint--error">{errors.first_name}</span>}
              </div>

              <div className="field">
                <label htmlFor="last_name">
                  Apellidos <span>*</span>
                </label>
                <div className="input-icon">
                  <User size={16} />
                  <input
                    id="last_name"
                    className="input"
                    value={form.last_name}
                    onChange={(e) => set('last_name', e.target.value)}
                    onBlur={() => blur('last_name')}
                    placeholder="Torres"
                    autoComplete="family-name"
                  />
                </div>
                {showErr('last_name') && <span className="field-hint field-hint--error">{errors.last_name}</span>}
              </div>

              <div className="field">
                <label htmlFor="document_type">
                  Tipo de documento <span>*</span>
                </label>
                <div className="input-icon">
                  <IdCard size={16} />
                  <select
                    id="document_type"
                    className="input select"
                    value={form.document_type}
                    onChange={(e) => set('document_type', e.target.value)}
                  >
                    {DOC_TYPES.map((d) => (
                      <option key={d.value} value={d.value}>
                        {d.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="field">
                <label htmlFor="document_number">
                  Número de documento <span>*</span>
                </label>
                <div className="input-icon">
                  <FileText size={16} />
                  <input
                    id="document_number"
                    className="input"
                    value={form.document_number}
                    onChange={(e) => set('document_number', e.target.value)}
                    onBlur={() => blur('document_number')}
                    placeholder="12345678"
                    inputMode="numeric"
                    autoComplete="off"
                  />
                </div>
                {showErr('document_number') && (
                  <span className="field-hint field-hint--error">{errors.document_number}</span>
                )}
              </div>

              <div className="field">
                <label htmlFor="reg_email">
                  Correo electrónico <span>*</span>
                </label>
                <div className="input-icon">
                  <Mail size={16} />
                  <input
                    id="reg_email"
                    className="input"
                    type="email"
                    value={form.email}
                    onChange={(e) => set('email', e.target.value)}
                    onBlur={() => blur('email')}
                    placeholder="paciente@email.com"
                    autoComplete="email"
                  />
                </div>
                {showErr('email') && <span className="field-hint field-hint--error">{errors.email}</span>}
              </div>

              <div className="field">
                <label htmlFor="phone">Teléfono</label>
                <div className="input-icon">
                  <Phone size={16} />
                  <input
                    id="phone"
                    className="input"
                    value={form.phone}
                    onChange={(e) => set('phone', e.target.value)}
                    onBlur={() => blur('phone')}
                    placeholder="+54 11 0000-0000"
                    autoComplete="tel"
                  />
                </div>
                {showErr('phone') && <span className="field-hint field-hint--error">{errors.phone}</span>}
              </div>

              <div className="field">
                <label htmlFor="reg_password">
                  Contraseña <span>*</span>
                </label>
                <div className="input-icon">
                  <Lock size={16} />
                  <input
                    id="reg_password"
                    className="input"
                    type={showPass ? 'text' : 'password'}
                    value={form.password}
                    onChange={(e) => set('password', e.target.value)}
                    onBlur={() => blur('password')}
                    placeholder="Mínimo 6 caracteres"
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    className="input-icon__btn"
                    onClick={() => setShowPass((v) => !v)}
                    tabIndex={-1}
                    aria-label="Mostrar contraseña"
                  >
                    {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {form.password && (
                  <div className="pwd-meter" aria-hidden>
                    <div className="pwd-meter__bar">
                      <span
                        style={{
                          width: `${((score + 1) / 5) * 100}%`,
                          background: SCORE_COLOR[score],
                        }}
                      />
                    </div>
                    <small style={{ color: SCORE_COLOR[score] }}>{SCORE_LABEL[score]}</small>
                  </div>
                )}
                {showErr('password') && <span className="field-hint field-hint--error">{errors.password}</span>}
              </div>

              <div className="field">
                <label htmlFor="reg_password2">
                  Confirmar contraseña <span>*</span>
                </label>
                <div className="input-icon">
                  <Lock size={16} />
                  <input
                    id="reg_password2"
                    className="input"
                    type={showPass2 ? 'text' : 'password'}
                    value={form.password_confirm}
                    onChange={(e) => set('password_confirm', e.target.value)}
                    onBlur={() => blur('password_confirm')}
                    placeholder="Repita la contraseña"
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    className="input-icon__btn"
                    onClick={() => setShowPass2((v) => !v)}
                    tabIndex={-1}
                    aria-label="Mostrar confirmación"
                  >
                    {showPass2 ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {showErr('password_confirm') && (
                  <span className="field-hint field-hint--error">{errors.password_confirm}</span>
                )}
              </div>
            </div>

            <label className={`auth__check auth__check--block ${showErr('accept_terms') ? 'is-error' : ''}`}>
              <input
                type="checkbox"
                checked={form.accept_terms}
                onChange={(e) => set('accept_terms', e.target.checked)}
                onBlur={() => blur('accept_terms')}
              />
              <span>
                Acepto los <strong>términos y condiciones</strong> y el tratamiento de mis datos
                personales para fines asistenciales.
              </span>
            </label>
            {showErr('accept_terms') && (
              <span className="field-hint field-hint--error">{errors.accept_terms}</span>
            )}

            {serverErr && (
              <div className="error-box error-box--elegant" role="alert">
                {serverErr}
              </div>
            )}
            {okMsg && (
              <div className="success-box" role="status">
                {okMsg}
              </div>
            )}

            <button
              className="btn btn--primary btn--block btn--lg"
              type="submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="btn__spinner" aria-hidden />
                  Creando cuenta…
                </>
              ) : (
                'Crear cuenta'
              )}
            </button>
          </form>

          <div className="auth__register-block">
            <p>¿Ya tienes una cuenta?</p>
            <Link to="/login" className="btn btn--register">
              <ArrowLeft size={17} />
              Volver al inicio de sesión
            </Link>
          </div>

          <p className="auth__hint">
            Los usuarios públicos reciben rol <strong>paciente</strong> automáticamente.
          </p>
        </div>
      </div>
    </div>
  );
}
