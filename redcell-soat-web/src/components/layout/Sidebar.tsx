import { Bell, Coins, LogOut, Settings, ShieldCheck, UserPlus } from 'lucide-react';

export type SidebarSection = 'polizas' | 'alertas' | 'tarifas' | 'usuarios' | 'configuracion';

interface SidebarProps {
  activeSection: SidebarSection;
  onSelectSection: (section: SidebarSection) => void;
  alertasCount: number;
  tarifaActivaValor?: number;
  onLogout: () => void;
}

export function Sidebar({
  activeSection,
  onSelectSection,
  alertasCount,
  tarifaActivaValor,
  onLogout,
}: SidebarProps) {
  return (
    <aside
      style={{
        position: 'fixed',
        top: '1.25rem',
        left: '1.25rem',
        width: '16.5rem',
        height: 'calc(100vh - 2.5rem)',
        padding: '1.75rem 1.35rem',
        boxSizing: 'border-box',
        zIndex: 30,
      }}
      className="bg-white border border-[#E2E8F0] rounded-2xl shadow-sm flex flex-col justify-between overflow-y-auto"
    >
      <div className="min-h-0 w-full">
        <div className="flex items-center gap-3 pb-6 mb-2 border-b border-[#E2E8F0] px-1">
          <img
            src="/logo-redcell.png"
            alt="RedCell"
            className="h-9 w-auto object-contain shrink-0"
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).style.display = 'none';
            }}
          />
          <div className="min-w-0">
            <div className="font-bold text-base leading-tight font-headline">RedCell SOAT</div>
            <div className="text-[11px] text-[#64748B] mt-0.5">Armenia · Quindío</div>
          </div>
        </div>

        <nav className="mt-5 space-y-2.5">
          <button
            type="button"
            onClick={() => onSelectSection('polizas')}
            className={
              activeSection === 'polizas'
                ? 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl bg-red-50 text-[#DC2626] font-semibold text-sm cursor-pointer border-0 text-left transition-colors'
                : 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left transition-colors'
            }
          >
            <span className="flex items-center gap-3 min-w-0">
              <ShieldCheck size={18} className="shrink-0" />
              <span className="truncate">Base de datos SOAT</span>
            </span>
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('alertas')}
            className={
              activeSection === 'alertas'
                ? 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl bg-amber-100 text-[#0F172A] font-semibold text-sm cursor-pointer border-0 text-left transition-colors'
                : 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left transition-colors'
            }
          >
            <span className="flex items-center gap-3 min-w-0">
              <Bell size={18} className="shrink-0" />
              <span className="truncate">Alertas pendientes</span>
            </span>
            <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-amber-100 text-[#D97706] shrink-0 mr-0.5">
              {alertasCount}
            </span>
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('tarifas')}
            className={
              activeSection === 'tarifas'
                ? 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl bg-red-50 text-[#16A34A] font-semibold text-sm cursor-pointer border-0 text-left transition-colors'
                : 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left transition-colors'
            }
          >
            <span className="flex items-center gap-3 min-w-0">
              <Coins size={18} className="shrink-0" />
              <span className="truncate">Uso de plataforma</span>
            </span>
            {tarifaActivaValor != null && (
              <span className="text-xs font-semibold text-[#16A34A] whitespace-nowrap shrink-0 mr-0.5">
                ${Math.round(tarifaActivaValor).toLocaleString('es-CO')}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('usuarios')}
            className={
              activeSection === 'usuarios'
                ? 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl bg-red-50 text-[#DC2626] font-semibold text-sm cursor-pointer border-0 text-left transition-colors'
                : 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left transition-colors'
            }
          >
            <span className="flex items-center gap-3 min-w-0">
              <UserPlus size={18} className="shrink-0" />
              <span className="truncate">Usuarios</span>
            </span>
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('configuracion')}
            className={
              activeSection === 'configuracion'
                ? 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl bg-[#F1F5F9] text-[#0F172A] font-semibold text-sm cursor-pointer border-0 text-left transition-colors'
                : 'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left transition-colors'
            }
          >
            <span className="flex items-center gap-3 min-w-0">
              <Settings size={18} className="shrink-0" />
              <span className="truncate">Configuración</span>
            </span>
          </button>
        </nav>
      </div>

      <div className="pt-4 border-t border-[#E2E8F0]/60">
        <button
          type="button"
          onClick={onLogout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-[#64748B] hover:text-[#DC2626] hover:bg-red-50 cursor-pointer border-0 bg-transparent text-left shrink-0 transition-colors"
        >
          <LogOut size={18} className="shrink-0" />
          <span>Cerrar sesión</span>
        </button>
      </div>
    </aside>
  );
}