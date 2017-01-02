package org.obrienscience.radar.model;

import javax.persistence.Entity;

@Entity
public class SatSite extends Site {
	private GPS dishGPS;
	public GPS getDishGPS() {		return dishGPS;	}
	public void setDishGPS(GPS dishGPS) {		this.dishGPS = dishGPS;	}
}
