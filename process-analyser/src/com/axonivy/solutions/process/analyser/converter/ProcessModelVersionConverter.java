package com.axonivy.solutions.process.analyser.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModelVersion;

@FacesConverter(value = "pmvConverter")
public class ProcessModelVersionConverter implements Converter {

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
