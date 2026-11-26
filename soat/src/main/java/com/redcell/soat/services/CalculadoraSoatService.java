package com.redcell.soat.services;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

import com.redcell.soat.entity.Poliza;

@Service
public class CalculadoraSoatService {

    private static final BigDecimal TASA_RUNT = new BigDecimal("2400");

    public BigDecimal calcularTotalPagar(Poliza poliza) {
        String tipo = poliza.getTipoVehiculo() != null ? poliza.getTipoVehiculo().toUpperCase() : "";
        int cc = poliza.getCilindraje() != null ? poliza.getCilindraje() : 0;
        int edad = poliza.getEdadVehiculo() != null ? poliza.getEdadVehiculo() : 0;
        BigDecimal toneladas = poliza.getCapacidadToneladas() != null ? poliza.getCapacidadToneladas() : BigDecimal.ZERO;

        BigDecimal prima = BigDecimal.ZERO;
        BigDecimal fosyga = BigDecimal.ZERO;

        // 1. MOTOS (100 - 150)
        if (tipo.contains("MOTO") || tipo.contains("CICLOMOTOR")) {
            if (tipo.contains("CICLOMOTOR")) {
                prima = new BigDecimal("80100");
                fosyga = new BigDecimal("41600");
            } else if (tipo.contains("MOTOCARRO") || tipo.contains("TRICIMOTO") || tipo.contains("CUADRICICLO")) {
                prima = new BigDecimal("253000");
                fosyga = new BigDecimal("131500");
            } else if (cc < 100) {
                prima = new BigDecimal("167000");
                fosyga = new BigDecimal("86800");
            } else if (cc <= 200) {
                prima = new BigDecimal("224300");
                fosyga = new BigDecimal("116600");
            } else {
                prima = new BigDecimal("499400");
                fosyga = new BigDecimal("259600");
            }
        }
        // 2. CAMPEROS Y CAMIONETAS (211 - 232)
        else if (tipo.contains("CAMPERO") || tipo.contains("CAMIONETA")) {
            if (cc < 1500) {
                if (edad <= 9) {
                    prima = new BigDecimal("520000");
                    fosyga = new BigDecimal("270400");
                } else {
                    prima = new BigDecimal("625400");
                    fosyga = new BigDecimal("325200");
                }
            } else if (cc <= 2500) {
                if (edad <= 9) {
                    prima = new BigDecimal("621200");
                    fosyga = new BigDecimal("323000");
                } else {
                    prima = new BigDecimal("736200");
                    fosyga = new BigDecimal("382800");
                }
            } else {
                if (edad <= 9) {
                    prima = new BigDecimal("728900");
                    fosyga = new BigDecimal("379000");
                } else {
                    prima = new BigDecimal("836600");
                    fosyga = new BigDecimal("435000");
                }
            }
        }
        // 3. CARGA O MIXTO (310 - 330)
        else if (tipo.contains("CARGA") || tipo.contains("MIXTO")) {
            if (toneladas.compareTo(new BigDecimal("5")) < 0) {
                prima = new BigDecimal("582900");
                fosyga = new BigDecimal("303100");
            } else if (toneladas.compareTo(new BigDecimal("15")) <= 0) {
                prima = new BigDecimal("842400");
                fosyga = new BigDecimal("438000");
            } else {
                prima = new BigDecimal("1065500");
                fosyga = new BigDecimal("554000");
            }
        }
        // 4. OFICIALES ESPECIALES (410 - 430)
        else if (tipo.contains("OFICIAL")) {
            if (cc < 1500) {
                prima = new BigDecimal("656000");
                fosyga = new BigDecimal("341100");
            } else if (cc <= 2500) {
                prima = new BigDecimal("827500");
                fosyga = new BigDecimal("430300");
            } else {
                prima = new BigDecimal("992300");
                fosyga = new BigDecimal("515900");
            }
        }
        // 5. AUTOS FAMILIARES (511 - 532)
        else if (tipo.contains("AUTOS FAMILIARES") || tipo.contains("AUTOMOVIL") || tipo.contains("AUTOMÓVIL")) {
            if (cc < 1500) {
                if (edad <= 9) {
                    prima = new BigDecimal("292700");
                    fosyga = new BigDecimal("152200");
                } else {
                    prima = new BigDecimal("388500");
                    fosyga = new BigDecimal("202000");
                }
            } else if (cc <= 2500) {
                if (edad <= 9) {
                    prima = new BigDecimal("356800");
                    fosyga = new BigDecimal("185500");
                } else {
                    prima = new BigDecimal("444100");
                    fosyga = new BigDecimal("230900");
                }
            } else {
                if (edad <= 9) {
                    prima = new BigDecimal("416900");
                    fosyga = new BigDecimal("216700");
                } else {
                    prima = new BigDecimal("494700");
                    fosyga = new BigDecimal("257200");
                }
            }
        }
        // 6. VEHÍCULOS PARA SEIS O MÁS PASAJEROS (611 - 622)
        else if (tipo.contains("SEIS") || tipo.contains("PASAJEROS")) {
            if (cc < 2500) {
                if (edad <= 9) {
                    prima = new BigDecimal("523000");
                    fosyga = new BigDecimal("271900");
                } else {
                    prima = new BigDecimal("668000");
                    fosyga = new BigDecimal("347300");
                }
            } else {
                if (edad <= 9) {
                    prima = new BigDecimal("700600");
                    fosyga = new BigDecimal("364300");
                } else {
                    prima = new BigDecimal("841600");
                    fosyga = new BigDecimal("437600");
                }
            }
        }
        // 7. AUTOS DE NEGOCIOS Y TAXIS (711 - 732)
        else if (tipo.contains("NEGOCIO") || tipo.contains("TAXI")) {
            if (cc < 1500) {
                if (edad <= 9) {
                    prima = new BigDecimal("183900");
                    fosyga = new BigDecimal("95600");
                } else {
                    prima = new BigDecimal("230000");
                    fosyga = new BigDecimal("119600");
                }
            } else if (cc <= 2500) {
                if (edad <= 9) {
                    prima = new BigDecimal("228700");
                    fosyga = new BigDecimal("118900");
                } else {
                    prima = new BigDecimal("282900");
                    fosyga = new BigDecimal("147100");
                }
            } else {
                if (edad <= 9) {
                    prima = new BigDecimal("295400");
                    fosyga = new BigDecimal("153600");
                } else {
                    prima = new BigDecimal("346700");
                    fosyga = new BigDecimal("180200");
                }
            }
        }
        // 8. BUSES Y BUSETAS SERVICIO PÚBLICO URBANO (810)
        else if (tipo.contains("BUS") || tipo.contains("BUSETA")) {
            prima = new BigDecimal("441400");
            fosyga = new BigDecimal("229500");
        }
        // 9. SERVICIO PÚBLICO INTERMUNICIPAL (910 - 920)
        else if (tipo.contains("INTERMUNICIPAL")) {
            if (cc < 10) {
                prima = new BigDecimal("436300");
                fosyga = new BigDecimal("226800");
            } else {
                prima = new BigDecimal("633500");
                fosyga = new BigDecimal("329400");
            }
        }

        if (prima.equals(BigDecimal.ZERO)) {
            throw new RuntimeException("No se pudo calcular la tarifa para el vehículo: " + tipo + " con " + cc + "cc");
        }

        BigDecimal totalSoat = prima.add(fosyga).add(TASA_RUNT);
        poliza.setPrecioSoat(totalSoat);
        return totalSoat;
    }
}