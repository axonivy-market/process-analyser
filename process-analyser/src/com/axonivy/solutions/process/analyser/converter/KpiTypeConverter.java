package com.axonivy.solutions.process.analyser.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import jakarta.enterprise.context.ApplicationScoped;

@FacesConverter(value = "kpiTypeConverter", managed = true)
@ApplicationScoped
public class KpiTypeConverter implements Converter<Object> {

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return KpiType.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new ConverterException("Invalid KPI Type: " + value, e);
    }
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
    if (value == null) {
      return "";
    }
    if (KpiType.class.isInstance(value)) {
      return KpiType.class.cast(value).name();
    }
    throw new ConverterException("Unexpected value type: " + value.getClass().getName());
  }
}