import { useEffect, useState, type FormEvent } from 'react';
import { X } from 'lucide-react';
import type { Poliza, PolizaUpdateDTO } from '../../types/soat';
import { formatCOP } from '../../utils/format';

interface EditarPolizaModalProps {
  poliza: Poliza | null;
  onClose: () => void;
  onSave: (id: number, dto: PolizaUpdateDTO) => Promise<void>;
}

interface FormState {
  placa: string;
  tipoVehiculo: string;
  marcaLinea: string;
  cilindraje: string;
  aseguradora: string;
  edadVehiculo: string;
  capacidadToneladas: string;
  desde: string;
  hasta: string;
  propietario: string;
  tipoDocumento: string;
  documento: string;
  usoPlataforma: string;
  telefono: string;
  email: string;
}

const initialForm: FormState = {
  placa: '',
  tipoVehiculo: '',
  marcaLinea: '',
  cilindraje: '0',
  aseguradora: '',
  edadVehiculo: '0',
  capacidadToneladas: '0',
  desde: '',
  hasta: '',
  propietario: '',
  tipoDocumento: 'CC',
  documento: '0',
  usoPlataforma: '15000',
  telefono: '',
  email: '',
};

export function EditarPolizaModal({ poliza, onClose, onSave }: EditarPolizaModalProps) {
  const [form, setForm] = useState<FormState>(initialForm);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (poliza) {
      setForm({
        placa: poliza.placa ?? '',
        tipoVehiculo: poliza.tipoVehiculo ?? '',
        marcaLinea: poliza.marcaLinea ?? '',
        cilindraje: String(poliza.cilindraje ?? 0),
        aseguradora: poliza.aseguradora ?? '',
        edadVehiculo: String(poliza.edadVehiculo ?? 0),
        capacidadToneladas: String(poliza.capacidadToneladas ?? 0),
        desde: poliza.desde ?? '',
        hasta: poliza.hasta ?? '',
        propietario: poliza.propietario ?? '',
        tipoDocumento: poliza.tipoDocumento ?? 'CC',
        documento: String(poliza.documento ?? 0),
        usoPlataforma: String(poliza.usoPlataforma ?? 15000),
        telefono: poliza.telefono ?? '',
        email: poliza.email ?? '',
      });
    }
  }, [poliza]);

  if (!poliza) return null;

  const setField = (key: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const dto: PolizaUpdateDTO = {
        placa: form.placa.trim().toUpperCase(),
        tipoVehiculo: form.tipoVehiculo.trim(),
        marcaLinea: form.marcaLinea.trim(),
        cilindraje: Math.max(0, Number(form.cilindraje || 0)),
        aseguradora: form.aseguradora.trim(),
        edadVehiculo: Math.max(0, Number(form.edadVehiculo || 0)),
        capacidadToneladas: Math.max(0, Number(form.capacidadToneladas || 0)),
        desde: form.desde.trim(),
        hasta: form.hasta.trim(),
        propietario: form.propietario.trim(),
        tipoDocumento: form.tipoDocumento.trim(),
        documento: Math.max(0, Number(form.documento || 0)),
        usoPlataforma: Math.max(0, Number(form.usoPlataforma || 0)),
        telefono: form.telefono.trim(),
        email: form.email.trim(),
      };
      await onSave(poliza.id, dto);
      onClose();
    } finally {
      setSaving(false);
    }
  };

  const totalSimulado =
    Number(poliza.precioSoat || 0) + Math.max(0, Number(form.usoPlataforma || 0));

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
      <div
        className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-2xl p-6 shadow-sm mx-auto"
        style={{ maxHeight: '90vh', overflowY: 'auto' }}
      >
        {/* Encabezado centrado */}
        <div className="relative pb-4 border-b border-[#E2E8F0] text-center mb-4">
          <button
            type="button"
            onClick={onClose}
            className="text-[#64748B] bg-transparent border-0 cursor-pointer"
            style={{ position: 'absolute', right: 0, top: 0 }}
          >
            <X size={18} />
          </button>
          <h2 className="text-lg font-bold m-0 font-headline">
            Editar SOAT completo · {poliza.placa}
          </h2>
          <p className="text-xs text-[#64748B] mt-1 mb-0">
            Modifica los datos del vehículo, propietario, vigencia o uso de plataforma.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Simulación en vivo */}
          <div className="p-3.5 rounded-xl bg-[#F8FAFC] border border-[#E2E8F0] grid grid-cols-1 sm:grid-cols-3 gap-2 text-center">
            <div>
              <div className="text-[11px] text-[#64748B]">Valor SOAT</div>
              <div className="text-sm font-bold">{formatCOP(poliza.precioSoat)}</div>
            </div>
            <div>
              <div className="text-[11px] text-[#64748B]">Uso de plataforma</div>
              <div className="text-sm font-bold text-[#16A34A]">
                {formatCOP(Number(form.usoPlataforma || 0))}
              </div>
            </div>
            <div>
              <div className="text-[11px] text-[#DC2626] font-semibold">Total estimado</div>
              <div className="text-sm font-bold text-[#DC2626]">{formatCOP(totalSimulado)}</div>
            </div>
          </div>

          {/* Vehículo */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold mb-1">Placa</label>
              <input
                type="text"
                required
                value={form.placa}
                onChange={(e) => setField('placa', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm uppercase font-semibold"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Aseguradora</label>
              <input
                type="text"
                value={form.aseguradora}
                onChange={(e) => setField('aseguradora', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Tipo / Clase de vehículo</label>
              <input
                type="text"
                value={form.tipoVehiculo}
                onChange={(e) => setField('tipoVehiculo', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Marca y línea</label>
              <input
                type="text"
                value={form.marcaLinea}
                onChange={(e) => setField('marcaLinea', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Cilindraje (cc)</label>
              <input
                type="number"
                min={0}
                value={form.cilindraje}
                onChange={(e) => setField('cilindraje', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold mb-1">Edad (años)</label>
                <input
                  type="number"
                  min={0}
                  value={form.edadVehiculo}
                  onChange={(e) => setField('edadVehiculo', e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">Capacidad (t)</label>
                <input
                  type="number"
                  min={0}
                  step="0.1"
                  value={form.capacidadToneladas}
                  onChange={(e) => setField('capacidadToneladas', e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>
            </div>
          </div>

          {/* Propietario y vigencia */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-[#E2E8F0]">
            <div>
              <label className="block text-xs font-semibold mb-1">Propietario</label>
              <input
                type="text"
                value={form.propietario}
                onChange={(e) => setField('propietario', e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold mb-1">Tipo doc.</label>
                <input
                  type="text"
                  value={form.tipoDocumento}
                  onChange={(e) => setField('tipoDocumento', e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">Documento</label>
                <input
                  type="number"
                  min={0}
                  value={form.documento}
                  onChange={(e) => setField('documento', e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Vigencia desde</label>
              <input
                type="text"
                value={form.desde}
                onChange={(e) => setField('desde', e.target.value)}
                placeholder="YYYY-MM-DD"
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Vigencia hasta</label>
              <input
                type="text"
                value={form.hasta}
                onChange={(e) => setField('hasta', e.target.value)}
                placeholder="YYYY-MM-DD"
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Teléfono WhatsApp</label>
              <input
                type="text"
                value={form.telefono}
                onChange={(e) => setField('telefono', e.target.value)}
                placeholder="3117034142"
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1">Correo electrónico</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setField('email', e.target.value)}
                placeholder="cliente@correo.com"
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm"
              />
            </div>
          </div>

          {/* Uso de plataforma */}
          <div className="pt-2 border-t border-[#E2E8F0]">
            <label className="block text-xs font-semibold mb-1">Uso de plataforma (COP)</label>
            <input
              type="number"
              min={0}
              step={100}
              required
              value={form.usoPlataforma}
              onChange={(e) => setField('usoPlataforma', e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm font-semibold"
            />
          </div>

          <div className="flex justify-center gap-2.5 pt-3 border-t border-[#E2E8F0]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-lg border border-[#E2E8F0] text-sm bg-white cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 rounded-lg bg-[#16A34A] hover:bg-[#15803D] text-white text-sm font-semibold border-0 cursor-pointer disabled:opacity-60"
            >
              {saving ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}