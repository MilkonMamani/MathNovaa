package pe.mathnova.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
