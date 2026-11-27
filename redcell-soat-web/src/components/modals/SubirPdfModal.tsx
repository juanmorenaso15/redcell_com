import { useState, type FormEvent } from 'react';
import { FileUp, X } from 'lucide-react';

interface SubirPdfModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpload: (file: File) => Promise<void>;
}

export function SubirPdfModal({ isOpen, onClose, onUpload }: SubirPdfModalProps) {
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!pdfFile) return;
    setUploading(true);
    try {
      await onUpload(pdfFile);
      setPdfFile(null);
      onClose();
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold m-0 font-headline">Subir SOAT en PDF</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[#64748B] bg-transparent border-0 cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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
              onClick={onClose}
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
  );
}