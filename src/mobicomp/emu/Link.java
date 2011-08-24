package mobicomp.emu;

/*
 * class Link
 * 
 * this class is the internal representation of a link
 * 
 * a link can either be closed (a client sits on both ends) or open (the user is
 * currently dragging around on end of the link - the other end is fixed at a
 * client). there can be at most one open socket at the time
 *  
 */

import java.util.Vector;
import java.util.Iterator;
import java.net.DatagramPacket;
import java.awt.*;

import java.util.Random;

public class Link {
    // some constants for drawing
    public static final Color RUNNING_LINK_COLOR = new Color(0.0f, 0.0f, 1.0f);
    public static final Color SLEEPING_LINK_COLOR = new Color(1.0f, 0.0f, 0.0f);
    public static final Color USED_LINK_COLOR = new Color(0.0f, 1.0f, 0.0f);
    public static final int CATCH_RADIUS = 10;
    public static final int INTERSECTION_DISTANCE = 3;
    public static final int ARROW_LENGTH = 10;
    public static final int ARROW_WIDTH = 8;
    public static final int LINK_WIDTH = 2;

    // some constants for feedback for send()
    // NO_GROUP_MEMBER means that the receiver has no socket that listens to
    // the multicast group of the packet
    public static final int PACKET_SENT = 1;
    public static final int PACKET_LOST = 2;
    public static final int NO_GROUP_MEMBER = 3;

    // some constants for delay settings
    public static final int NO_DELAY = 1;
    public static final int CONST_DELAY = 2;
    public static final int DIST_DELAY = 3;

    // some constants for error settings
    public static final int NO_ERROR = 1;
    public static final int CONST_ERROR = 2;
    public static final int DETERM_ERROR = 3;

    // two strokes for drawing the link
    private static final BasicStroke normal = new BasicStroke(LINK_WIDTH);

    private static final float dashPattern[] = { 8.0f };

    private static final BasicStroke dashed = new BasicStroke(LINK_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f);

    // start and endpoint
    private Client start;

    private Client end;

    private boolean isBidirectional;

    // delay and error settings
    private int delayType = NO_DELAY;

    private int errorType = NO_ERROR;

    private int constDelayMs = 0;

    private float distDelayFactor = 0;

    private float constErrorProb = 0.0f;

    private String errorPattern = new String();

    private int patternPos = 0; // current position in the error pattern

    private Random rand = new Random();

    // counters for successfully sent and lost packets
    private int succCount = 0;

    private int lostCount = 0;

    // flashing -> change color of the arrow
    private boolean flashingStart = false;

    private boolean flashingEnd = false;

    private FlashLinkThread flashThreadStart = null;

    private FlashLinkThread flashThreadEnd = null;

    // coordinates of start and endpoint
    private Point openEnd;

    // constructor to create an open link (other end not fixed yet)
    public Link(Client start, int endX, int endY) {
        this.start = start;
        this.end = null;
        this.openEnd = new Point(endX, endY);
        this.isBidirectional = true;
    }

    //	 constructor to create a closed link
    public Link(Client start, Client end, boolean isBidirectional) {
        if (start == null || end == null)
            throw new NullPointerException();

        this.start = start;
        this.end = end;
        this.isBidirectional = isBidirectional;
    }

    // constructor to create a closed bidirectional link
    public Link(Client start, Client end) {
        this(start, end, true);
    }

    // return the client on the end of the links which is !=sender
    public Client getReceiver(Client sender) {
        if (openEnd != null || this.end == null) {
            // an open link has no receiver!
            return null;
        }

        if (this.start == sender)
            return this.end;

        // the end node is sending and the link is marked as bidirectional
        // connection
        if ((this.end == sender) && this.isBidirectional)
            return this.start;

        // sender must be either start or end point of the link!
        return null;
    }

    // send a packet
    public int send(DatagramPacket p, Client sender, EmuSocket socket) {
        boolean sentSomething = false;

        Client receiver = getReceiver(sender);

        if (receiver == null)
            throw new RuntimeException("ERROR: send() called on wrong link");

        Vector receiverSockets = receiver.getSockets();

        if (!this.isNextPacketDeliverable()) {
            return Link.PACKET_LOST;
        }

        for (Iterator iter = receiverSockets.iterator(); iter.hasNext();) {
            EmuSocket curSocket = (EmuSocket) iter.next();

            if (curSocket.getGroups().contains(p.getAddress())) {
                // send the packet to the socket
                curSocket.receivePacket(p, this.getDelay());
                sentSomething = true;
            }
        }

        if (sentSomething) {
            if (Options.flashTime != 0) {
                // adjust color of arrow (-> flashing)
                if (receiver == this.start) {
                    // change the color of the link
                    if (flashThreadStart == null || !flashThreadStart.isAlive()) {
                        flashThreadStart = new FlashLinkThread(true);
                        flashThreadStart.start();
                    } else {
                        flashThreadStart.reset();
                    }
                } else {
                    // change the color of the link
                    if (flashThreadEnd == null || !flashThreadEnd.isAlive()) {
                        flashThreadEnd = new FlashLinkThread(false);
                        flashThreadEnd.start();
                    } else {
                        flashThreadEnd.reset();
                    }
                }
            }

            return Link.PACKET_SENT;
        } else {
            return Link.NO_GROUP_MEMBER;
        }
    }

