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
    <aside className="w-64 shrink-0 bg-white border-r border-[#E2E8F0] flex-col justify-between p-5 hidden md:flex">
      <div>
        <div className="flex items-center gap-2.5 pb-6 border-b border-[#E2E8F0]">
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
            <div className="text-[11px] text-[#64748B]">Armenia · Quindío</div>
          </div>
        </div>

        <nav className="mt-6 space-y-1.5">
          <button
            type="button"
            onClick={() => onSelectSection('polizas')}
            className={
              activeSection === 'polizas'
                ? 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg bg-red-50 text-[#DC2626] font-semibold text-sm cursor-pointer border-0 text-left'
                : 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left'
            }
          >
            <span className="flex items-center gap-2.5">
              <ShieldCheck size={18} className="shrink-0" />
              Base de datos SOAT
            </span>
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('alertas')}
            className={
              activeSection === 'alertas'
                ? 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg bg-amber-100 text-[#0F172A] font-semibold text-sm cursor-pointer border-0 text-left'
                : 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left'
            }
          >
            <span className="flex items-center gap-2.5">
              <Bell size={18} className="shrink-0" />
              Alertas pendientes
            </span>
            <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-amber-100 text-[#D97706]">
              {alertasCount}
            </span>
          </button>

          <button
            type="button"
            onClick={() => onSelectSection('tarifas')}
            className={
              activeSection === 'tarifas'
                ? 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg bg-red-50 text-[#16A34A] font-semibold text-sm cursor-pointer border-0 text-left'
                : 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left'
            }
          >
            <span className="flex items-center gap-2.5">
              <Coins size={18} className="shrink-0" />
              Uso de plataforma
            </span>
            {tarifaActivaValor != null && (
              <span className="text-xs font-semibold text-[#16A34A] whitespace-nowrap">
                ${Math.round(tarifaActivaValor).toLocaleString('es-CO')}
              </span>
            )}
          </button>

          <button
  type="button"
  onClick={() => onSelectSection('usuarios')}
  className={
    activeSection === 'usuarios'
      ? 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg bg-red-50 text-[#DC2626] font-semibold text-sm cursor-pointer border-0 text-left'
      : 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left'
  }
>
  <span className="flex items-center gap-2.5">
    <UserPlus size={18} className="shrink-0" />
    Usuarios
  </span>
</button>

          <button
            type="button"
            onClick={() => onSelectSection('configuracion')}
            className={
              activeSection === 'configuracion'
                ? 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg bg-[#F1F5F9] text-[#0F172A] font-semibold text-sm cursor-pointer border-0 text-left'
                : 'w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#0F172A] text-sm cursor-pointer border-0 bg-transparent text-left'
            }
          >
            <span className="flex items-center gap-2.5">
              <Settings size={18} className="shrink-0" />
              Configuración
            </span>
          </button>
        </nav>
      </div>

      <button
        type="button"
        onClick={onLogout}
        className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-[#64748B] hover:text-[#DC2626] hover:bg-red-50 cursor-pointer border-0 bg-transparent"
      >
        <LogOut size={17} className="shrink-0" />
        Cerrar sesión
      </button>
    </aside>
  );
}