package com.axonivy.solutions.process.analyser.converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import ch.ivyteam.ivy.environment.Ivy;

@FacesConverter("dateCustomFieldConverter")
public class DateCustomFieldConverter implements Converter {
  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
    Ivy.log().warn("stringValue " + value);
    if (value == null || value.trim().isEmpty()) {
      return null;
    }

    try {
      String[] dateRange = value.split(",");
      Ivy.log().warn("dateRange " + dateRange.length);
      if (dateRange.length == 1) {
        Ivy.log().warn("here ");
        throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid Selection",
            "Please select either no dates or two dates."));
      }

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      List<LocalDate> dateList =
          Arrays.stream(dateRange).map(date -> LocalDate.parse(date.trim(), formatter)).collect(Collectors.toList());

      return dateList;

    } catch (Exception e) {
      throw new ConverterException(
          new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Invalid date format"));
    }
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Ivy.log().warn("value " + value);
    if (value == null) {
      return "";
    }

    if (value instanceof List) {
      List<?> dateList = (List<?>) value;
      return dateList.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
    throw new ConverterException(
        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Invalid date object."));
  }
}
