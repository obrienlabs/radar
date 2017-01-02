package org.obrienscience.radar.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

@Entity
public class FutureTrack extends Track {


	@OneToMany(mappedBy="futureTrack", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
	private List<Reading> futureReadings;

	public List<Reading> getFutureReadings() {
		return futureReadings;
	}

	public void setFutureReadings(List<Reading> futureReadings) {
		this.futureReadings = futureReadings;
	}
}
