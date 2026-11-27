import { useCallback, useEffect, useMemo, useState } from 'react';
import { Bell, CheckCircle2, ExternalLink, Plus, ShieldCheck, X } from 'lucide-react';
import { Sidebar } from '../components/layout/Sidebar';
import { DetallePolizaModal } from '../components/modals/DetallePolizaModal';
import { EditarPolizaModal } from '../components/modals/EditarPolizaModal';
import { SubirPdfModal } from '../components/modals/SubirPdfModal';
import { KpiCards } from '../components/polizas/KpiCards';
import { PolizasTable, type EstadoVigenciaFilter } from '../components/polizas/PolizasTable';
import { api } from '../services/api';
import type { AlertaRecordatorio, Poliza, PolizaUpdateDTO } from '../types/soat';
import { buildWhatsappLink, formatCOP } from '../utils/format';

interface DashboardPageProps {
  onUnauthorized: () => void;
}

export function DashboardPage({ onUnauthorized }: DashboardPageProps) {
  const [polizas, setPolizas] = useState<Poliza[]>([]);
  const [alertas, setAlertas] = useState<AlertaRecordatorio[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [aseguradoraFilter, setAseguradoraFilter] = useState('TODAS');
  const [tipoVehiculoFilter, setTipoVehiculoFilter] = useState('TODOS');
  const [vigenciaFilter, setVigenciaFilter] = useState<EstadoVigenciaFilter>('TODOS');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<'polizas' | 'alertas'>('polizas');

  const [uploadOpen, setUploadOpen] = useState(false);
  const [viewingPoliza, setViewingPoliza] = useState<Poliza | null>(null);
  const [editingPoliza, setEditingPoliza] = useState<Poliza | null>(null);
  const [markingKey, setMarkingKey] = useState<string | null>(null);

  const cargarDatos = useCallback(async () => {
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
        onUnauthorized();
      } else {
        setFeedback(msg);
      }
    } finally {
      setLoading(false);
    }
  }, [onUnauthorized]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const handleUploadPdf = async (file: File) => {
    const res = await api.subirPdf(file);
    setFeedback(res.message);
    await cargarDatos();
  };

  const handleGuardarEdicion = async (id: number, dto: PolizaUpdateDTO) => {
    const actualizada = await api.actualizarPoliza(id, dto);
    setFeedback(
      `Póliza ${actualizada.placa} actualizada · Uso de plataforma: ${formatCOP(actualizada.usoPlataforma)} · Total: ${formatCOP(actualizada.totalPagar)}.`
    );
    await cargarDatos();
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

  const handleMarkWhatsappSent = async (polizaId: number, tipo: string) => {
    const key = `${polizaId}-${tipo}`;
    setMarkingKey(key);
    try {
      await api.marcarWhatsappEnviado(polizaId, tipo);
      setFeedback(`Recordatorio (${tipo}) marcado como enviado por WhatsApp.`);
      await cargarDatos();
    } catch (err: unknown) {
      setFeedback(err instanceof Error ? err.message : 'Error al marcar WhatsApp como enviado');
    } finally {
      setMarkingKey(null);
    }
  };

  const aseguradoras = useMemo(() => {
    const set = new Set<string>();
    polizas.forEach((p) => {
      if (p.aseguradora?.trim()) set.add(p.aseguradora.trim());
    });
    return Array.from(set).sort();
  }, [polizas]);

  const tiposVehiculo = useMemo(() => {
    const set = new Set<string>();
    polizas.forEach((p) => {
      if (p.tipoVehiculo?.trim()) set.add(p.tipoVehiculo.trim());
    });
    return Array.from(set).sort();
  }, [polizas]);

  const polizasFiltradas = useMemo(() => {
    const q = search.trim().toLowerCase();
    const hoyStr = new Date().toISOString().slice(0, 10);

    return polizas.filter((p) => {
      if (q) {
        const matchesSearch =
          p.placa?.toLowerCase().includes(q) ||
          p.propietario?.toLowerCase().includes(q) ||
          String(p.documento ?? '').includes(q) ||
          p.aseguradora?.toLowerCase().includes(q) ||
          p.marcaLinea?.toLowerCase().includes(q);
        if (!matchesSearch) return false;
      }

      if (aseguradoraFilter !== 'TODAS' && p.aseguradora?.trim() !== aseguradoraFilter) {
        return false;
      }

      if (tipoVehiculoFilter !== 'TODOS' && p.tipoVehiculo?.trim() !== tipoVehiculoFilter) {
        return false;
      }

      if (vigenciaFilter !== 'TODOS') {
        const estaVigente = Boolean(p.activo) && (!p.hasta || p.hasta >= hoyStr);
        if (vigenciaFilter === 'VIGENTES' && !estaVigente) return false;
        if (vigenciaFilter === 'VENCIDOS' && estaVigente) return false;
      }

      return true;
    });
  }, [polizas, search, aseguradoraFilter, tipoVehiculoFilter, vigenciaFilter]);

  const handleClearFilters = () => {
    setSearch('');
    setAseguradoraFilter('TODAS');
    setTipoVehiculoFilter('TODOS');
    setVigenciaFilter('TODOS');
  };

  const totalPlataforma = useMemo(
    () => polizas.reduce((acc, p) => acc + Number(p.usoPlataforma || 0), 0),
    [polizas]
  );

  return (
    <div className="min-h-screen flex bg-[#F8FAFC] text-[#0F172A] w-full">
      <Sidebar alertasCount={alertas.length} onLogout={onUnauthorized} />

      <main className="flex-1 p-6 md:p-8 max-w-none w-full">
        <header className="flex flex-wrap items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-[#0F172A] m-0 font-headline">
              Base de datos SOAT
            </h1>
            <p className="text-sm text-[#64748B] mt-1 mb-0">
              Consulta, carga PDFs, edita pólizas y gestiona recordatorios de vencimiento.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              onClick={() => setActiveSection('polizas')}
              className={
                activeSection === 'polizas'
                  ? 'inline-flex items-center gap-2 px-3.5 py-2.5 rounded-lg text-sm font-semibold cursor-pointer border border-[#0F172A] bg-[#0F172A] text-white'
                  : 'inline-flex items-center gap-2 px-3.5 py-2.5 rounded-lg text-sm font-semibold cursor-pointer border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'
              }
            >
              <ShieldCheck size={16} />
              Pólizas ({polizas.length})
            </button>

            <button
              type="button"
              onClick={() => setActiveSection('alertas')}
              className={
                activeSection === 'alertas'
                  ? 'inline-flex items-center gap-2 px-3.5 py-2.5 rounded-lg text-sm font-semibold cursor-pointer border border-[#D97706] bg-[#D97706] text-white'
                  : 'inline-flex items-center gap-2 px-3.5 py-2.5 rounded-lg text-sm font-semibold cursor-pointer border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'
              }
            >
              <Bell size={16} />
              Alertas ({alertas.length})
            </button>

            <button
              type="button"
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
              type="button"
              onClick={() => setFeedback(null)}
              className="text-[#64748B] hover:text-[#0F172A] bg-transparent border-0 cursor-pointer"
            >
              <X size={16} />
            </button>
          </div>
        )}

        <KpiCards totalPolizas={polizas.length} totalPlataforma={totalPlataforma} />

        {activeSection === 'polizas' ? (
          <PolizasTable
            polizas={polizasFiltradas}
            totalCount={polizas.length}
            loading={loading}
            search={search}
            onSearchChange={setSearch}
            aseguradoraFilter={aseguradoraFilter}
            onAseguradoraChange={setAseguradoraFilter}
            aseguradoras={aseguradoras}
            tipoVehiculoFilter={tipoVehiculoFilter}
            onTipoVehiculoChange={setTipoVehiculoFilter}
            tiposVehiculo={tiposVehiculo}
            vigenciaFilter={vigenciaFilter}
            onVigenciaChange={setVigenciaFilter}
            onClearFilters={handleClearFilters}
            onView={(p) => setViewingPoliza(p)}
            onEdit={(p) => setEditingPoliza(p)}
            onDelete={handleEliminar}
          />
        ) : (
          <section className="bg-white border border-[#E2E8F0] rounded-xl p-6">
            <div className="text-center mb-5">
              <h2 className="text-lg font-bold m-0 font-headline">
                Alertas y recordatorios de vencimiento
              </h2>
              <p className="text-xs text-[#64748B] mt-1 mb-0">
                Ventanas de notificación configuradas: 1 mes, 5 días, 1 día y mismo día.
              </p>
            </div>

            {alertas.length === 0 ? (
              <p className="text-sm text-[#64748B] text-center py-6 m-0">
                No hay alertas pendientes en este momento.
              </p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                {alertas.map((alerta) => {
                  const key = `${alerta.polizaId}-${alerta.tipoRecordatorio}`;
                  const waLink =
                    alerta.whatsappUrl || buildWhatsappLink(alerta.telefono, alerta.mensajeAlerta);

                  return (
                    <div
                      key={key}
                      className="p-4 rounded-xl border border-[#E2E8F0] bg-[#F8FAFC] flex flex-col justify-between gap-3"
                    >
                      <div>
                        <div className="flex items-center justify-between gap-2 mb-1.5">
                          <span className="font-bold text-base font-headline">{alerta.placa}</span>
                          <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-[#0F172A]">
                            {alerta.etiqueta}
                          </span>
                        </div>
                        <div className="text-sm font-medium">{alerta.propietario || 'Cliente'}</div>
                        <div className="text-xs text-[#64748B] mt-0.5">
                          Vence: <strong>{alerta.fechaVencimiento}</strong> · Programada:{' '}
                          {alerta.fechaProgramada} ({alerta.diasTranscurridos} días)
                        </div>
                        <div className="text-xs text-[#64748B] mt-1">
                          Email: {alerta.enviadoEmail ? 'Enviado' : 'Pendiente'} · WhatsApp:{' '}
                          {alerta.enviadoWhatsapp ? 'Enviado' : 'Pendiente'}
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center justify-center gap-2 pt-2 border-t border-[#E2E8F0]">
                        {waLink && (
                          <a
                            href={waLink}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#16A34A] hover:bg-[#15803D] text-white text-xs font-semibold"
                            style={{ textDecoration: 'none' }}
                          >
                            <ExternalLink size={13} />
                            Enviar WhatsApp
                          </a>
                        )}

                        {!alerta.enviadoWhatsapp && (
                          <button
                            type="button"
                            disabled={markingKey === key}
                            onClick={() =>
                              handleMarkWhatsappSent(alerta.polizaId, alerta.tipoRecordatorio)
                            }
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#E2E8F0] bg-white hover:bg-[#F1F5F9] text-xs font-semibold text-[#0F172A] cursor-pointer"
                          >
                            <CheckCircle2 size={13} className="text-[#16A34A]" />
                            {markingKey === key ? 'Guardando...' : 'Marcar enviado'}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        )}
      </main>

      <SubirPdfModal
        isOpen={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onUpload={handleUploadPdf}
      />

      <DetallePolizaModal
        poliza={viewingPoliza}
        alertas={alertas}
        onClose={() => setViewingPoliza(null)}
        onEdit={(p) => setEditingPoliza(p)}
        onMarkWhatsappSent={handleMarkWhatsappSent}
      />

      <EditarPolizaModal
        poliza={editingPoliza}
        onClose={() => setEditingPoliza(null)}
        onSave={handleGuardarEdicion}
      />
    </div>
  );
}