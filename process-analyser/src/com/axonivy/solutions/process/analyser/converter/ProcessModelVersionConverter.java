package com.axonivy.solutions.process.analyser.converter;

import ch.ivyteam.ivy.application.app.Application;
import ch.ivyteam.ivy.application.project.Project;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "pmvConverter")
@ApplicationScoped
public class ProcessModelVersionConverter implements Converter<Object> {

  @Override
  public Project getAsObject(FacesContext arg0, UIComponent arg1, String value) throws ConverterException {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Application.current().projects().find(value);
  }

  @Override
  public String getAsString(FacesContext arg0, UIComponent arg1, Object value) throws ConverterException {
    if (value == null) {
      return "";
    }
    if (Project.class.isInstance(value)) {
      return Project.class.cast(value).name();
    }
    throw new ConverterException("Unexpected value type: " + value.getClass().getName());
  }
}