    // test if the next packet is deliverable according to error settings
    public boolean isNextPacketDeliverable() {
        switch (errorType) {
        case NO_ERROR:
            succCount++;
            return true;

        case CONST_ERROR:
            if (rand.nextFloat() > constErrorProb / 100) {
                succCount++;
                return true;
            } else {
                lostCount++;
                return false;
            }

        case DETERM_ERROR:
            if (errorPattern.equals("")) {
                // pattern is empty -> packet successfull
                succCount++;
                return true;
            }

            patternPos %= errorPattern.length();

            if (errorPattern.charAt(patternPos++) == '+') {
                succCount++;
                return true;
            } else {
                lostCount++;
                return false;
            }

        default:
            throw new RuntimeException("ERROR: Illegal error type " + errorType);
        }
    }

    // get the delay of the next packet
    public int getDelay() {
        switch (delayType) {
        case NO_DELAY:
            return 0;

        case CONST_DELAY:
            return constDelayMs;

        case DIST_DELAY:
            // calculate distance between start and end node
            int x = start.getX() - end.getX();
            int y = start.getY() - end.getY();
            float dist = (float) Math.sqrt(x * x + y * y);
            return (int) (dist * distDelayFactor);

        default:
            throw new RuntimeException("ERROR: Illegal delay type " + delayType);
        }
    }

    public int getDelayType() {
        return delayType;
    }

    public void setDelayType(int type) {
        if ((type == NO_DELAY) || (type == CONST_DELAY) || (type == DIST_DELAY))
            delayType = type;
        else
            throw new RuntimeException("ERROR: Illegal delay type value " + type);
    }

    public int getErrorType() {
        return errorType;
    }

    public void setErrorType(int type) {
        if ((type == NO_ERROR) || (type == CONST_ERROR) || (type == DETERM_ERROR))
            errorType = type;
        else
            throw new RuntimeException("ERROR: Illegal error type value: " + type);
    }

    public int getConstDelayMs() {
        return constDelayMs;
    }

    public void setConstDelayMs(int delay) {
        if (delay >= 0) {
            constDelayMs = delay;
        } else {
            throw new RuntimeException("ERROR: Delay must be positive");
        }
    }

    public float getDelayFactor() {
        return distDelayFactor;
    }

    public void setDelayFactor(float factor) {
        if (factor >= 0) {
            distDelayFactor = factor;
        } else {
            throw new RuntimeException("ERROR: Delay factor must be positive");
        }
    }

    public float getConstErrorProb() {
        return constErrorProb;
    }

    public void setConstErrorProb(float prob) {
        if ((prob >= 0.0f) && (prob <= 100.0f)) {
            constErrorProb = prob;
        } else {
            throw new RuntimeException("ERROR: Illegal error probability");
        }
    }

    public String getErrorPattern() {
        return errorPattern;
    }

    public void setErrorPattern(String pattern) {
        // check whether the String contains only + and -
        for (int i = 0; i < pattern.length(); i++) {
            if ((pattern.charAt(i) != '+') && (pattern.charAt(i) != '-'))
                throw new RuntimeException("ERROR: Illegal chars in error pattern");
        }

        errorPattern = pattern;
    }

    // set the endpoint of an open link
    public void setEndpoint(int x, int y) {
        if ((end != null) || (openEnd == null))
            throw new RuntimeException("ERROR: Impossible to set coordinates of a closed link");

        openEnd.x = x;
        openEnd.y = y;
    }

    // set the endpoint of a closed link
    public void setEndpoint(Client dest) {
        if (end != null)
            throw new RuntimeException("ERROR: Link is already closed");

        end = dest;

        openEnd = null;
    }

    // returns true if this links connects c1 and c2
    public boolean conntects(Client c1, Client c2) {
        if ((start == c1 && end == c2) || (start == c2 && end == c1))
            return true;
        else
            return false;
    }

