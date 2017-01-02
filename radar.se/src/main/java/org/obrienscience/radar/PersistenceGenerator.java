package org.obrienscience.radar;

import org.obrienscience.radar.integration.ApplicationService;
import org.obrienscience.radar.integration.LiveRadarService;

public class PersistenceGenerator {
	private ApplicationService service;
	
	public PersistenceGenerator() {
	}

	public ApplicationService getService() {
		if(null == service) {
			service = new LiveRadarService();
		}
		return service;
	}

	public void setService(ApplicationService service) {
		this.service = service;
	}

	public void populate() {
		getService().prePopulateDatabase();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PersistenceGenerator generator = new PersistenceGenerator();
		generator.populate();

	}

}
