package org.obrienscience.radar;

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class AnimApplet2 extends Applet implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = -5412190348711102401L;
    public static final String MAX_HEIGHT = "480";
    public static final String MAX_WIDTH = "640";
    /** we are running as an applet unless we enter main() */
    private static boolean isApplet = true;
    
    // vars used to blink objects
    protected boolean bvOddFrame = true;
    protected int ivFrameCount = 0;
    protected int ivFrameFlipCount = 10;
    protected int ivFrames = 0;
    protected int xPos, yPos = 0;
    protected String eventString;
    protected int counter = 0;

    //protected static int izMaxWidth, izMaxHeight, izSleepTime = 1; // milliseconds to sleep
    protected static int izMaxWidth, izMaxHeight = 1; // milliseconds to sleep
    // private custom objects

    // The next two objects are for double-buffering
    protected Graphics gContext; // off-screen graphics context
    protected Image buffer; // buffer in which to draw image

    // popup menu
    protected String svPopupNames[] = { "Erase", "Reset", "Randomize" };
    protected MenuItem thePopupMenuItems[];
    protected PopupMenu aPopupMenu;

    private int originXPixel;
    private int originYPixel;
    private int radiusX;
    private int radiusY;
    private int extentX;
    private int extentY;
    
    // load the images when the applet begins executing
    @Override
    public void init() {
        //this.genericInit();
        // applet listen for mouse events, ok
        addMouseListener(this);
        addMouseMotionListener(this);
        if (isApplet) {
            String maxHeight = getParameter("maxHeight");
            if(null == maxHeight || maxHeight.length() < 1) {
                maxHeight = MAX_HEIGHT;
            }
            String maxWidth = getParameter("maxWidth");        
            if(null == maxWidth || maxWidth.length() < 1) {
                maxWidth = MAX_WIDTH;
            }
            izMaxHeight = Integer.parseInt(maxHeight);
            izMaxWidth = Integer.parseInt(maxWidth);
        }

        // create popup menu
        aPopupMenu = new PopupMenu("Popup");
        thePopupMenuItems = new MenuItem[svPopupNames.length];

        for (int i = 0; i < svPopupNames.length; i++) {
            thePopupMenuItems[i] = new MenuItem(svPopupNames[i]);
            aPopupMenu.add(thePopupMenuItems[i]);
            thePopupMenuItems[i].addActionListener(this);
        }

        // add popup menu to this canvas
        add(aPopupMenu);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

        // create graphics subsystem
        buffer = createImage(izMaxWidth, izMaxHeight); // create image buffer
        gContext = buffer.getGraphics(); // get graphics context

        // set background of buffer to black
        gContext.setColor(Color.black);
        gContext.fillRect(0, 0, izMaxWidth, izMaxHeight + 30);
    }

    private void setValues(String event, int x, int y) {
        eventString = event;
        xPos = x;
        yPos = y; // repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        setValues("Clicked", e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setValues("Pressed", e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        setValues("Released", e.getX(), e.getY());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setValues("Entered", e.getX(), e.getY());
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setValues("Exited", e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setValues("Dragging", e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setValues("Moving", e.getX(), e.getY());
    }

    // capture popup menu event
    @Override
    public void processMouseEvent(MouseEvent e) {
        if (e.isPopupTrigger())
            aPopupMenu.show(this, e.getX(), e.getY());
        super.processMouseEvent(e);
    }

    // process popup menu event
    @Override
    public void actionPerformed(ActionEvent e) {
        // "Erase"
        if (e.getSource() == thePopupMenuItems[0]) {        }

        // "Reset"
        if (e.getSource() == thePopupMenuItems[1]) {
            init();
        }

        // "Randomize"
        if (e.getSource() == thePopupMenuItems[2]) {       }
    }

    // start the applet
    @Override
    public void start() {    }

    // stop the applet
    @Override
    public void stop() {    }

    // display the image in the Applet's Graphics context
    @Override
    public void paint(Graphics g) {
        g.drawImage(buffer, 0, 0, this);
        // clear previous image from buffer
        if(null != gContext) { // image may not be ready
            gContext.setColor(Color.black);
            gContext.fillRect(0, 0, izMaxWidth, izMaxHeight);

            // update frame and frameflip count
            ivFrames++;
            if (ivFrameCount < ivFrameFlipCount) {
                bvOddFrame = false;
                ivFrameCount++;
            } else {
                if (ivFrameCount < ivFrameFlipCount * 2) {
                    bvOddFrame = true;
                    ivFrameCount++;
                } else {
                    bvOddFrame = false;
                    ivFrameCount = 0;
                }
            }
        }

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            showStatus(e.toString());
        }
        repaint(); // display buffered image
    }

    // override update to eliminate flicker
    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public abstract void genericInit();
    
    public void applicationInit() {
        isApplet = false;
        izMaxWidth = 478;//580;//478;//384;
        izMaxHeight = 478;//384; // try to use a power of 2
        setOriginXPixel(izMaxWidth >> 1);
        setOriginYPixel(izMaxHeight >> 1);
        setRadiusX(izMaxWidth >> 1);
        setRadiusY(izMaxHeight >> 1);
        setExtentX(izMaxWidth);
        setExtentY(izMaxHeight);
        this.genericInit();
        
        Frame aFrame = new Frame("Radar (c) 2013 F.Michael O'Brien");
        //AnimApplet2 anApplet = new AnimApplet2();
        aFrame.add("Center", this);
        aFrame.setSize(izMaxWidth + 10, izMaxHeight + 30);
        //aFrame.show();
        aFrame.setVisible(true);
        this.init();
        this.start();        
    }
    
    public void tearDown() {
        this.stop();
    }
    
/*    // allow application use outside of browser, appletviewer
    public static void main(String args[]) {
        AnimApplet2 anApplet = new AnimApplet2();
        anApplet.applicationInit();
    }*/

    @Override
    public String getAppletInfo() {
        return "(2013) Written by F.Michael O'Brien";
    }
    
    public int getOriginXPixel() {        return originXPixel;    }
    public void setOriginXPixel(int originXPixel) {        this.originXPixel = originXPixel;    }
    public int getOriginYPixel() {        return originYPixel;    }
    public void setOriginYPixel(int originYPixel) {        this.originYPixel = originYPixel;    }
    public int getRadiusX() {        return radiusX;    }
    public void setRadiusX(int radiusX) {        this.radiusX = radiusX;    }
    public int getRadiusY() {        return radiusY;    }
    public void setRadiusY(int radiusY) {        this.radiusY = radiusY;    }
    public int getExtentX() {        return extentX;    }
    public void setExtentX(int extentX) {        this.extentX = extentX;    }
    public int getExtentY() {        return extentY;    }
    public void setExtentY(int extentY) {        this.extentY = extentY;    }
    public Graphics getgContext() {        return gContext;    }    
}
