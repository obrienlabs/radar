package org.obrienscience.radar.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@Entity
public class GPS {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_GPS", sequenceName="EL_GPS_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_GPS")
	private Long id;
	private Short longHour;
	private Short longMin;
	private Short longSec;
	private Short latHour;
	private Short latMin;
	private Short latSec;
	private Short elevation;
	
    @Version
    private Long version;

    public GPS() {
    	
    }
	public GPS(Short lhour, Short lmin, Short lsec, Short thour, Short tmin, Short tsec ) {
    	longHour = lhour; longMin = lmin; longSec = lsec;
    	latHour = thour; latMin = tmin; latSec = tsec;
    }
    
    public Long getVersion() {        return version;    }
    public Long getId() {        return id;    }
    public void setId(Long id) {        this.id = id;    }
    public Short getLongHour() {        return longHour;    }
    public void setLongHour(Short longHour) {        this.longHour = longHour;    }
    public Short getLongMin() {        return longMin;    }
    public void setLongMin(Short longMin) {        this.longMin = longMin;    }
    public Short getLongSec() {        return longSec;    }
    public void setLongSec(Short longSec) {        this.longSec = longSec;    }
    public Short getElevation() {        return elevation;    }
    public void setElevation(Short elevation) {        this.elevation = elevation;    }
    public Short getLatHour() {		return latHour;	}
	public void setLatHour(Short latHour) {		this.latHour = latHour;	}
	public Short getLatMin() {		return latMin;	}
	public void setLatMin(Short latMin) {		this.latMin = latMin;	}
	public Short getLatSec() {		return latSec;	}
	public void setLatSec(Short latSec) {		this.latSec = latSec;	}
}
