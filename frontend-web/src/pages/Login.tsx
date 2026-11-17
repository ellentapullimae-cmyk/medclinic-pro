import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthBrandPanel from '../components/AuthBrandPanel';
import { Eye, EyeOff, Lock, Mail, UserPlus } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [remember, setRemember] = useState(true);
  const [err, setErr] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const nav = useNavigate();

  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  const canSubmit = emailOk && password.length >= 4 && !loading;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr('');
    if (!emailOk) {
      setErr('Ingresa un correo electrónico válido.');
      return;
    }
    if (password.length < 4) {
      setErr('La contraseña debe tener al menos 4 caracteres.');
      return;
    }
    setLoading(true);
    try {
      await login(email.trim(), password);
      if (remember) {
        localStorage.setItem('mc_remember_email', email.trim());
      } else {
        localStorage.removeItem('mc_remember_email');
      }
      nav('/');
    } catch (error: any) {
      const detail = error?.response?.data?.detail;
      if (typeof detail === 'string') setErr(detail);
      else if (error?.response?.status === 401) setErr('Credenciales inválidas. Verifica tu correo y contraseña.');
      else if (error?.response?.status === 422) setErr('Datos inválidos. Revise los campos.');
      else setErr('No se pudo iniciar sesión. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth">
      <AuthBrandPanel
        headline={
          <>
            La gestión de tu clínica,
            <br />
            en <span className="auth__accent">un solo lugar</span>.
          </>
        }
        description="Pacientes, citas, historia clínica, facturación e inventario con datos en tiempo real y seguridad de nivel profesional."
      />

      <div className="auth__panel">
        <div className="auth__card">
          <div className="auth__card-icon">
            <Lock size={20} />
          </div>
          <h2>Bienvenido</h2>
          <p className="auth__subtitle">Ingresa a tu cuenta para continuar con MedClinic Pro</p>

          <form onSubmit={submit} className="auth__form" noValidate>
            <div className="field">
              <label htmlFor="email">
                Correo electrónico <span>*</span>
              </label>
              <div className="input-icon">
                <Mail size={16} />
                <input
                  id="email"
                  className="input"
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@clinica.com"
                  required
                />
              </div>
              {email.length > 0 && !emailOk && (
                <span className="field-hint field-hint--error">Formato de correo inválido</span>
              )}
            </div>

            <div className="field">
              <label htmlFor="password">
                Contraseña <span>*</span>
              </label>
              <div className="input-icon">
                <Lock size={16} />
                <input
                  id="password"
                  className="input"
                  type={showPass ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  className="input-icon__btn"
                  onClick={() => setShowPass((v) => !v)}
                  aria-label={showPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  tabIndex={-1}
                >
                  {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="auth__row">
              <label className="auth__check">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                />
                <span>Recordar sesión</span>
              </label>
              <button
                type="button"
                className="auth__link"
                onClick={() =>
                  setErr('Contacte al administrador del sistema para recuperar su contraseña.')
                }
              >
                ¿Olvidaste tu contraseña?
              </button>
            </div>

            {err && (
              <div className="error-box error-box--elegant" role="alert">
                {err}
              </div>
            )}

            <button className="btn btn--primary btn--block btn--lg" type="submit" disabled={!canSubmit}>
              {loading ? (
                <>
                  <span className="btn__spinner" aria-hidden />
                  Ingresando…
                </>
              ) : (
                'INGRESAR'
              )}
            </button>
          </form>

          <div className="auth__register-block">
            <p>¿No tienes una cuenta?</p>
            <Link to="/register" className="btn btn--register">
              <UserPlus size={17} />
              Regístrate aquí
            </Link>
          </div>

          <p className="auth__hint">
            Admin demo: <strong>admin@clinica.com</strong> · <strong>admin123</strong>
          </p>
        </div>
      </div>
    </div>
  );
}
