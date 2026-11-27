import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Bell, FileUp, LogOut, Pencil, Plus, Search, ShieldCheck, Trash2, X } from 'lucide-react';
import { api } from './services/api';
import type { AlertaRecordatorio, Poliza } from './types/soat';

const formatCOP = (valor?: number | null) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(Number(valor || 0));

export function App() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('redcell_token'));
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const [polizas, setPolizas] = useState<Poliza[]>([]);
  const [alertas, setAlertas] = useState<AlertaRecordatorio[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);

  // Modal subida PDF
  const [uploadOpen, setUploadOpen] = useState(false);
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  // Modal edición de póliza y uso de plataforma
  const [editingPoliza, setEditingPoliza] = useState<Poliza | null>(null);
  const [usoPlataformaInput, setUsoPlataformaInput] = useState<string>('15000');
  const [telefonoInput, setTelefonoInput] = useState<string>('');
  const [emailClienteInput, setEmailClienteInput] = useState<string>('');
  const [savingEdit, setSavingEdit] = useState(false);

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const [listaPolizas, listaAlertas] = await Promise.all([
        api.listarPolizas(),
        api.obtenerAlertas().catch(() => []),
      ]);
      setPolizas(listaPolizas);
      setAlertas(listaAlertas);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error cargando datos';
      if (msg.includes('401') || msg.includes('403') || msg.toLowerCase().includes('token')) {
        api.logout();
        setToken(null);
      } else {
        setFeedback(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      cargarDatos();
    }
  }, [token]);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setAuthLoading(true);
    setAuthError(null);
    try {
      const jwt = await api.login(email, password);
      setToken(jwt);
    } catch (err: unknown) {
      setAuthError(err instanceof Error ? err.message : 'Credenciales inválidas');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleLogout = () => {
    api.logout();
    setToken(null);
    setPolizas([]);
    setAlertas([]);
  };

  const handleUploadPdf = async (e: FormEvent) => {
    e.preventDefault();
    if (!pdfFile) return;
    setUploading(true);
    setFeedback(null);
    try {
      const res = await api.subirPdf(pdfFile);
      setFeedback(res.message);
      setUploadOpen(false);
      setPdfFile(null);
      await cargarDatos();
    } catch (err: unknown) {
      setFeedback(err instanceof Error ? err.message : 'Error subiendo PDF');
    } finally {
      setUploading(false);
    }
  };

  const abrirEdicion = (poliza: Poliza) => {
    setEditingPoliza(poliza);
    setUsoPlataformaInput(String(poliza.usoPlataforma ?? 15000));
    setTelefonoInput(poliza.telefono ?? '');
    setEmailClienteInput(poliza.email ?? '');
  };

  const handleGuardarEdicion = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingPoliza) return;
    setSavingEdit(true);
    setFeedback(null);
    try {
      const nuevoUso = Math.max(0, Number(usoPlataformaInput || 0));
      await api.actualizarPoliza(editingPoliza.id, {
        usoPlataforma: nuevoUso,
        telefono: telefonoInput,
        email: emailClienteInput,
      });
      setFeedback(`Póliza ${editingPoliza.placa} actualizada. Uso de plataforma: ${formatCOP(nuevoUso)}.`);
      setEditingPoliza(null);
      await cargarDatos();
    } catch (err: unknown) {
      setFeedback(err instanceof Error ? err.message : 'Error al actualizar póliza');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleEliminar = async (poliza: Poliza) => {
    if (!window.confirm(`¿Eliminar la póliza de la placa ${poliza.placa}?`)) return;
    try {
      await api.eliminarPoliza(poliza.id);
      setFeedback(`Póliza ${poliza.placa} eliminada.`);
      await cargarDatos();
    } catch (err: unknown) {
      setFeedback(err instanceof Error ? err.message : 'Error al eliminar póliza');
    }
  };

  const polizasFiltradas = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return polizas;
    return polizas.filter(
      (p) =>
        p.placa?.toLowerCase().includes(q) ||
        p.propietario?.toLowerCase().includes(q) ||
        String(p.documento ?? '').includes(q) ||
        p.aseguradora?.toLowerCase().includes(q)
    );
  }, [polizas, search]);

  const totalRecaudo = useMemo(
    () => polizas.reduce((acc, p) => acc + Number(p.totalPagar || 0), 0),
    [polizas]
  );

  const totalPlataforma = useMemo(
    () => polizas.reduce((acc, p) => acc + Number(p.usoPlataforma || 0), 0),
    [polizas]
  );

  if (!token) {
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
              <h1 className="text-xl font-bold text-[#0F172A] m-0">RedCell SOAT</h1>
              <p className="text-xs text-[#64748B] m-0">Ingreso al panel operativo</p>
            </div>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-[#0F172A] mb-1">Correo electrónico</label>
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

            {authError && (
              <p className="text-xs font-medium text-[#DC2626] bg-red-50 border border-red-200 rounded-lg p-2.5 m-0">
                {authError}
              </p>
            )}

            <button
              type="submit"
              disabled={authLoading}
              className="w-full py-2.5 px-4 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white font-semibold text-sm cursor-pointer disabled:opacity-60"
            >
              {authLoading ? 'Ingresando...' : 'Iniciar sesión'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex bg-[#F8FAFC] text-[#0F172A]">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-[#E2E8F0] flex flex-col justify-between p-5 hidden md:flex">
        <div>
          <div className="flex items-center gap-2.5 pb-6 border-b border-[#E2E8F0]">
            <img
              src="/logo-redcell.png"
              alt="RedCell"
              className="h-9 w-auto object-contain"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.display = 'none';
              }}
            />
            <div>
              <div className="font-bold text-base leading-tight font-headline">RedCell SOAT</div>
              <div className="text-[11px] text-[#64748B]">Armenia · Quindío</div>
            </div>
          </div>

          <nav className="mt-6 space-y-1.5">
            <div className="flex items-center justify-between px-3 py-2.5 rounded-lg bg-red-50 text-[#DC2626] font-semibold text-sm">
              <span className="flex items-center gap-2.5">
                <ShieldCheck size={18} />
                Base de datos SOAT
              </span>
            </div>
            <div className="flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] text-sm">
              <span className="flex items-center gap-2.5">
                <Bell size={18} />
                Alertas pendientes
              </span>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-amber-100 text-[#F59E0B]">
                {alertas.length}
              </span>
            </div>
          </nav>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-[#64748B] hover:text-[#DC2626] hover:bg-red-50 cursor-pointer border-0 bg-transparent"
        >
          <LogOut size={17} />
          Cerrar sesión
        </button>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-6 md:p-8 max-w-7xl mx-auto w-full">
        <header className="flex flex-wrap items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-[#0F172A] m-0">Base de datos SOAT</h1>
            <p className="text-sm text-[#64748B] mt-1 mb-0">
              Consulta, carga PDFs y ajusta el uso de plataforma por póliza.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setUploadOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white text-sm font-semibold cursor-pointer border-0"
            >
              <Plus size={17} />
              Subir SOAT PDF
            </button>
          </div>
        </header>

        {feedback && (
          <div className="mb-5 px-4 py-3 rounded-xl bg-white border border-[#E2E8F0] flex items-center justify-between text-sm">
            <span>{feedback}</span>
            <button
              onClick={() => setFeedback(null)}
              className="text-[#64748B] hover:text-[#0F172A] bg-transparent border-0 cursor-pointer"
            >
              <X size={16} />
            </button>
          </div>
        )}

        {/* KPIs */}
        <section className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div className="bg-white border border-[#E2E8F0] rounded-xl p-5">
            <div className="text-xs font-medium text-[#64748B]">SOAT vigentes</div>
            <div className="text-2xl font-bold mt-1 font-headline">{polizas.length}</div>
          </div>
          <div className="bg-white border border-[#E2E8F0] rounded-xl p-5">
            <div className="text-xs font-medium text-[#64748B]">Acumulado uso de plataforma</div>
            <div className="text-2xl font-bold mt-1 text-[#16A34A] font-headline">
              {formatCOP(totalPlataforma)}
            </div>
          </div>
          <div className="bg-white border border-[#E2E8F0] rounded-xl p-5">
            <div className="text-xs font-medium text-[#64748B]">Recaudo total estimado</div>
            <div className="text-2xl font-bold mt-1 font-headline">{formatCOP(totalRecaudo)}</div>
          </div>
        </section>

        {/* Buscador y Tabla */}
        <section className="bg-white border border-[#E2E8F0] rounded-xl overflow-hidden">
          <div className="p-4 border-b border-[#E2E8F0] flex items-center justify-between gap-3">
            <div className="relative flex-1 max-w-md">
              <Search
                size={16}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-[#64748B]"
              />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Buscar por placa, propietario, documento o aseguradora..."
                className="w-full pl-9 pr-3 py-2 rounded-lg border border-[#E2E8F0] text-sm outline-none focus:border-[#0F172A]"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#E2E8F0] bg-[#F8FAFC] text-[11px] font-semibold uppercase text-[#64748B]">
                  <th className="py-3 px-4">Placa / Vehículo</th>
                  <th className="py-3 px-4">Propietario</th>
                  <th className="py-3 px-4">Aseguradora</th>
                  <th className="py-3 px-4">Vigencia</th>
                  <th className="py-3 px-4">Valor SOAT</th>
                  <th className="py-3 px-4">Uso Plataforma</th>
                  <th className="py-3 px-4">Total a Pagar</th>
                  <th className="py-3 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E2E8F0] text-sm">
                {loading ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-[#64748B]">
                      Cargando pólizas...
                    </td>
                  </tr>
                ) : polizasFiltradas.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-[#64748B]">
                      No hay pólizas registradas.
                    </td>
                  </tr>
                ) : (
                  polizasFiltradas.map((p) => (
                    <tr key={p.id} className="hover:bg-[#F8FAFC]">
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-[#0F172A]">{p.placa}</div>
                        <div className="text-xs text-[#64748B]">{p.marcaLinea || p.tipoVehiculo}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div>{p.propietario || '—'}</div>
                        <div className="text-xs text-[#64748B]">
                          {p.tipoDocumento ? `${p.tipoDocumento} ` : ''}
                          {p.documento ?? ''}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-xs font-medium">{p.aseguradora || '—'}</td>
                      <td className="py-3.5 px-4 text-xs text-[#64748B]">
                        <div>{p.desde || '—'}</div>
                        <div>{p.hasta || '—'}</div>
                      </td>
                      <td className="py-3.5 px-4">{formatCOP(p.precioSoat)}</td>
                      <td className="py-3.5 px-4 font-medium text-[#16A34A]">
                        {formatCOP(p.usoPlataforma)}
                      </td>
                      <td className="py-3.5 px-4 font-bold">{formatCOP(p.totalPagar)}</td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            onClick={() => abrirEdicion(p)}
                            title="Editar uso de plataforma y datos"
                            className="p-2 rounded-lg border border-[#E2E8F0] hover:bg-[#F1F5F9] text-[#0F172A] bg-white cursor-pointer"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            onClick={() => handleEliminar(p)}
                            title="Eliminar póliza"
                            className="p-2 rounded-lg border border-[#E2E8F0] hover:bg-red-50 text-[#DC2626] bg-white cursor-pointer"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>

      {/* Modal Subir PDF */}
      {uploadOpen && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold m-0">Subir SOAT en PDF</h2>
              <button
                onClick={() => setUploadOpen(false)}
                className="text-[#64748B] bg-transparent border-0 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleUploadPdf} className="space-y-4">
              <label className="flex flex-col items-center justify-center border-2 border-dashed border-[#E2E8F0] rounded-xl p-6 cursor-pointer hover:border-[#DC2626]">
                <FileUp size={28} className="text-[#DC2626] mb-2" />
                <span className="text-sm font-medium text-center">
                  {pdfFile ? pdfFile.name : 'Selecciona el PDF de Previsora o AXA Colpatria'}
                </span>
                <input
                  type="file"
                  accept="application/pdf"
                  className="hidden"
                  onChange={(e) => setPdfFile(e.target.files?.[0] ?? null)}
                />
              </label>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setUploadOpen(false)}
                  className="px-4 py-2 rounded-lg border border-[#E2E8F0] text-sm bg-white cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={!pdfFile || uploading}
                  className="px-4 py-2 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white text-sm font-semibold border-0 cursor-pointer disabled:opacity-60"
                >
                  {uploading ? 'Procesando...' : 'Subir y calcular'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Editar Uso de Plataforma */}
      {editingPoliza && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold m-0">Editar póliza · {editingPoliza.placa}</h2>
              <button
                onClick={() => setEditingPoliza(null)}
                className="text-[#64748B] bg-transparent border-0 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleGuardarEdicion} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold mb-1">
                  Uso de plataforma (COP)
                </label>
                <input
                  type="number"
                  min={0}
                  step={100}
                  required
                  value={usoPlataformaInput}
                  onChange={(e) => setUsoPlataformaInput(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm font-semibold"
                />
                <p className="text-xs text-[#64748B] mt-1 mb-0">
                  Valor SOAT base: {formatCOP(editingPoliza.precioSoat)} · Nuevo total estimado:{' '}
                  <strong>
                    {formatCOP(Number(editingPoliza.precioSoat || 0) + Number(usoPlataformaInput || 0))}
                  </strong>
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold mb-1">Teléfono WhatsApp</label>
                <input
                  type="text"
                  value={telefonoInput}
                  onChange={(e) => setTelefonoInput(e.target.value)}
                  placeholder="3117034142"
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold mb-1">Correo electrónico</label>
                <input
                  type="email"
                  value={emailClienteInput}
                  onChange={(e) => setEmailClienteInput(e.target.value)}
                  placeholder="cliente@correo.com"
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingPoliza(null)}
                  className="px-4 py-2 rounded-lg border border-[#E2E8F0] text-sm bg-white cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingEdit}
                  className="px-4 py-2 rounded-lg bg-[#16A34A] hover:bg-[#15803D] text-white text-sm font-semibold border-0 cursor-pointer disabled:opacity-60"
                >
                  {savingEdit ? 'Guardando...' : 'Guardar y recalcular'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;