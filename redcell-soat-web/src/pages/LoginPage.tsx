import { useState, type FormEvent } from 'react';
import { api } from '../services/api';

interface LoginPageProps {
  onSuccess: (token: string) => void;
}

export function LoginPage({ onSuccess }: LoginPageProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const jwt = await api.login(email, password);
      onSuccess(jwt);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Credenciales inválidas');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-[#F8FAFC]">
      <div className="w-full max-w-md bg-white border border-[#E2E8F0] rounded-2xl p-8 shadow-sm">
        <div className="flex items-center gap-3 mb-6">
          <img
            src="/logo-redcell.png"
            alt="RedCell"
            className="h-11 w-auto object-contain"
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).style.display = 'none';
            }}
          />
          <div>
            <h1 className="text-xl font-bold text-[#0F172A] m-0 font-headline">RedCell SOAT</h1>
            <p className="text-xs text-[#64748B] m-0">Ingreso al panel operativo</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-[#0F172A] mb-1">
              Correo electrónico
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="usuario@redcell.com"
              className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm outline-none focus:border-[#DC2626]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#0F172A] mb-1">Contraseña</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm outline-none focus:border-[#DC2626]"
            />
          </div>

          {error && (
            <p className="text-xs font-medium text-[#DC2626] bg-red-50 border border-red-200 rounded-lg p-2.5 m-0">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white font-semibold text-sm cursor-pointer disabled:opacity-60"
          >
            {loading ? 'Ingresando...' : 'Iniciar sesión'}
          </button>
        </form>
      </div>
    </div>
  );
}