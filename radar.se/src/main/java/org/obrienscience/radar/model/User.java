package org.obrienscience.radar.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@Entity
public class User {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_USER", sequenceName="EL_USER_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_USER")
    private Long id;

    @OneToOne
    private GPS gps;

    @Version
    private Long version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GPS getGps() {
		return gps;
	}

	public void setGps(GPS gps) {
		this.gps = gps;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
