package com.axonivy.solutions.process.analyser.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import com.axonivy.solutions.process.analyser.enums.KpiType;

@FacesConverter("kpiTypeConverter")
public class KpiTypeConverter implements Converter {

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