    // returns true if the position (x,y) is close to the start or end of this
    // link
    public boolean hasEdgeAt(int x, int y) {
        if (end == null)
            throw new RuntimeException("ERROR: intersect() not allowed on open links");

        Point startPoint = calcCoord(true);
        Point endPoint = calcCoord(false);

        if (Math.pow(x - startPoint.getX(), 2) + Math.pow(y - startPoint.getY(), 2) < Math.pow(CATCH_RADIUS, 2)) {
            // start point hit
            return true;
        }

        if (Math.pow(x - endPoint.getX(), 2) + Math.pow(y - endPoint.getY(), 2) < Math.pow(CATCH_RADIUS, 2)) {
            // end point hit
            return true;
        }

        return false;
    }

    // returns true if (x,y) is close to the link
    public boolean intersects(int x0, int y0) {

        if (end == null)
            throw new RuntimeException("ERROR: intersect() not allowed on open links");

        int x1,x2,y1,y2;
        
        x1 = start.getX();
        x2 = end.getX();
        y1 = start.getY();
        y2 = end.getY();
        
        // calculate the distance between the direct start-end connection and the point
        double o = Math.abs((x2-x1)*(y1-y0)-(x1-x0)*(y2-y1));
        double u = Math.sqrt(Math.pow((x2-x1),2)+Math.pow((y2-y1),2));
        
        if ((o/u) < INTERSECTION_DISTANCE) {
            return true;
        }

        return false;
    }

    // returns the endpoint which is closer to (x,y) than the other one
    public Client getPointNear(int x, int y) {
        if (start == null || end == null)
            throw new RuntimeException("ERROR: getPointNear() not allowed on open links");

        int distToStart = (int) Math.pow(start.getX() - x, 2) + (int) Math.pow(start.getY() - y, 2);

        int distToEnd = (int) Math.pow(end.getX() - x, 2) + (int) Math.pow(end.getY() - y, 2);

        if (distToStart < distToEnd) {
            return start;
        } else {
            return end;
        }
    }

    // re-open a closed link. client c is no more connected to the link
    public void setOpen(Client c) {
        if (c == null)
            throw new RuntimeException("ERROR: parameter for setOpen must be != null");

        if (c == start) {
            // exchange start and endpoint
            openEnd = calcCoord(true);
            start = end;
            end = null;

        } else if (c == end) {
            openEnd = calcCoord(false);
            end = null;
        } else
            throw new RuntimeException("ERROR: setOpen needs start or endpoint as parameter");
    }

    // calculate the coordinates of the startpoint (endpoint) if parameter is
    // true (false) of the arrow when drawing this link
    private Point calcCoord(boolean startpoint) {
        if (!startpoint && (end == null))
            return openEnd;

        int deltaX;
        int deltaY;

        if (end == null) {
            deltaX = start.getX() - openEnd.x;
            deltaY = start.getY() - openEnd.y;
        } else {
            deltaX = start.getX() - end.getX();
            deltaY = start.getY() - end.getY();
        }

        double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        double prop = (Client.RADIUS) / dist;

        int offsetX = (int) Math.round(deltaX * prop);
        int offsetY = (int) Math.round(deltaY * prop);

        if (startpoint) {
            return new Point(start.getX() - offsetX, start.getY() - offsetY);
        } else
            return new Point(end.getX() + offsetX, end.getY() + offsetY);
    }

