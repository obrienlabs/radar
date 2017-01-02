package org.obrienscience.radar.model;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.swing.ImageIcon;

@Entity
public class Sweep {

    @Id
    @SequenceGenerator(name="EL_SEQUENCE_SWEEP", sequenceName="EL_SWEEP_SEQ", allocationSize=10)
    @GeneratedValue(generator="EL_SEQUENCE_SWEEP")
    private Long id;

    @ManyToOne(cascade=CascadeType.PERSIST)
    private RadarSite site;
    
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name="image")
    private byte[] filteredImage;	// original image is on HD, filtered image is persisted
    
    @Transient
    private Image bufferedImage; // must keep in sync with filteredImage;
    
	@OneToMany(mappedBy="sweep", fetch=FetchType.EAGER,cascade=CascadeType.ALL)
	private List<Reading> readings;
    
    //@Temporal(value = null)
    private String timestamp;

    /**
     * Whether the image was a live capture without a destructive overlay - or was historical (with overlay)
     * Note: when no historical data exists - Environment Canada will return the next image (10 min later in the sequence)
     */
    private Boolean isHistorical;

	@Version
    private Long version;

	/** clear all references */
	public void clear() {
		setSite(null);
		setReadings(null);
		bufferedImage = null;
		filteredImage = null;
	}
	
    /**
     * Translate from byte[] array to Image
     * @return
     */
    public Image getImage() {
    	Image image = null;
    	if(filteredImage != null && filteredImage.length > 0) {
    		image = new ImageIcon(filteredImage).getImage();
    		bufferedImage = image;
    	}
    	return image;
    	
    }
    
    /**
     * Translate from Image to byte[] array
     * @param image
     */
    public void setImage(Image image) {
    	BufferedImage aBufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    	Graphics2D g = aBufferedImage.createGraphics();
    	g.drawImage(image, 0,0,null);
    	g.dispose();
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	try {
    		ImageIO.write(aBufferedImage, "GIF", baos);
    		filteredImage = baos.toByteArray();
    		bufferedImage = image;
    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    		assert(false);
    	}
    }
    
	public byte[] getFilteredImage() {		return filteredImage;	}
	public void setFilteredImage(byte[] filteredImage) {		this.filteredImage = filteredImage;	}
	public Long getId() {		return id;	}
	public void setId(Long id) {		this.id = id;	}
	public Site getSite() {		return site;	}
	public void setSite(RadarSite site) {		this.site = site;	}
	public List<Reading> getReadings() {		return readings;	}
	public void setReadings(List<Reading> readings) {		this.readings = readings;	}
	public Long getVersion() {		return version;	}
	public void setVersion(Long version) {		this.version = version;	}
    public String getTimestamp() {        return timestamp;    }
    public void setTimestamp(String timestamp) {        this.timestamp = timestamp;    }
    public Boolean getIsHistorical() {		return isHistorical;	}
	public void setIsHistorical(Boolean isHistorical) {		this.isHistorical = isHistorical;	}
    
}
