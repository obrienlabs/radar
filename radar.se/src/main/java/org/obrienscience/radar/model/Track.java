package org.obrienscience.radar.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@Entity
public class Track {

    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(name="EL_SEQUENCE_TRACK", sequenceName="EL_TRACK_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_TRACK")
    private Long id;

	@OneToMany(mappedBy="track", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
	private List<Reading> prevReadings;
	//@OneToMany(mappedBy="track", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
	//private List<Reading> futureReadings;

    @Version
    private Long version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Reading> getPrevReadings() {
		return prevReadings;
	}

	public void setPrevReadings(List<Reading> prevReadings) {
		this.prevReadings = prevReadings;
	}
/*
	public List<Reading> getFutureReadings() {
		return futureReadings;
	}

	public void setFutureReadings(List<Reading> futureReadings) {
		this.futureReadings = futureReadings;
	}
*/
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

    
}