    // draw the link
    public void draw(Graphics g1D) {
        // don't draw an open link if the end is inside of its startpoint
        if ((end == null) && (openEnd.distance(start.getX(), start.getY()) < Client.RADIUS))
            return;

        // activate anti aliasing
        Graphics2D g = (Graphics2D) g1D;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // set correct color
        if ((end != null) && start.isRunning() && end.isRunning())
            g.setColor(RUNNING_LINK_COLOR);
        else
            g.setColor(SLEEPING_LINK_COLOR);

        // calculate start- & endpoint and link length
        Point s = new Point();
        Point e = new Point();
        double dist;

        s.x = start.getX();
        s.y = start.getY();

        if (end == null) {
            e.x = openEnd.x;
            e.y = openEnd.y;
            dist = s.distance(e) + Client.RADIUS;
        } else {
            e.x = end.getX();
            e.y = end.getY();
            dist = s.distance(e);
        }

        int fact = 1; //  fact = +/- 1 (needed if s not on the left of e)

        if (s.x > e.x)
            fact = -1;

        // rotate g so that we can draw horizontally
        double theta = Math.atan(((float) (e.y - s.y)) / (e.x - s.x));
        g.rotate(theta, s.x, s.y);

        // draw the link
        Stroke old = g.getStroke();

        if (getErrorType() == NO_ERROR) {
            g.setStroke(normal);
        } else {
            g.setStroke(dashed);
        }
        if (this.isBidirectional) {
            g.drawLine(s.x + fact * (Client.RADIUS + ARROW_LENGTH), s.y, (s.x + fact * ((int) dist - Client.RADIUS - ARROW_LENGTH)), s.y);
        } else {
            g.drawLine(s.x + fact * Client.RADIUS, s.y, (s.x + fact * ((int) dist - Client.RADIUS - ARROW_LENGTH)), s.y);
        }
        g.setStroke(old);

        // draw the arrows
        //
        //  /| |\ ^ W = ARROW_WIDTH
        // < ------ > W L = ARROW_LENGTH
        //  \| |/ v
        //
        // <L>
        // <-- dist -->

        int arrowX[] = new int[3];
        int arrowY[] = new int[3];

        arrowX[0] = s.x + fact * (Client.RADIUS);
        arrowX[1] = s.x + fact * (Client.RADIUS + ARROW_LENGTH);
        arrowX[2] = s.x + fact * (Client.RADIUS + ARROW_LENGTH);

        arrowY[0] = s.y;
        arrowY[1] = s.y - ARROW_WIDTH / 2;
        arrowY[2] = s.y + ARROW_WIDTH / 2;

        if (this.isBidirectional) {
            if (flashingStart) {
                g.setColor(USED_LINK_COLOR);
                g.fillPolygon(arrowX, arrowY, 3);
                g.setColor(RUNNING_LINK_COLOR);
            } else {
                g.fillPolygon(arrowX, arrowY, 3);
            }
        } else {

        }

        arrowX[0] = s.x + fact * ((int) dist - Client.RADIUS);
        arrowX[1] = s.x + fact * ((int) dist - Client.RADIUS - ARROW_LENGTH);
        arrowX[2] = s.x + fact * ((int) dist - Client.RADIUS - ARROW_LENGTH);

        if (flashingEnd) {
            g.setColor(USED_LINK_COLOR);
            g.fillPolygon(arrowX, arrowY, 3);
            g.setColor(RUNNING_LINK_COLOR);
        } else {
            g.fillPolygon(arrowX, arrowY, 3);
        }

        // write delay & packet info to the link
        if (end != null) {
            // write delay above the link
            String delayText = Integer.toString(getDelay()) + " ms";

            if (delayText.equals("0 ms")) {
                delayText = "no delay";
            }

            int delayWidth = g.getFontMetrics().stringWidth(delayText);

            // draw the text only if there is enough space
            if (delayWidth < dist - 2 * Client.RADIUS)
                g.drawString(delayText, (int) (s.x + fact * dist / 2 - delayWidth / 2), s.y - 8);

            // write sent/lost packets below the link
            String packetText = "sent: " + Integer.toString(succCount) + " lost: " + Integer.toString(lostCount);

            String packetTextShort = Integer.toString(succCount) + " / " + Integer.toString(lostCount);

            int packetWidth = g.getFontMetrics().stringWidth(packetText);
            int packetWidthShort = g.getFontMetrics().stringWidth(packetTextShort);

            // draw the text only if there is enough space
            if (packetWidth < dist - 2 * Client.RADIUS)
                g.drawString(packetText, (int) (s.x + fact * dist / 2 - packetWidth / 2), s.y + 15);
            else if (packetWidthShort < dist - 2 * Client.RADIUS)
                g.drawString(packetTextShort, (int) (s.x + fact * dist / 2 - packetWidthShort / 2), s.y + 15);
        }

        // rotate g back
        g.rotate(-theta, s.x, s.y);

    }

    public String toString() {
        if (end == null) {
            return new String("Open link starting in " + start.getName());
        } else {
            return new String("Link between " + start.getName() + " and " + end.getName());
        }
    }

    // a private thread which changes the color of the arrows when required
    private class FlashLinkThread extends Thread {
        private long stopAt;

        boolean startArrow;

        public FlashLinkThread(boolean start) {
            super();
            startArrow = start;
        }

        public void run() {
            // change the color of the link
            if (startArrow)
                flashingStart = true;
            else
                flashingEnd = true;

            Emulator.getRef().redrawGraph();

            stopAt = System.currentTimeMillis() + Options.flashTime;

            while (System.currentTimeMillis() < stopAt) {
                try {
                    sleep(stopAt - System.currentTimeMillis());
                } catch (InterruptedException e) {
                    // d'oh...
                }
            }

            // switch back the color
            if (startArrow)
                flashingStart = false;
            else
                flashingEnd = false;

            Emulator.getRef().redrawGraph();
        }

        public void reset() {
            stopAt = System.currentTimeMillis() + Options.flashTime;
        }
    }

    // all those one-liners to get and set private fields...
    public Client getStart() {
        return start;
    }

    public Client getEnd() {
        return end;
    }

    public void setStart(Client start) {
        this.start = start;
    }

    public void setEnd(Client end) {
        this.end = end;
    }

    /**
     * @return Returns the isBidirectional.
     */
    public boolean isBidirectional() {
        return isBidirectional;
    }

    public void setBidirectional(boolean bi) {
        this.isBidirectional = bi;
    }
}