import { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client';

interface User { id: number; email: string; full_name: string; role: string; specialty?: string; }
interface AuthCtx { user: User | null; token: string | null; login: (e:string,p:string)=>Promise<void>; logout: ()=>void; }

const Ctx = createContext<AuthCtx>(null as any);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    if (token) {
      api.get('/api/v1/users/me').then(r => setUser(r.data)).catch(() => { localStorage.removeItem('token'); setToken(null); });
    } else setUser(null);
  }, [token]);

  const login = async (email: string, password: string) => {
    const r = await api.post('/api/v1/auth/login', { email, password });
    const t = r.data.access_token;
    localStorage.setItem('token', t);
    setToken(t);
  };
  const logout = () => { localStorage.removeItem('token'); setToken(null); setUser(null); };

  return <Ctx.Provider value={{ user, token, login, logout }}>{children}</Ctx.Provider>;
}
