package com.axonivy.utils.process.analyser.demo.managedbean;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.persistence.query.IPagedResult;
import ch.ivyteam.ivy.security.IRole;
import ch.ivyteam.ivy.security.IUser;
import ch.ivyteam.ivy.security.exec.Sudo;

@ManagedBean
@ViewScoped
public class PlanningFlightBean {

	private Map<String, String> aircrafts;
	private Map<String, String> pilots;
	private Map<String, String> cabinCrews;
	private String suggestionFromMarketing;
	private String pilot;
	private String cabinCrew;
	private String aircraft;
	private String from;
	private String to;
	private Date date;

	@PostConstruct
	public void init() {
		suggestionFromMarketing = """
				HCM <--> New York,
				Amsterdam <--> Delhi,
				Dubai City <--> Ha Noi,
				Gen�ve <--> Hong Kong,
				Jakarta <--> Kuala Lumpur
				""";

		aircrafts = new HashMap<>();
		aircrafts.put("Plane-1", "Plane-1");
		aircrafts.put("Plane-2", "Plane-2");
		aircrafts.put("Plane-3", "Plane-3");

		Sudo.run(() -> {
			IRole rolePilot = Ivy.security().roles().find("Pilot");
			if (rolePilot != null) {
				pilots = new HashMap<>();
				IPagedResult<IUser> userHasPilotRolePageResult = rolePilot.users().assignedPaged();
				for (int i = 1; i <= userHasPilotRolePageResult.count(); i++) {
					for (IUser u : userHasPilotRolePageResult.page(i)) {
						pilots.put(u.getFullName(), u.getFullName());
					}
				}
			}

			IRole roleCabinCrew = Ivy.security().roles().find("CabinCrew");
			if (roleCabinCrew != null) {
				cabinCrews = new HashMap<>();
				IPagedResult<IUser> userHasCabinCrewRolePageResult = roleCabinCrew.users().assignedPaged();
				for (int i = 1; i <= userHasCabinCrewRolePageResult.count(); i++) {
					for (IUser u : userHasCabinCrewRolePageResult.page(i)) {
						cabinCrews.put(u.getFullName(), u.getFullName());
					}
				}
			}
		});
	}

	public Map<String, String> getAircrafts() {
		return aircrafts;
	}

	public void setAircrafts(Map<String, String> aircrafts) {
		this.aircrafts = aircrafts;
	}

	public Map<String, String> getPilots() {
		return pilots;
	}

	public void setPilots(Map<String, String> pilots) {
		this.pilots = pilots;
	}

	public Map<String, String> getCabinCrews() {
		return cabinCrews;
	}

	public void setCabinCrews(Map<String, String> cabinCrews) {
		this.cabinCrews = cabinCrews;
	}

	public String getSuggestionFromMarketing() {
		return suggestionFromMarketing;
	}

	public void setSuggestionFromMarketing(String suggestionFromMarketing) {
		this.suggestionFromMarketing = suggestionFromMarketing;
	}

	public String getPilot() {
		return pilot;
	}

	public void setPilot(String pilot) {
		this.pilot = pilot;
	}

	public String getCabinCrew() {
		return cabinCrew;
	}

	public void setCabinCrew(String cabinCrew) {
		this.cabinCrew = cabinCrew;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getAircraft() {
		return aircraft;
	}

	public void setAircraft(String aircraft) {
		this.aircraft = aircraft;
	}

	public boolean isCompleted() {
		return StringUtils.isNoneBlank(pilot, cabinCrew, aircraft, from, to) && date != null;
	}
}
