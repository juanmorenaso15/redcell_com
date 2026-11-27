import { Bell, LogOut, ShieldCheck } from 'lucide-react';

interface SidebarProps {
  alertasCount: number;
  onLogout: () => void;
}

export function Sidebar({ alertasCount, onLogout }: SidebarProps) {
  return (
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
              {alertasCount}
            </span>
          </div>
        </nav>
      </div>

      <button
        type="button"
        onClick={onLogout}
        className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-[#64748B] hover:text-[#DC2626] hover:bg-red-50 cursor-pointer border-0 bg-transparent"
      >
        <LogOut size={17} />
        Cerrar sesión
      </button>
    </aside>
  );
}