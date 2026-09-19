package edu.umg.programacion2.proyecto.modelo;

import java.time.LocalDateTime;

/** Datos inmutables de una cita; el servicio aplica las reglas de negocio. */
public final class Cita {

    private final int id;
    private final String cliente;
    private final LocalDateTime fechaHora;
    private final String servicio;
    private final int duracionMinutos;
    private final EstadoCita estado;
    private final boolean requiereConfirmacionLlamada;

    public Cita(int id, String cliente, LocalDateTime fechaHora, String servicio,
            int duracionMinutos, EstadoCita estado) {
        this(id, cliente, fechaHora, servicio, duracionMinutos, estado, false);
    }

    public Cita(int id, String cliente, LocalDateTime fechaHora, String servicio,
            int duracionMinutos, EstadoCita estado, boolean requiereConfirmacionLlamada) {

        this.id = id;
        this.cliente = cliente;
        this.fechaHora = fechaHora;
        this.servicio = servicio;
        this.duracionMinutos = duracionMinutos;
        this.estado = estado;
        this.requiereConfirmacionLlamada = requiereConfirmacionLlamada;
    }

    public int getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getServicio() {
        return servicio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public boolean isRequiereConfirmacionLlamada() {
        return requiereConfirmacionLlamada;
    }
}