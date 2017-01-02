package org.obrienscience.radar.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class RadarSite extends Site {

    public static final int COLOR_BLACK = -16777216;
    
    /** Radar doppler intensity values 0=max, 14=min */
    public static final int PRECIP_INTENSITY_COLOR_CODES[] = {
        -10092391,
        -6736948,
        -64871,
        -65536,
        -39424,
        -26368,
        -13312,
        -205,
        -16751104,
        -16738048,
        -16724992,
        -16711834,
        -16737793,
        -6697729};
    public static final int PRECIP_INTENSITY_COLOR_CODES_SIZE = PRECIP_INTENSITY_COLOR_CODES.length;
    
    public static final int COLOR_WHITE =  -65794;
    
	@OneToMany(mappedBy="site", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
    private List<Sweep> sweeps;
	
	@OneToOne
	private GPS dishGPS;
	
	public GPS getDishGPS() {		return dishGPS;	}
	public void setDishGPS(GPS dishGPS) {		this.dishGPS = dishGPS;	}
    public List<Sweep> getSweeps() {        return sweeps;    }
    
    public void addSweep(Sweep sweep) {
    	if(null == sweeps) {
    		sweeps = new ArrayList<Sweep>();
    	}
    	sweeps.add(sweep);
    }

    public void removeSweep(Sweep sweep) {
    	sweeps.remove(sweep);
    }
    
    public void setSweeps(List<Sweep> sweeps) {        this.sweeps = sweeps;    }
	
}
