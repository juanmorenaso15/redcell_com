package com.redcell.soat.services;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
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
            String textoMayus = normalizarSinTildes(textoLimpio.toUpperCase());

            if (textoMayus.contains("PREVISORA")) {
                poliza.setAseguradora("LA PREVISORA S.A.");
                extraerPrevisora(textoMayus, poliza);
            } else if (textoMayus.contains("AXA")
                    || textoMayus.contains("COLPATRIA")
                    || textoMayus.contains("SOAT POLIZA")) {
                poliza.setAseguradora("AXA COLPATRIA");
                extraerAxaColpatria(textoMayus, poliza);
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

    private String normalizarSinTildes(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.replace('Κ', 'K')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void extraerPrevisora(String upper, Poliza poliza) {
        String placa = extraerPlaca(upper);
        if (placa != null) {
            poliza.setPlaca(placa);
        }

        extraerTipoVehiculo(upper, poliza);

        String marca = extraerMarca(upper);
        String linea = extraerLineaPrevisora(upper);

        if (marca != null && linea != null) {
            poliza.setMarcaLinea(linea.startsWith(marca + " ") ? linea : marca + " " + linea);
        } else {
            poliza.setMarcaLinea(marca != null ? marca : linea);
        }

        Integer cilindraje = extraerCilindraje(upper);
        if (cilindraje != null) {
            poliza.setCilindraje(cilindraje);
        }

        Integer modeloAnio = extraerModelo(upper, cilindraje, null, null);
        poliza.setCapacidadToneladas(extraerCapacidadToneladas(upper));
        poliza.setPropietario(extraerNombrePorEtiqueta(upper));

        extraerDocumento(upper, poliza);
        extraerFechasVigencia(upper, poliza, false);

        if (modeloAnio == null) {
            modeloAnio = extraerModelo(upper, cilindraje, poliza.getDesde(), poliza.getHasta());
        }
        poliza.setEdadVehiculo(calcularEdadDesdeModelo(modeloAnio, poliza.getDesde()));
    }

    private void extraerAxaColpatria(String upper, Poliza poliza) {
        String placa = extraerPlaca(upper);
        if (placa != null) {
            poliza.setPlaca(placa);
        }

        extraerTipoVehiculo(upper, poliza);

        String marca = extraerMarcaAxa(upper);
        String linea = extraerLineaAxa(upper);

        if (marca != null && linea != null) {
            poliza.setMarcaLinea(linea.startsWith(marca + " ") ? linea : marca + " " + linea);
        } else {
            poliza.setMarcaLinea(marca != null ? marca : linea);
        }

        Integer cilindraje = extraerCilindraje(upper);
        if (cilindraje != null) {
            poliza.setCilindraje(cilindraje);
        }

        poliza.setCapacidadToneladas(extraerCapacidadToneladas(upper));
        poliza.setPropietario(extraerNombrePorEtiqueta(upper));

        extraerDocumento(upper, poliza);
        extraerFechasVigencia(upper, poliza, true);

        Integer modeloAnio = extraerModelo(upper, cilindraje, poliza.getDesde(), poliza.getHasta());
        poliza.setEdadVehiculo(calcularEdadDesdeModelo(modeloAnio, poliza.getDesde()));
    }

    private String extraerMarca(String upper) {
        Pattern patronMarca = Pattern.compile("\\bMARCA\\b\\s*:?\\s*([A-Z0-9\\-]{2,20})\\b");
        Matcher matcherMarca = patronMarca.matcher(upper);
        while (matcherMarca.find()) {
            String cand = matcherMarca.group(1).trim();
            if (!cand.equals("VEHICULO") && !cand.equals("LINEA") && !cand.equals("CLASE") && !cand.equals("SERVICIO")) {
                return cand;
            }
        }
        return null;
    }

    private String extraerLineaPrevisora(String upper) {
        if (upper.contains("YUAN PLUS EV")) {
            return "YUAN PLUS EV";
        }
        Pattern patronLinea = Pattern.compile(
                "(?:LINEA\\s+VEHICULO|VEHICULO)\\s*:?\\s*([A-Z0-9\\s\\-]{2,30}?)(?=\\s+(?:CLASE|SERVICIO|APELLIDOS|TOMADOR|CODIGO|TARIFA|PRIMA|CILINDRAJE|MODELO|PLACA|CHASIS|MOTOR|CARROCERIA|$))"
        );
        Matcher matcherLinea = patronLinea.matcher(upper);
        while (matcherLinea.find()) {
            String candidata = matcherLinea.group(1).trim();
            if (!candidata.startsWith("MOTOS")
                    && !candidata.startsWith("CAMPEROS")
                    && !candidata.startsWith("AUTOS")
                    && !candidata.startsWith("VEHICULO PARA")
                    && !candidata.equals("PARA 6 O MAS")) {
                return candidata;
            }
        }
        return null;
    }

    private String extraerMarcaAxa(String upper) {
        if (upper.contains("KIA")) return "KIA";
        if (upper.contains("TVS")) return "TVS";

        Pattern patronMarca = Pattern.compile("\\bMARCA\\b\\s*:?\\s*([A-Z0-9\\-]{2,20})\\b");
        Matcher matcherMarca = patronMarca.matcher(upper);
        while (matcherMarca.find()) {
            String candidata = matcherMarca.group(1).trim();
            if (!candidata.equals("VEHICULO") && !candidata.equals("LINEA") && !candidata.equals("CLASE")) {
                return candidata;
            }
        }
        return null;
    }

    private String extraerLineaAxa(String upper) {
        if (upper.contains("PICANTO EX")) return "PICANTO EX";
        if (upper.contains("RAIDER 125 FI")) return "RAIDER 125 FI";

        Pattern patronLinea = Pattern.compile(
                "(?:LINEAULO|LINEA(?:\\s+VEHICULO)?)\\s*:?\\s*([A-Z0-9\\s\\-]{2,25}?)(?=\\s+(?:NO\\.|N°|CILINDRAJE|MODELO|CARROCERIA|SERVICIO|PASAJEROS|TOMADOR|CLASE|$))"
        );
        Matcher matcherLinea = patronLinea.matcher(upper);
        while (matcherLinea.find()) {
            String candidataLinea = matcherLinea.group(1).trim();
            if (!candidataLinea.startsWith("VEHICULO")) {
                return candidataLinea;
            }
        }
        return null;
    }

    private Integer extraerCilindraje(String upper) {
        int idxCil = upper.indexOf("CILINDRAJE");
        if (idxCil < 0) {
            idxCil = upper.indexOf("VATIOS");
        }
        if (idxCil >= 0) {
            String tramo = upper.substring(idxCil, Math.min(idxCil + 200, upper.length()));
            Matcher mCil = Pattern.compile("\\b(\\d{1,2}(?:\\.\\d{3})|\\d{2,4})\\b").matcher(tramo);
            while (mCil.find()) {
                int valor = parsearEnteroConMiles(mCil.group(1));
                if (valor >= 50 && valor <= 15000 && !esAnioVehiculoOFecha(valor)) {
                    return valor;
                }
            }
        }

        Matcher patronAntesModelo = Pattern.compile("\\b(\\d{1,2}(?:\\.\\d{3})|\\d{2,4})\\b.{0,40}?\\b((?:19|20)\\d{2})\\b").matcher(upper);
        while (patronAntesModelo.find()) {
            int valor = parsearEnteroConMiles(patronAntesModelo.group(1));
            if (valor >= 50 && valor <= 15000) {
                return valor;
            }
        }

        return null;
    }

    private int parsearEnteroConMiles(String raw) {
        String limpio = raw.replace(".", "").replace(",", "").trim();
        return Integer.parseInt(limpio);
    }

    private boolean esAnioVehiculoOFecha(int valor) {
        return valor >= 2020 && valor <= 2030;
    }

    private Integer extraerModelo(String upper, Integer cilindraje, LocalDate desde, LocalDate hasta) {
        int anioDesde = (desde != null) ? desde.getYear() : -1;
        int anioHasta = (hasta != null) ? hasta.getYear() : -1;

        if (cilindraje != null) {
            String cilPattern = String.valueOf(cilindraje);
            Matcher patronPar = Pattern.compile("\\b" + cilPattern + "\\b.{0,45}?\\b((?:19|20)\\d{2})\\b").matcher(upper);
            while (patronPar.find()) {
                int anio = Integer.parseInt(patronPar.group(1));
                if (anio >= 1970 && anio <= 2030 && anio != anioDesde && anio != anioHasta) {
                    return anio;
                }
            }
        }

        int idxModelo = upper.indexOf("MODELO");
        if (idxModelo >= 0) {
            String tramo = upper.substring(idxModelo, Math.min(idxModelo + 220, upper.length()));
            Matcher mAnio = Pattern.compile("\\b((?:19|20)\\d{2})\\b").matcher(tramo);
            while (mAnio.find()) {
                int anio = Integer.parseInt(mAnio.group(1));
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
            if (anio >= 1970 && anio <= 2030 && anio != anioDesde && anio != anioHasta) {
                return anio;
            }
        }

        return null;
    }

    private void extraerDocumento(String upper, Poliza poliza) {
        Pattern patronTipoDoc = Pattern.compile(
                "TIPO\\s+DE\\s*DOCUMENT\\s*O\\s*(?:DEL\\s+TOMADOR)?\\s*:?\\s*\\b(CC|CE|TI|NIT|PA)\\b"
        );
        Matcher matcherTipoDoc = patronTipoDoc.matcher(upper);
        if (matcherTipoDoc.find()) {
            poliza.setTipoDocumento(matcherTipoDoc.group(1));
        }

        Pattern patronDocNum = Pattern.compile(
                "(?:No\\.?|NRO\\.?|NUMERO)\\s*DE\\s*DOCUMENT\\s*O\\s*(?:DEL\\s+TOMADOR)?\\s*:?\\s*\\b(\\d{6,12})\\b"
        );
        Matcher matcherDocNum = patronDocNum.matcher(upper);
        if (matcherDocNum.find()) {
            poliza.setDocumento(Long.parseLong(matcherDocNum.group(1)));
            if (poliza.getTipoDocumento() == null) {
                poliza.setTipoDocumento("CC");
            }
            return;
        }

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

    private void extraerFechasVigencia(String upper, Poliza poliza, boolean priorizarColumnas) {
        if (priorizarColumnas) {
            extraerFechasPorColumnas(upper, poliza);
        }

        if (poliza.getDesde() == null || poliza.getHasta() == null) {
            List<LocalDate> fechasIso = extraerFechasIso(upper);
            if (fechasIso.size() >= 3) {
                poliza.setDesde(fechasIso.get(1));
                poliza.setHasta(fechasIso.get(2));
            } else if (fechasIso.size() == 2) {
                poliza.setDesde(fechasIso.get(0));
                poliza.setHasta(fechasIso.get(1));
            }
        }

        if (poliza.getDesde() == null || poliza.getHasta() == null) {
            extraerFechasPorColumnas(upper, poliza);
        }
    }

    private String extraerNombrePorEtiqueta(String upper) {
        Pattern pEtiqueta = Pattern.compile(
                "APELLIDOS\\s+Y\\s+NOMBRES(?:\\s+DEL\\s+TOMADOR)?\\s*:?\\s*(.{5,220})"
        );
        Matcher mEtiqueta = pEtiqueta.matcher(upper);
        if (mEtiqueta.find()) {
            String tramo = mEtiqueta.group(1).trim();
            String bloqueNombre = tramo.split(
                    "\\s+(?:CODIGO|TELEFONO|TIPO\\s+DE\\s*DOCUMENT|NO\\.?\\s*DE\\s*DOCUMENT|CIUDAD|SUCURSAL|\\d{6,12})"
            )[0].trim();
            bloqueNombre = limpiarPrefijoTomador(bloqueNombre);

            Matcher mComa = Pattern.compile(
                    "([A-ZÑ]{2,}(?:\\s+[A-ZÑ]{2,}){0,2}\\s*,\\s*[A-ZÑ]{2,}(?:\\s+[A-ZÑ]{2,}){0,2})"
            ).matcher(bloqueNombre);
            if (mComa.find()) {
                String cand = limpiarPrefijoTomador(normalizarNombre(mComa.group(1)));
                if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                    return cand;
                }
            }

            Matcher mSinComa = Pattern.compile("^([A-ZÑ]{2,}(?:\\s+[A-ZÑ]{2,}){1,3})\\b")
                    .matcher(bloqueNombre);
            if (mSinComa.find()) {
                String cand = limpiarPrefijoTomador(normalizarNombre(mSinComa.group(1)));
                if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                    return cand;
                }
            }
        }

        int limite = upper.length();
        for (String corte : new String[]{"PRIMA SOAT", "CONTRIBUCION FOSYGA", "CLAUSULA", "AUTORIZO", "SENOR USUARIO"}) {
            int pos = upper.indexOf(corte);
            if (pos > 0 && pos < limite) {
                limite = pos;
            }
        }
        String zonaSuperior = upper.substring(0, limite);

        Matcher mGlobal = Pattern.compile(
                "([A-ZÑ]{2,}(?:\\s+[A-ZÑ]{2,}){1,2}\\s*,\\s*[A-ZÑ]{2,}(?:\\s+[A-ZÑ]{2,}){0,2})"
        ).matcher(zonaSuperior);
        while (mGlobal.find()) {
            String cand = limpiarPrefijoTomador(normalizarNombre(mGlobal.group(1)));
            if (!cand.isEmpty() && !esTextoNoPersona(cand)) {
                return cand;
            }
        }

        return null;
    }

    private String limpiarPrefijoTomador(String texto) {
        return texto.replaceFirst("^(?:DEL\\s+)?(?:[A-ZÑ]*OMADOR\\s+)+", "").trim();
    }

    private boolean esTextoNoPersona(String texto) {
        String[] prohibidas = {
            "APELLIDOS", "NOMBRES", "TOMADOR", "TIPO DE DOCUMENTO", "NUMERO DE DOCUMENTO",
            "NO. DE DOCUMENTO", "CIUDAD", "RESIDENCIA", "EXPEDICION", "TELEFONO",
            "FECHA", "VIGENCIA", "POLIZA", "SOAT", "RUNT", "GASTOS", "MEDICOS",
            "QUIRURGICOS", "PORTAR", "SIEMPRE", "AUTORIDADES", "TRANSITO", "VICTIMAS",
            "ASEGURADORA", "SUCURSAL", "PRODUCTOR", "FORMULARIO", "TARIFA", "PRIMA",
            "CONTRIBUCION", "FOSYGA", "TASA", "TOTAL A PAGAR", "HABEAS DATA",
            "VIGENTE", "ACARREA", "MULTAS", "ECONOMICAS", "DETENCION", "VEHICULO",
            "RENOVAR", "RECOMENDACIONES", "ACCIDENTE", "RESULTA", "HERIDO", "DEBE",
            "SER", "ATENDIDO", "PRESTADOR", "SALUD", "CASO", "CONTRARIO", "DENUNCIE",
            "SUPERINTENDENCIA", "PARA", "QUE", "CONSULTE", "ALMACENE", "ADMINISTRE",
            "AUTORIZO", "SEGUROS"
        };
        for (String p : prohibidas) {
            if (texto.contains(p)) {
                return true;
            }
        }
        return false;
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
                "(?:PLACA\\s*(?:NO\\.?|NRO\\.?|N°)?|NRO\\.?\\s*PLACA)\\s*:?\\s*([A-Z0-9\\s\\-]{5,8})\\b"
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
        // 1. Subtipos específicos primero para evitar falsos positivos
        if (upper.contains("INTERMUNICIPAL")) {
            poliza.setTipoVehiculo("INTERMUNICIPAL");
            return;
        }
        if (upper.contains("MOTOCARRO") || upper.contains("TRICIMOTO") || upper.contains("CUADRICICLO")) {
            poliza.setTipoVehiculo("MOTOCARRO");
            return;
        }
        if (upper.contains("CICLOMOTOR")) {
            poliza.setTipoVehiculo("CICLOMOTOR");
            return;
        }
        if (upper.contains("VEHICULO PARA 6 O MAS")
                || upper.contains("VEHICULOS PARA SEIS O MAS PASAJEROS")
                || upper.contains("SEIS O MAS PASAJEROS")) {
            poliza.setTipoVehiculo("VEHICULO PARA 6 O MAS");
            return;
        }
        if (upper.contains("AUTOS FAMILIARES") || upper.contains("AUTOMOVILES FAMILIARES")) {
            poliza.setTipoVehiculo("AUTOS FAMILIARES");
            return;
        }
        if (upper.contains("CAMPEROS O CAMIONETA") || upper.contains("CAMPEROS Y CAMIONETAS")) {
            poliza.setTipoVehiculo("CAMPEROS Y CAMIONETAS");
            return;
        }
        if (upper.contains("CARGA O MIXTO") || upper.contains("CARGA") || upper.contains("MIXTO")) {
            poliza.setTipoVehiculo("CARGA O MIXTO");
            return;
        }
        if (upper.contains("OFICIAL")) {
            poliza.setTipoVehiculo("OFICIAL");
            return;
        }
        if (upper.contains("NEGOCIO") || upper.contains("TAXI")) {
            poliza.setTipoVehiculo("TAXI");
            return;
        }
        if (upper.contains("BUSETA")) {
            poliza.setTipoVehiculo("BUSETA");
            return;
        }
        if (upper.contains("BUS")) {
            poliza.setTipoVehiculo("BUS");
            return;
        }
        if (upper.contains("CAMIONETA")) {
            poliza.setTipoVehiculo("CAMIONETA");
            return;
        }
        if (upper.contains("CAMPERO")) {
            poliza.setTipoVehiculo("CAMPERO");
            return;
        }
        if (upper.contains("AUTOMOVIL")) {
            poliza.setTipoVehiculo("AUTOMOVIL");
            return;
        }
        if (upper.contains("MOTO")) {
            poliza.setTipoVehiculo("MOTO");
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