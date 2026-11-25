package pe.mathnova.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanoEnteroConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean valor) {
        return valor == null ? null : (valor ? 1 : 0);
    }

    @Override
    public Boolean convertToEntityAttribute(Integer valor) {
        return valor == null ? null : valor != 0;
    }
}
