package com.VetSphere.Backend.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProductoSoapResponse")
public class ProductoSoapResponse {
    private String codigo;
    private String nombre;
    private String categoria;
    private Integer stock;
    private Integer stockCritico;
    private BigDecimal precio;
    private boolean prioridadAlta;
    private boolean critico;
}


