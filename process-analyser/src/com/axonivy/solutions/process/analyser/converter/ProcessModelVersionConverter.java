package com.axonivy.solutions.process.analyser.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import jakarta.enterprise.context.ApplicationScoped;

@FacesConverter(value = "pmvConverter", managed = true)
@ApplicationScoped
public class ProcessModelVersionConverter implements Converter<Object> {

  @Override
  public IProcessModelVersion getAsObject(FacesContext arg0, UIComponent arg1, String value) throws ConverterException {
    if (value == null || value.isBlank()) {
      return null;
    }
    return IApplication.current().findProcessModelVersion(value);
  }

  @Override
  public String getAsString(FacesContext arg0, UIComponent arg1, Object value) throws ConverterException {
    if (value == null) {
      return "";
    }
    if (IProcessModelVersion.class.isInstance(value)) {
      return IProcessModelVersion.class.cast(value).getVersionName();
    }
    throw new ConverterException("Unexpected value type: " + value.getClass().getName());
  }
}
