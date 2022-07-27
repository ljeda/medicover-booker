package ljeda.medicover.selenium;

import java.time.LocalDate;
import java.time.LocalTime;

public class Slot {
	private LocalDate date;
	private LocalTime time;
	private String place;
	private String doctor;
	
	public Slot(LocalDate date, LocalTime time, String place, String doctor) {
		this.date = date;
		this.time = time;
		this.place = place;
		this.doctor = doctor;
	}
	
	public LocalDate getDate() {
		return date;
	}
	
	public LocalTime getTime() {
		return time;
	}
	
	public String getPlace() {
		return place;
	}
	
	public String getDoctor() {
		return doctor;
	}
}
