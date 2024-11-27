package com.axonivy.utils.process.analyzer.demo.managedbean;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.demo.data.FlightInformation;

@ManagedBean
@ViewScoped
public class SearchingFlightBean {
  private String selectedFlightId;
  private List<FlightInformation> flights = new ArrayList<>();

  public void searchFlights(String from, String to, Date date) {
    if (StringUtils.isBlank(to) || StringUtils.isBlank(from) || date == null) {
      return;
    }
    FlightInformation flight1 = new FlightInformation();
    flight1.setId("F-1");
    flight1.setPlaneRegistrationNumber("0001");
    flight1.setStartTime(LocalTime.of(6, 0));
    flight1.setEndTime(LocalTime.of(12, 0));
    flight1.setDate(date);
    flight1.setFrom(from);
    flight1.setTo(to);

    FlightInformation flight2 = new FlightInformation();
    flight2.setId("F-2");
    flight2.setPlaneRegistrationNumber("0002");
    flight2.setStartTime(LocalTime.of(13, 0));
    flight2.setEndTime(LocalTime.of(19, 0));
    flight2.setDate(date);
    flight2.setFrom(from);
    flight2.setTo(to);

    FlightInformation flight3 = new FlightInformation();
    flight3.setId("F-3");
    flight3.setPlaneRegistrationNumber("0003");
    flight3.setStartTime(LocalTime.of(17, 0));
    flight3.setEndTime(LocalTime.of(23, 0));
    flight3.setDate(date);
    flight3.setFrom(from);
    flight3.setTo(to);

    flights = new ArrayList<>();
    flights.add(flight1);
    flights.add(flight2);
    flights.add(flight3);
  }

  public List<FlightInformation> getFlights() {
    return flights;
  }

  public void setFlights(List<FlightInformation> flights) {
    this.flights = flights;
  }

  public String getSelectedFlightId() {
    return selectedFlightId;
  }

  public void setSelectedFlightId(String selectedFlightId) {
    this.selectedFlightId = selectedFlightId;
  }

  public String generateLabelForFlight(FlightInformation flight) {
    return String.format("%s: %s (%s) - %s (%s)", flight.getId(),
        flight.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")), flight.getFrom(),
        flight.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")), flight.getTo());
  }
}
