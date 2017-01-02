package org.obrienscience.radar.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.Version;

//@MappedSuperclass
@Entity
//@TypeConverter(name="BigIntegerToString",dataType=String.class,objectType=BigInteger.class)
public class Reading {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_READING", sequenceName="EL_READING_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_READING")
    private Long id;

    //@OneToOne
    //private Site site;
    private Short x;
    private Short y;
    @OneToOne
    private GPS gps;
    private Byte intensity;
    private Short windSpeed;
    private Short windDirection;
    
	//@Column(nullable=true)
    @ManyToOne(fetch=FetchType.EAGER)
    private Event event;
    @ManyToOne(fetch=FetchType.EAGER)
    private Sweep sweep;
    @ManyToOne(fetch=FetchType.EAGER)
    private FutureTrack futureTrack;
    @ManyToOne(fetch=FetchType.EAGER)
    private Track track;
    
    public FutureTrack getFutureTrack() {
        return futureTrack;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public void setFutureTrack(FutureTrack futureTrack) {
        this.futureTrack = futureTrack;
    }
    @Version
    private Long version;

    @Override
    public String toString() {
        StringBuffer aBuffer = new StringBuffer("Reading");//getClass().getSimpleName());
        aBuffer.append("@");
        aBuffer.append(hashCode());
        aBuffer.append("( id: ");
        aBuffer.append(getId());
        aBuffer.append(")");
        return aBuffer.toString();
    }    
    
    // Simple get/set
    public Long getId() {        					return id;    }
    public void setId(Long id) {        			this.id = id;    }
    public Long getVersion() {        	     		return version;    }
    public void setVersion(Long version) {        	this.version = version;    }
    //public Site getSite() {		return site;	}
	//public void setSite(Site site) {		this.site = site;	}
	public Short getX() {		return x;	}
	public void setX(Short x) {		this.x = x;	}
	public Short getY() {		return y;	}
	public void setY(Short y) {		this.y = y;	}
	public GPS getGPS() {		return gps;	}
	public void setGPS(GPS gps) {		this.gps = gps;	}
	public Byte getIntensity() {		return intensity;	}
	public void setIntensity(Byte intensity) {		this.intensity = intensity;	}
//	public Date getDate() {		return date;	}
//	public void setDate(Date date) {		this.date = date;	}
	public Short getWindSpeed() {		return windSpeed;	}
	public void setWindSpeed(Short windSpeed) {		this.windSpeed = windSpeed;	}
	public Short getWindDirection() {		return windDirection;	}
	public void setWindDirection(Short windDirection) {		this.windDirection = windDirection;	}
	public Event getEvent() {		return event;	}
	public void setEvent(Event event) {		this.event = event;	}
    public GPS getGps() {
        return gps;
    }
    public void setGps(GPS gps) {
        this.gps = gps;
    }
    public Sweep getSweep() {
        return sweep;
    }
    public void setSweep(Sweep sweep) {
        this.sweep = sweep;
    }
    
}
