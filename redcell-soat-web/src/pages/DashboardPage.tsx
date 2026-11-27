import { useCallback, useEffect, useMemo, useState } from 'react';
import { Bell, CheckCircle2, Coins, ExternalLink, LogOut, Plus, ShieldCheck, X } from 'lucide-react';
import { ConfiguracionSection } from '../components/configuracion/ConfiguracionSection';
import { Sidebar, type SidebarSection } from '../components/layout/Sidebar';
import {
  construirMensajeWhatsappConEmojis,
  DetallePolizaModal,
} from '../components/modals/DetallePolizaModal';
import { EditarPolizaModal } from '../components/modals/EditarPolizaModal';
import { SubirPdfModal } from '../components/modals/SubirPdfModal';
import { KpiCards } from '../components/polizas/KpiCards';
import { PolizasTable, type EstadoVigenciaFilter } from '../components/polizas/PolizasTable';
import { UsoPlataformaSection } from '../components/tarifas/UsoPlataformaSection';
import { api } from '../services/api';
import type {
  ActualizarPerfilDTO,
  AlertaRecordatorio,
  ConfiguracionTarifa,
  PerfilUsuario,
  Poliza,
  PolizaUpdateDTO,
} from '../types/soat';
import { alertasSwal } from '../utils/alerts';
import { obtenerUsuarioSesion } from '../utils/auth';
import { buildWhatsappLink, formatCOP } from '../utils/format';

interface DashboardPageProps {
  onUnauthorized: () => void;
}

