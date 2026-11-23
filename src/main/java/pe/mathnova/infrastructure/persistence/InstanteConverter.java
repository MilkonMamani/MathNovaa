package pe.mathnova.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Instant ↔ TEXT ISO-8601 UTC ('2026-11-15T19:21:00Z'), como pide V1. */
@Converter
public class InstanteConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant instante) {
        return instante == null ? null : instante.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String texto) {
        return texto == null ? null : Instant.parse(texto);
    }
}
