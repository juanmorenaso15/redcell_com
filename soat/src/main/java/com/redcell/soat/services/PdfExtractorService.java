package com.redcell.soat.services;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.redcell.soat.entity.Poliza;

@Service
public class PdfExtractorService {

    public Poliza extraerDatosPdf(MultipartFile archivo) throws IOException {
        try (InputStream is = archivo.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String texto = stripper.getText(document);
            String textoLimpio = limpiarTexto(texto);

            Poliza poliza = new Poliza();
            String textoMayus = textoLimpio.toUpperCase();

            if (textoMayus.contains("PREVISORA")) {
                poliza.setAseguradora("LA PREVISORA S.A.");
                extraerPrevisora(textoLimpio, poliza);
            } else if (textoMayus.contains("AXA") || textoMayus.contains("COLPATRIA") || textoMayus.contains("SOAT PÓLIZA") || textoMayus.contains("SOAT POLIZA")) {
                poliza.setAseguradora("AXA COLPATRIA");
                extraerAxaColpatria(textoLimpio, poliza);
            } else {
                throw new RuntimeException("Aseguradora no reconocida.");
            }

            return poliza;
        }
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("\\r?\\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
    }

    private void extraerPrevisora(String texto, Poliza poliza) {
        String upper = texto.toUpperCase();

        String placa = extraerPlaca(upper);
        if (placa != null) {
            poliza.setPlaca(placa);
        }

        extraerTipoVehiculo(upper, poliza);

        String marca = null;
        String linea = null;

        Pattern patronMarca = Pattern.compile("\\bMARCA\\b\\s*:?\\s*([A-Z0-9\\-]{2,20})\\b");
        Matcher matcherMarca = patronMarca.matcher(upper);
        if (matcherMarca.find()) {
            marca = matcherMarca.group(1);
        }

        Pattern patronLinea = Pattern.compile(
                "(?:LINEA\\s+VEH[IÍ]CULO|VEH[IÍ]CULO)\\s*:?\\s*([A-Z0-9\\s\\-]{2,30}?)(?=\\s+(?:APELLIDOS|TOMADOR|C[OÓ]DIGO|TARIFA|PRIMA|CILINDRAJE|MODELO|PLACA|CLASE|$))"
        );
        Matcher matcherLinea = patronLinea.matcher(upper);
        if (matcherLinea.find()) {
            String candidata = matcherLinea.group(1).trim();
            if (!candidata.startsWith("MOTOS") && !candidata.startsWith("CAMPEROS") && !candidata.startsWith("AUTOS")) {
                linea = upper.contains("YUAN PLUS EV") ? "YUAN PLUS EV" : candidata;
            }
        }

        if (marca != null && linea != null) {
            poliza.setMarcaLinea(marca + " " + linea);
        } else {
            poliza.setMarcaLinea(marca != null ? marca : linea);
        }

        Integer cilindraje = extraerCilindrajePrevisora(upper);
        if (cilindraje != null) {
            poliza.setCilindraje(cilindraje);
        }

        Integer modeloAnio = extraerModeloPrevisora(upper, cilindraje);

        poliza.setCapacidadToneladas(extraerCapacidadToneladas(upper));

        poliza.setPropietario(extraerNombrePorEtiqueta(upper));

        Pattern patronTipoDoc = Pattern.compile("TIPO\\s+DE\\s*DOCUMENT\\s*O\\s*(?:DEL\\s+TOMADOR)?\\s*:?\\s*\\b(CC|CE|TI|NIT|PA)\\b");
        Matcher matcherTipoDoc = patronTipoDoc.matcher(upper);
        if (matcherTipoDoc.find()) {
            poliza.setTipoDocumento(matcherTipoDoc.group(1));
        }

        Pattern patronDocNum = Pattern.compile("No\\.?\\s*DE\\s*DOCUMENT\\s*O\\s*(?:DEL\\s+TOMADOR)?\\s*:?\\s*\\b(\\d{6,12})\\b");
        Matcher matcherDocNum = patronDocNum.matcher(upper);
        if (matcherDocNum.find()) {
            poliza.setDocumento(Long.parseLong(matcherDocNum.group(1)));
            if (poliza.getTipoDocumento() == null) {
                poliza.setTipoDocumento("CC");
            }
        } else {
            Pattern patronCedula = Pattern.compile("(?<!\\$)\\b([1-9]\\d{6,9})\\b");
            Matcher matcherCedula = patronCedula.matcher(upper);
            while (matcherCedula.find()) {
                String num = matcherCedula.group(1);
                if (num.length() >= 7 && num.length() <= 10) {
                    poliza.setDocumento(Long.parseLong(num));
                    if (poliza.getTipoDocumento() == null) {
                        poliza.setTipoDocumento("CC");
                    }
                    break;
                }
            }
        }

        List<LocalDate> fechasIso = extraerFechasIso(upper);
        if (fechasIso.size() >= 3) {
            poliza.setDesde(fechasIso.get(1));
            poliza.setHasta(fechasIso.get(2));
        } else if (fechasIso.size() == 2) {
            poliza.setDesde(fechasIso.get(0));
            poliza.setHasta(fechasIso.get(1));
        } else {
            extraerFechasPorColumnas(upper, poliza);
        }

        poliza.setEdadVehiculo(calcularEdadDesdeModelo(modeloAnio, poliza.getDesde()));
    }

    private void extraerAxaColpatria(String texto, Poliza poliza) {
        String upper = texto.toUpperCase();

        String placa = extraerPlaca(upper);
        if (placa != null) {
            poliza.setPlaca(placa);
        }

        extraerTipoVehiculo(upper, poliza);

        String marca = null;
        String linea = null;

        if (upper.contains("KIA") || upper.contains("ΚΙΑ")) {
            marca = "KIA";
        } else if (upper.contains("TVS")) {
            marca = "TVS";
        } else {
            Pattern patronMarca = Pattern.compile("\\bMARCA\\b\\s*:?\\s*([A-Z]{2,20})\\b");
            Matcher matcherMarca = patronMarca.matcher(upper);
            if (matcherMarca.find()) {
                String candidata = matcherMarca.group(1);
                if (!candidata.equals("VEHÍCULO") && !candidata.equals("VEHICULO") && !candidata.equals("LÍNEA") && !candidata.equals("LINEA")) {
                    marca = candidata;
                }
            }
        }

        if (upper.contains("PICANTO EX")) {
            linea = "PICANTO EX";
        } else if (upper.contains("RAIDER 125 FI")) {
            linea = "RAIDER 125 FI";
        } else {
            Pattern patronLinea = Pattern.compile("(?:LINEAULO|L[IÍ]NEA(?:\\s+VEH[IÍ]CULO)?)\\s*:?\\s*([A-Z0-9\\s\\-]{2,25}?)(?=\\s+(?:No\\.|N°|CILINDRAJE|MODELO|CARROCER[IÍ]A|SERVICIO|PASAJEROS|TOMADOR|$))");
            Matcher matcherLinea = patronLinea.matcher(upper);
            if (matcherLinea.find()) {
                String candidataLinea = matcherLinea.group(1).trim();
                if (!candidataLinea.startsWith("VEHÍCULO") && !candidataLinea.startsWith("VEHICULO")) {
                    linea = candidataLinea;
                }
            }
        }

        if (marca != null && linea != null) {
            poliza.setMarcaLinea(marca + " " + linea);
        } else {
            poliza.setMarcaLinea(marca != null ? marca : linea);
        }

        Integer cilindraje = null;
        Pattern patronCilindraje = Pattern.compile("CILINDRAJE(?:\\s*/\\s*VATIOS)?\\s*:?\\s*(\\d{2,4})");
        Matcher matcherCilindraje = patronCilindraje.matcher(upper);
        if (matcherCilindraje.find()) {
            cilindraje = Integer.parseInt(matcherCilindraje.group(1));
            poliza.setCilindraje(cilindraje);
        } else {
            Pattern patronCilModelo = Pattern.compile("\\b(\\d{3,4})\\s+(?:19|20)\\d{2}\\b");
            Matcher matcherCilModelo = patronCilModelo.matcher(upper);
            if (matcherCilModelo.find()) {
                cilindraje = Integer.parseInt(matcherCilModelo.group(1));
                poliza.setCilindraje(cilindraje);
            }
        }

        poliza.setCapacidadToneladas(extraerCapacidadToneladas(upper));

        poliza.setPropietario(extraerNombrePorEtiqueta(upper));

        Pattern patronDoc = Pattern.compile("(?:No\\.?\\s*DE\\s*DOCUMENTO(?:\\s+DEL\\s+TOMADOR)?|C\\.?C\\.?)\\s*:?\\s*(\\d{6,12})");
        Matcher matcherDoc = patronDoc.matcher(upper);
        if (matcherDoc.find()) {
            poliza.setDocumento(Long.parseLong(matcherDoc.group(1)));
            poliza.setTipoDocumento("CC");
        }

        extraerFechasPorColumnas(upper, poliza);
        if (poliza.getDesde() == null || poliza.getHasta() == null) {
            List<LocalDate> fechasIso = extraerFechasIso(upper);
            if (fechasIso.size() >= 3 && poliza.getDesde() == null && poliza.getHasta() == null) {
                poliza.setDesde(fechasIso.get(1));
                poliza.setHasta(fechasIso.get(2));
            } else if (fechasIso.size() >= 2) {
                if (poliza.getDesde() == null) poliza.setDesde(fechasIso.get(0));
                if (poliza.getHasta() == null) poliza.setHasta(fechasIso.get(1));
            }
        }

        Integer modeloAnio = extraerModeloAxa(upper, cilindraje, poliza.getDesde(), poliza.getHasta());
        poliza.setEdadVehiculo(calcularEdadDesdeModelo(modeloAnio, poliza.getDesde()));
    }

    private Integer extraerModeloAxa(String upper, Integer cilindraje, LocalDate desde, LocalDate hasta) {
        int anioDesde = (desde != null) ? desde.getYear() : -1;
        int anioHasta = (hasta != null) ? hasta.getYear() : -1;

        int idxModelo = upper.indexOf("MODELO");
        if (idxModelo >= 0) {
            String tramo = upper.substring(idxModelo, Math.min(idxModelo + 250, upper.length()));
            Matcher mAnio = Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(tramo);
            while (mAnio.find()) {
                int anio = Integer.parseInt(mAnio.group(1));
                if (anio >= 1970 && anio <= 2030 && anio != anioDesde && anio != anioHasta) {
                    return anio;
                }
            }
        }

        if (cilindraje != null) {
            Matcher mCil = Pattern.compile("\\b" + cilindraje + "\\b.{0,80}?\\b((?:19|20)\\d{2})\\b").matcher(upper);
            while (mCil.find()) {
                int anio = Integer.parseInt(mCil.group(1));
                if (anio >= 1970 && anio <= 2030 && anio != anioDesde && anio != anioHasta) {
                    return anio;
                }
            }
        }

        int corte = upper.length();
        for (String hito : new String[]{"APELLIDOS", "TOMADOR", "TARIFA", "PRIMA SOAT"}) {
            int pos = upper.indexOf(hito);
            if (pos > 0 && pos < corte) {
                corte = pos;
            }
        }
        Matcher mSuperior = Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(upper.substring(0, corte));
        while (mSuperior.find()) {
            int anio = Integer.parseInt(mSuperior.group(1));
            if (anio >= 1970 && anio <= 2025 && anio != anioDesde && anio != anioHasta) {
                return anio;
            }
        }

        return null;
    }

    private String extraerNombrePorEtiqueta(String upper) {
        Pattern pEtiqueta = Pattern.compile(
                "APELLIDOS\\s+Y\\s+NOMBRES(?:\\s+DEL\\s+TOMADOR)?\\s*:?\\s*(.{5,220})"
        );
        Matcher mEtiqueta = pEtiqueta.matcher(upper);
        if (mEtiqueta.find()) {
            String tramo = mEtiqueta.group(1).trim();
            String bloqueNombre = tramo.split("\\s+(?:C[OÓ]DIGO|TEL[EÉ]FONO|TIPO\\s+DE\\s*DOCUMENT|No\\.?\\s*DE\\s*DOCUMENT|CIUDAD|SUCURSAL|\\d{6,12})")[0].trim();
            bloqueNombre = limpiarPrefijoTomador(bloqueNombre);

            Matcher mComa = Pattern.compile("([A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){0,2}\\s*,\\s*[A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){0,2})")
                                   .matcher(bloqueNombre);
            if (mComa.find()) {
                String cand = limpiarPrefijoTomador(normalizarNombre(mComa.group(1)));
                if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                    return cand;
                }
            }

            Matcher mSinComa = Pattern.compile("^([A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){1,3})\\b")
                                      .matcher(bloqueNombre);
            if (mSinComa.find()) {
                String cand = limpiarPrefijoTomador(normalizarNombre(mSinComa.group(1)));
                if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                    return cand;
                }
            }
        }

