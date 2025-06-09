package com.axonivy.solutions.process.analyser.demo.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.axonivy.solutions.process.analyser.demo.data.FlightInformation;
import com.axonivy.solutions.process.analyser.demo.managedbean.SearchingFlightBean;

@FacesConverter(value = "flightInformationConverter", forClass = FlightInformation.class)
public class FlightInformationConverter implements Converter{

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        SearchingFlightBean bean = context.getApplication()
            .evaluateExpressionGet(context, "#{searchingFlightBean}", SearchingFlightBean.class);
        for (FlightInformation flight : bean.getFlights()) {
            if (flight.getId() != null && flight.getId().equals(value)) {
                return flight;
            }
        }
        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value instanceof FlightInformation) {
            FlightInformation flight = (FlightInformation) value;
            return flight.getId();
        }
        return "";
    }
}
