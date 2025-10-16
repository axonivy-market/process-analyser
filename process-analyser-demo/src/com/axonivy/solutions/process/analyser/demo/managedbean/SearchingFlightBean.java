package com.axonivy.solutions.process.analyser.demo.managedbean;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.solutions.process.analyser.demo.data.FlightInformation;

@ManagedBean
@ViewScoped
public class SearchingFlightBean {
	private FlightInformation selectedFlight;

	public FlightInformation getSelectedFlight() {
		return selectedFlight;
	}

	public void setSelectedFlight(FlightInformation selectedFlight) {
		this.selectedFlight = selectedFlight;
	}

	private List<FlightInformation> flights = new ArrayList<>();

	public void searchFlights(FlightInformation flightInformation) {
		if (flightInformation.getFrom() == "" || flightInformation.getDate() == null
				|| flightInformation.getTo() == "") {
			return;
		}
		flights = new ArrayList<>();
		Random random = new Random();
		int numberOfFlights = 3 + random.nextInt(8);

		Set<LocalTime[]> scheduledTimes = new HashSet<>();

		for (int i = 1; i <= numberOfFlights; i++) {
			LocalTime startTime;
			LocalTime endTime;
			boolean isOverlap;

			do {
				int startHour = random.nextInt(23);
				int startMinute = random.nextBoolean() ? 0 : 30;
				startTime = LocalTime.of(startHour, startMinute);

				int duration = 1 + random.nextInt(6);
				endTime = startTime.plusHours(duration);
				if (endTime.isAfter(LocalTime.of(23, 59))) {
					endTime = LocalTime.of(23, 59);
				}

				isOverlap = false;
				for (LocalTime[] existing : scheduledTimes) {
					if (!(endTime.isBefore(existing[0]) || startTime.isAfter(existing[1]))) {
						isOverlap = true;
						break;
					}
				}

			} while (isOverlap);

			scheduledTimes.add(new LocalTime[] { startTime, endTime });

			FlightInformation flight = new FlightInformation();
			flight.setId("F-" + i);
			flight.setPlaneRegistrationNumber(String.format("%04d", i));
			flight.setStartTime(startTime);
			flight.setEndTime(endTime);
			flight.setDate(flightInformation.getDate());
			flight.setTo(flightInformation.getTo());
			flight.setFrom(flightInformation.getFrom());

			flights.add(flight);
		}
	}

	public List<FlightInformation> getFlights() {
		return flights;
	}

	public void setFlights(List<FlightInformation> flights) {
		this.flights = flights;
	}

	public String generateLabelForFlight(FlightInformation flight) {
		return String.format("%s: %s - %s", flight.getId(),
				flight.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
				flight.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
	}
}