        int limite = upper.length();
        for (String corte : new String[]{"PRIMA SOAT", "CONTRIBUCION FOSYGA", "CONTRIBUCIÓN FOSYGA", "CLAUSULA", "CLÁUSULA", "AUTORIZO", "SEÑOR USUARIO"}) {
            int pos = upper.indexOf(corte);
            if (pos > 0 && pos < limite) {
                limite = pos;
            }
        }
        String zonaSuperior = upper.substring(0, limite);

        Matcher mGlobal = Pattern.compile("([A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){1,2}\\s*,\\s*[A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){0,2})")
                                 .matcher(zonaSuperior);
        while (mGlobal.find()) {
            String cand = limpiarPrefijoTomador(normalizarNombre(mGlobal.group(1)));
            if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                return cand;
            }
        }

        return null;
    }

    private String limpiarPrefijoTomador(String texto) {
        return texto.replaceFirst("^(?:DEL\\s+)?(?:[A-ZÁÉÍÓÚÑ]*OMADOR\\s+)+", "").trim();
    }

    private boolean esTextoNoPersona(String texto) {
        String[] prohibidas = {
            "APELLIDOS", "NOMBRES", "TOMADOR", "TIPO DE DOCUMENTO", "NUMERO DE DOCUMENTO",
            "NO. DE DOCUMENTO", "CIUDAD", "RESIDENCIA", "EXPEDICION", "EXPEDICIÓN",
            "TELEFONO", "TELÉFONO", "FECHA", "VIGENCIA", "POLIZA", "PÓLIZA", "SOAT",
            "RUNT", "GASTOS", "MEDICOS", "MÉDICOS", "QUIRURGICOS", "QUIRÚRGICOS",
            "PORTAR", "SIEMPRE", "AUTORIDADES", "TRANSITO", "TRÁNSITO", "VICTIMAS", "VÍCTIMAS",
            "ASEGURADORA", "SUCURSAL", "PRODUCTOR", "FORMULARIO", "TARIFA", "PRIMA",
            "CONTRIBUCION", "CONTRIBUCIÓN", "FOSYGA", "TASA", "TOTAL A PAGAR", "HABEAS DATA",
            "VIGENTE", "ACARREA", "MULTAS", "ECONOMICAS", "ECONÓMICAS", "DETENCION", "DETENCIÓN",
            "VEHICULO", "VEHÍCULO", "RENOVAR", "RECOMENDACIONES", "ACCIDENTE",
            "RESULTA", "HERIDO", "DEBE", "SER", "ATENDIDO", "PRESTADOR", "SALUD",
            "CASO", "CONTRARIO", "DENUNCIE", "SUPERINTENDENCIA",
            "PARA", "QUE", "CONSULTE", "ALMACENE", "ADMINISTRE", "AUTORIZO", "SEGUROS"
        };
        for (String p : prohibidas) {
            if (texto.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private Integer extraerCilindrajePrevisora(String upper) {
        int idxCil = upper.indexOf("CILINDRAJE");
        if (idxCil < 0) {
            idxCil = upper.indexOf("VATIOS");
        }
        if (idxCil >= 0) {
            String tramo = upper.substring(idxCil, Math.min(idxCil + 250, upper.length()));
            Pattern patronNum = Pattern.compile("\\b(\\d{2,4})\\b");
            Matcher matcherNum = patronNum.matcher(tramo);
            while (matcherNum.find()) {
                int valor = Integer.parseInt(matcherNum.group(1));
                if (valor >= 50 && valor < 1900 && valor != 2022 && valor != 2023 && valor != 2024 && valor != 2025 && valor != 2026 && valor != 2027) {
                    return valor;
                }
            }
        }

        Pattern patronAntesModelo = Pattern.compile("\\b(\\d{2,4})\\b.{0,40}?\\b((?:19|20)\\d{2})\\b");
        Matcher matcherAntesModelo = patronAntesModelo.matcher(upper);
        while (matcherAntesModelo.find()) {
            int valor = Integer.parseInt(matcherAntesModelo.group(1));
            if (valor >= 50 && valor < 1900) {
                return valor;
            }
        }

        int idxInicio = upper.indexOf("CARROCER");
        int idxFin = upper.indexOf("APELLIDOS");
        if (idxInicio >= 0) {
            String bloque = upper.substring(idxInicio, idxFin > idxInicio ? idxFin : Math.min(idxInicio + 300, upper.length()));
            Pattern patronBloque = Pattern.compile("\\b(\\d{2,4})\\b");
            Matcher matcherBloque = patronBloque.matcher(bloque);
            while (matcherBloque.find()) {
                int valor = Integer.parseInt(matcherBloque.group(1));
                if (valor >= 50 && valor < 1900) {
                    return valor;
                }
            }
        }

        return null;
    }

    private Integer extraerModeloPrevisora(String upper, Integer cilindraje) {
        if (cilindraje != null) {
            Pattern patronPar = Pattern.compile("\\b" + cilindraje + "\\b.{0,40}?\\b((?:19|20)\\d{2})\\b");
            Matcher matcherPar = patronPar.matcher(upper);
            if (matcherPar.find()) {
                return Integer.parseInt(matcherPar.group(1));
            }
        }

        int idxModelo = upper.indexOf("MODELO");
        if (idxModelo >= 0) {
            String tramo = upper.substring(idxModelo, Math.min(idxModelo + 250, upper.length()));
            Pattern patronAnio = Pattern.compile("\\b((?:19|20)\\d{2})\\b");
            Matcher matcherAnio = patronAnio.matcher(tramo);
            while (matcherAnio.find()) {
                int anio = Integer.parseInt(matcherAnio.group(1));
                if (anio >= 1980 && anio <= 2030) {
                    return anio;
                }
            }
        }

        return null;
    }

    private String normalizarNombre(String raw) {
        return raw.replaceAll("\\s*,\\s*", ", ")
                  .replaceAll("\\s+", " ")
                  .trim();
    }

    private int calcularEdadDesdeModelo(Integer modeloAnio, LocalDate desde) {
        if (modeloAnio == null) return 0;
        int anioRef = (desde != null) ? desde.getYear() : LocalDate.now().getYear();
        return Math.max(anioRef - modeloAnio, 0);
    }

    private BigDecimal extraerCapacidadToneladas(String upper) {
        Pattern pCap = Pattern.compile("CAPACIDAD\\s*TON\\.?\\s*:?\\s*(\\d+(?:[\\.,]\\d+)?)\\b");
        Matcher mCap = pCap.matcher(upper);
        if (mCap.find()) {
            try {
                return new BigDecimal(mCap.group(1).replace(",", "."));
            } catch (NumberFormatException ignored) {
            }
        }
        return BigDecimal.ZERO;
    }

    private String extraerPlaca(String upper) {
        Pattern patronEtiqueta = Pattern.compile(
                "(?:PLACA\\s*(?:No\\.?|NRO\\.?|N°)?|NRO\\.?\\s*PLACA)\\s*:?\\s*([A-Z0-9\\s\\-]{5,8})\\b"
        );
        Matcher matcherEtiqueta = patronEtiqueta.matcher(upper);
        while (matcherEtiqueta.find()) {
            String limpia = matcherEtiqueta.group(1).replaceAll("[\\s\\-]", "");
            if (limpia.matches("[A-Z]{3}[0-9]{2}[A-Z0-9]") || limpia.matches("[0-9]{3}[A-Z]{3}")) {
                return limpia;
            }
        }

        Pattern patronGeneral = Pattern.compile("\\b([A-Z]{3}[0-9]{2}[A-Z0-9]|[0-9]{3}[A-Z]{3})\\b");
        Matcher matcherGeneral = patronGeneral.matcher(upper);
        while (matcherGeneral.find()) {
            String candidata = matcherGeneral.group(1);
            if (!candidata.equals("SOAT") && !candidata.equals("RUNT")) {
                return candidata;
            }
        }

        return null;
    }

    private void extraerTipoVehiculo(String upper, Poliza poliza) {
        String[] tiposOrdenados = {
            "AUTOS FAMILIARES",
            "AUTOMOVILES FAMILIARES",
            "AUTOMÓVILES FAMILIARES",
            "CAMPEROS O CAMIONETA",
            "CAMIONETA",
            "AUTOMOVIL",
            "AUTOMÓVIL",
            "MOTO"
        };

        for (String tipo : tiposOrdenados) {
            if (upper.contains(tipo)) {
                if (tipo.contains("FAMILIARES")) poliza.setTipoVehiculo("AUTOS FAMILIARES");
                else if (tipo.contains("CAMPEROS")) poliza.setTipoVehiculo("CAMPEROS O CAMIONETA");
                else if (tipo.contains("CAMIONETA")) poliza.setTipoVehiculo("CAMIONETA");
                else if (tipo.contains("AUTOM")) poliza.setTipoVehiculo("AUTOMOVIL");
                else poliza.setTipoVehiculo(tipo);
                return;
            }
        }
    }

    private List<LocalDate> extraerFechasIso(String upper) {
        Pattern patronFechas = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
        Matcher matcherFechas = patronFechas.matcher(upper);
        List<LocalDate> fechas = new ArrayList<>();
        while (matcherFechas.find()) {
            try {
                fechas.add(LocalDate.parse(matcherFechas.group(1)));
            } catch (Exception ignored) {
            }
        }
        return fechas;
    }

    private void extraerFechasPorColumnas(String upper, Poliza poliza) {
        Pattern patronColumnas = Pattern.compile("\\b((?:19|20)\\d{2})\\s+(0?[1-9]|1[0-2])\\s+(0?[1-9]|[12]\\d|3[01])\\b");
        Matcher matcherColumnas = patronColumnas.matcher(upper);
        List<LocalDate> fechas = new ArrayList<>();
        while (matcherColumnas.find()) {
            try {
                fechas.add(LocalDate.of(
                    Integer.parseInt(matcherColumnas.group(1)),
                    Integer.parseInt(matcherColumnas.group(2)),
                    Integer.parseInt(matcherColumnas.group(3))
                ));
            } catch (Exception ignored) {
            }
        }
        if (fechas.size() >= 3) {
            poliza.setDesde(fechas.get(1));
            poliza.setHasta(fechas.get(2));
        } else if (fechas.size() == 2) {
            poliza.setDesde(fechas.get(0));
            poliza.setHasta(fechas.get(1));
        }
    }
}