export function DashboardPage({ onUnauthorized }: DashboardPageProps) {
  const [polizas, setPolizas] = useState<Poliza[]>([]);
  const [alertas, setAlertas] = useState<AlertaRecordatorio[]>([]);
  const [tarifas, setTarifas] = useState<ConfiguracionTarifa[]>([]);
  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingPerfil, setLoadingPerfil] = useState(false);
  const [sesionTick, setSesionTick] = useState(0);
  const [search, setSearch] = useState('');
  const [aseguradoraFilter, setAseguradoraFilter] = useState('TODAS');
  const [tipoVehiculoFilter, setTipoVehiculoFilter] = useState('TODOS');
  const [vigenciaFilter, setVigenciaFilter] = useState<EstadoVigenciaFilter>('TODOS');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<SidebarSection>('polizas');

  const [uploadOpen, setUploadOpen] = useState(false);
  const [viewingPoliza, setViewingPoliza] = useState<Poliza | null>(null);
  const [editingPoliza, setEditingPoliza] = useState<Poliza | null>(null);
  const [markingKey, setMarkingKey] = useState<string | null>(null);

  const usuario = useMemo(() => obtenerUsuarioSesion(), [sesionTick]);

  const cargarDatos = useCallback(async () => {
    setLoading(true);
    setLoadingPerfil(true);
    try {
      const [listaPolizas, listaAlertas, listaTarifas, perfilUsuario] = await Promise.all([
        api.listarPolizas(),
        api.obtenerAlertas().catch(() => []),
        api.listarTarifas().catch(() => []),
        api.obtenerPerfil().catch(() => null),
      ]);
      setPolizas(listaPolizas);
      setAlertas(listaAlertas);
      setTarifas(listaTarifas);
      if (perfilUsuario) {
        setPerfil(perfilUsuario);
        setSesionTick((t) => t + 1);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error cargando datos';
      if (msg.includes('401') || msg.includes('403') || msg.toLowerCase().includes('token')) {
        onUnauthorized();
      } else {
        setFeedback(msg);
      }
    } finally {
      setLoading(false);
      setLoadingPerfil(false);
    }
  }, [onUnauthorized]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const tarifaActiva = useMemo(() => tarifas.find((t) => Boolean(t.activo)), [tarifas]);

  const handleUploadPdf = async (file: File) => {
    const res = await api.subirPdf(file);
    await alertasSwal.exito('PDF procesado', res.message || 'Póliza cargada correctamente.');
    await cargarDatos();
  };

  const handleGuardarEdicion = async (id: number, dto: PolizaUpdateDTO) => {
    const actualizada = await api.actualizarPoliza(id, dto);
    await alertasSwal.exito(
      `Póliza ${actualizada.placa} actualizada`,
      `Uso de plataforma: ${formatCOP(actualizada.usoPlataforma)} · Total: ${formatCOP(actualizada.totalPagar)}`
    );
    await cargarDatos();
  };

  const handleEliminar = async (poliza: Poliza) => {
    const confirmado = await alertasSwal.confirmar(
      `¿Eliminar póliza ${poliza.placa}?`,
      `Se eliminará permanentemente el registro del SOAT de la placa ${poliza.placa}.`,
      'Sí, eliminar',
      true
    );
    if (!confirmado) return;

    try {
      await api.eliminarPoliza(poliza.id);
      await alertasSwal.exito('Póliza eliminada', `Se eliminó la póliza ${poliza.placa}.`);
      await cargarDatos();
    } catch (err: unknown) {
      await alertasSwal.error(
        'No se pudo eliminar',
        err instanceof Error ? err.message : 'Error al eliminar póliza'
      );
    }
  };

  const handleMarkWhatsappSent = async (polizaId: number, tipo: string) => {
    const key = `${polizaId}-${tipo}`;
    setMarkingKey(key);
    try {
      await api.marcarWhatsappEnviado(polizaId, tipo);
      await alertasSwal.toast('success', `Recordatorio (${tipo}) marcado como enviado`);
      await cargarDatos();
    } catch (err: unknown) {
      await alertasSwal.error(
        'Error al actualizar',
        err instanceof Error ? err.message : 'Error al marcar WhatsApp como enviado'
      );
    } finally {
      setMarkingKey(null);
    }
  };

  const handleEnviarEmailManual = async (poliza: Poliza) => {
    if (!poliza.email?.trim()) {
      await alertasSwal.advertencia(
        'Sin correo registrado',
        `La póliza ${poliza.placa} no tiene correo electrónico registrado.`
      );
      return;
    }
    try {
      await api.notificarPoliza(poliza.id, {
        email: poliza.email.trim(),
        gmail: true,
        whatsapp: false,
      });
      await alertasSwal.exito(
        'Correo encolado',
        `Recordatorio enviado a ${poliza.email.trim()} (Placa ${poliza.placa}).`
      );
    } catch (err: unknown) {
      await alertasSwal.error(
        'Error al enviar correo',
        err instanceof Error ? err.message : 'Error al encolar correo'
      );
    }
  };

  const handleSavePerfil = async (dto: ActualizarPerfilDTO) => {
    const actualizado = await api.actualizarPerfil(dto);
    setPerfil(actualizado);
    setSesionTick((t) => t + 1);
  };

  const handleCreateTarifa = async (nueva: Omit<ConfiguracionTarifa, 'id'>) => {
    await api.crearTarifa(nueva);
    await alertasSwal.exito(
      'Tarifa creada',
      `"${nueva.nombre}" guardada con valor de ${formatCOP(nueva.valor)}.`
    );
    await cargarDatos();
  };

  const handleUpdateTarifa = async (id: number, datos: Partial<ConfiguracionTarifa>) => {
    await api.actualizarTarifa(id, datos);
    await alertasSwal.exito('Tarifa actualizada', 'La configuración de uso de plataforma se guardó.');
    await cargarDatos();
  };

  const handleDeleteTarifa = async (id: number) => {
    const confirmado = await alertasSwal.confirmar(
      '¿Eliminar tarifa?',
      'Esta acción eliminará la tarifa seleccionada.',
      'Sí, eliminar',
      true
    );
    if (!confirmado) return;

    await api.eliminarTarifa(id);
    await alertasSwal.exito('Tarifa eliminada', 'La tarifa fue eliminada correctamente.');
    await cargarDatos();
  };

  const handleApplyTarifaToPolizas = async (valor: number, soloSinTarifa: boolean) => {
    const objetivos = soloSinTarifa
      ? polizas.filter((p) => Number(p.usoPlataforma || 0) === 0)
      : polizas;

    if (objetivos.length === 0) {
      await alertasSwal.advertencia(
        'Sin pólizas pendientes',
        'No hay pólizas con uso de plataforma en $0.'
      );
      return;
    }

    await Promise.all(
      objetivos.map((p) =>
        api.actualizarPoliza(p.id, {
          placa: p.placa,
          tipoVehiculo: p.tipoVehiculo,
          marcaLinea: p.marcaLinea,
          cilindraje: p.cilindraje,
          aseguradora: p.aseguradora,
          edadVehiculo: p.edadVehiculo,
          capacidadToneladas: p.capacidadToneladas,
          desde: p.desde,
          hasta: p.hasta,
          propietario: p.propietario,
          tipoDocumento: p.tipoDocumento,
          documento: p.documento,
          usoPlataforma: valor,
          telefono: p.telefono ?? '',
          email: p.email ?? '',
        })
      )
    );

    await alertasSwal.exito(
      'Tarifa aplicada',
      `Uso de plataforma (${formatCOP(valor)}) aplicado a ${objetivos.length} póliza(s).`
    );
    await cargarDatos();
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
    <div className="min-h-screen flex flex-col md:flex-row bg-[#F8FAFC] text-[#0F172A] w-full">
      <div className="flex md:hidden items-center justify-between gap-3 px-4 py-3 bg-white border-b border-[#E2E8F0]">
        <div className="flex items-center gap-2 min-w-0">
          <img
            src="/logo-redcell.png"
            alt="RedCell"
            className="h-8 w-auto object-contain shrink-0"
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).style.display = 'none';
            }}
          />
          <div className="min-w-0">
            <div className="font-bold text-sm leading-tight font-headline truncate">
              RedCell SOAT
            </div>
            <div className="text-[11px] text-[#64748B] truncate">
              {usuario.nombre} · {usuario.username}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onUnauthorized}
          className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-[#E2E8F0] bg-white text-xs font-semibold text-[#64748B] hover:text-[#DC2626] cursor-pointer shrink-0"
        >
          <LogOut size={14} />
          Salir
        </button>
      </div>

      <Sidebar
        activeSection={activeSection}
        onSelectSection={setActiveSection}
        alertasCount={alertas.length}
        tarifaActivaValor={tarifaActiva?.valor}
        onLogout={onUnauthorized}
      />

      <main className="flex-1 min-w-0 p-4 sm:p-6 md:p-8 max-w-none w-full">
        <header className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
          <div className="min-w-0">
            <h1 className="text-xl sm:text-2xl font-bold text-[#0F172A] m-0 font-headline">
              {activeSection === 'configuracion' ? 'Configuración' : 'Base de datos SOAT'}
            </h1>
            <p className="text-xs sm:text-sm text-[#64748B] mt-1 mb-0">
              {activeSection === 'configuracion'
                ? 'Gestiona tu perfil de usuario y parámetros del sistema.'
                : 'Consulta, carga PDFs, edita pólizas y gestiona uso de plataforma y recordatorios.'}
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-between sm:justify-end gap-3 shrink-0">
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setActiveSection('polizas')}
                className={
                  activeSection === 'polizas'
                    ? 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#0F172A] bg-[#0F172A] text-white'
                    : 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'
                }
              >
                <ShieldCheck size={16} className="shrink-0" />
                Pólizas ({polizas.length})
              </button>

              <button
                type="button"
                onClick={() => setActiveSection('alertas')}
                className={
                  activeSection === 'alertas'
                    ? 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#D97706] bg-[#D97706] text-white'
                    : 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'
                }
              >
                <Bell size={16} className="shrink-0" />
                Alertas ({alertas.length})
              </button>

              <button
                type="button"
                onClick={() => setActiveSection('tarifas')}
                className={
                  activeSection === 'tarifas'
                    ? 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#16A34A] bg-[#16A34A] text-white'
                    : 'inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'
                }
              >
                <Coins size={16} className="shrink-0" />
                Uso de plataforma
              </button>

              <button
                type="button"
                onClick={() => setUploadOpen(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer border-0"
              >
                <Plus size={16} className="shrink-0" />
                Subir SOAT PDF
              </button>
            </div>

            <button
              type="button"
              onClick={() => setActiveSection('configuracion')}
              title="Abrir configuración de perfil"
              className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] shadow-sm shrink-0 cursor-pointer text-left"
            >
              <img
                src="/logo-redcell.png"
                alt={usuario.nombre}
                className="h-8 w-8 rounded-full object-contain bg-[#F8FAFC] p-1 border border-[#E2E8F0] shrink-0"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).style.display = 'none';
                }}
              />
              <div className="leading-tight whitespace-nowrap">
                <div className="text-xs font-bold text-[#0F172A]">{usuario.nombre}</div>
                <div className="text-[11px] text-[#64748B]">
                  {usuario.rol} · <span className="font-medium text-[#0F172A]">{usuario.username}</span>
                </div>
              </div>
            </button>
          </div>
        </header>

        {feedback && (
          <div className="mb-5 px-4 py-3 rounded-xl bg-white border border-[#E2E8F0] flex items-center justify-between gap-3 text-sm">
            <span>{feedback}</span>
            <button
              type="button"
              onClick={() => setFeedback(null)}
              className="text-[#64748B] hover:text-[#0F172A] bg-transparent border-0 cursor-pointer shrink-0"
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
        ) : activeSection === 'tarifas' ? (
          <UsoPlataformaSection
            tarifas={tarifas}
            polizas={polizas}
            loading={loading}
            onCreateTarifa={handleCreateTarifa}
            onUpdateTarifa={handleUpdateTarifa}
            onDeleteTarifa={handleDeleteTarifa}
            onApplyTarifaToPolizas={handleApplyTarifaToPolizas}
          />
        ) : activeSection === 'configuracion' ? (
          <ConfiguracionSection
            perfil={perfil}
            loadingPerfil={loadingPerfil}
            tarifaActivaValor={tarifaActiva?.valor}
            onSavePerfil={handleSavePerfil}
            onIrATarifas={() => setActiveSection('tarifas')}
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
                  const polizaAsociada = polizas.find((p) => p.id === alerta.polizaId);
                  const mensajeAlertaEmojis = polizaAsociada
                    ? construirMensajeWhatsappConEmojis(polizaAsociada, alerta.etiqueta)
                    : alerta.mensajeAlerta;
                  const waLink = buildWhatsappLink(alerta.telefono, mensajeAlertaEmojis);

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
                            onClick={() => {
                              navigator.clipboard?.writeText(mensajeAlertaEmojis).catch(() => {});
                            }}
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
        onSendEmail={handleEnviarEmailManual}
      />

      <EditarPolizaModal
        poliza={editingPoliza}
        onClose={() => setEditingPoliza(null)}
        onSave={handleGuardarEdicion}
      />
    </div>
  );
}