package org.obrienscience.radar.model;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@MappedSuperclass
public abstract class Site {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_SITE", sequenceName="EL_SITE_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_SITE")
	private Long id;
    @OneToOne
	private GPS imageTopLeftGPS;
    @OneToOne
	private GPS imageTopRightGPS;
    @OneToOne
	private GPS imageBottomLeftGPS;
    @OneToOne
	private GPS imageBottomRightGPS;
	private Short imageWidth;
	private Short imageHeight;
	private Byte imageRotation;
	private String name;
	private String longName;
	
	private int interval;
	
	/**
	 * URLs are of the form
	 * http://www.weatheroffice.gc.ca/data/radar/temp_image/XFT/XFT_PRECIP_RAIN_2011_05_30_23_00.GIF
	 * parameterized as follows between underscores
	 * 
	 */
	private String url;
	private String parameterizedUrl;
	private String parameterizedHistoricalDataUrl;
	
	@Version
    private Long version;

    public Long getVersion() {        return version;    }
	public Long getId() {		return id;	}
	public void setId(Long id) {		this.id = id;	}
	public GPS getImageTopLeftGPS() {		return imageTopLeftGPS;	}
	public void setImageTopLeftGPS(GPS imageTopLeftGPS) {		this.imageTopLeftGPS = imageTopLeftGPS;	}
	public GPS getImageTopRightGPS() {		return imageTopRightGPS;	}
	public void setImageTopRightGPS(GPS imageTopRightGPS) {		this.imageTopRightGPS = imageTopRightGPS;	}
	public GPS getImageBottomLeftGPS() {		return imageBottomLeftGPS;	}
	public void setImageBottomLeftGPS(GPS imageBottomLeftGPS) {		this.imageBottomLeftGPS = imageBottomLeftGPS;	}
	public GPS getImageBottomRightGPS() {		return imageBottomRightGPS;	}
	public void setImageBottomRightGPS(GPS imageBottomRightGPS) {		this.imageBottomRightGPS = imageBottomRightGPS;	}
	public Short getImageWidth() {		return imageWidth;	}
	public void setImageWidth(Short imageWidth) {		this.imageWidth = imageWidth;	}
	public Short getImageHeight() {		return imageHeight;	}
	public void setImageHeight(Short imageHeight) {		this.imageHeight = imageHeight;	}
	public Byte getImageRotation() {		return imageRotation;	}
	public void setImageRotation(Byte imageRotation) {		this.imageRotation = imageRotation;	}
	public String getName() {		return name;	}
	public void setName(String name) {		this.name = name;	}
	public String getUrl() {		return url;	}
	public void setUrl(String url) {		this.url = url;	}
    public String getParameterizedHistoricalDataUrl() {		return parameterizedHistoricalDataUrl;	}
	public void setParameterizedHistoricalDataUrl(			String parameterizedHistoricalDataUrl) {		this.parameterizedHistoricalDataUrl = parameterizedHistoricalDataUrl;	}
	public String getParameterizedUrl() {		return parameterizedUrl;	}
	public void setParameterizedUrl(String parameterizedUrl) {		this.parameterizedUrl = parameterizedUrl;	}
	public int getInterval() {		return interval;	}
	public void setInterval(int interval) {		this.interval = interval;	}
	public String getLongName() {		return longName;	}
	public void setLongName(String longName) {		this.longName = longName;	}
	
}
