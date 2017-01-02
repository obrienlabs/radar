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
public class Event {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_EVENT", sequenceName="EL_EVENT_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_EVENT")
	private Long id;
	
	@OneToMany(mappedBy="event", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
	private List<Reading> readings;
	
    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Reading> getReadings() {
        return readings;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }
	
}
