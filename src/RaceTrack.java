import robotrace.GlobalState;
import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Implementation of a race track that is made from Bezier segments.
 */
class RaceTrack {
    GlobalState gs;

    // Dimension of the basic oval track specified in the assignment.
    final double ovalTrackCosRadius = 10;
    final double ovalTrackSinRadius = 14;
    double trackWidth = 4;
    double trackHeight = 2;
    int displayList;

    /** Keep track of which track this instance is to draw. */
    private int trackNr;

    /** Maintain number of available lanes to walk on. */
    private int trackLanes;

    /** Array with control points for the O-track. */
    private Vector[] controlPointsOTrack;

    /** Array with control points for the L-track. */
    private Vector[] controlPointsLTrack;

    /** Array with control points for the C-track. */
    private Vector[] controlPointsCTrack;

    /** Array with control points for the custom track. */
    private Vector[] controlPointsCustomTrack;

    /**
     * Constructs the race track, sets up display lists.
     */
    public RaceTrack(int trackNr, int trackLanes, GlobalState gs) {
        this.trackNr = trackNr;
        this.trackLanes = trackLanes;
        this.gs = gs;
    }

    // Specify (after instantiation) which of the tracks to use. TRACKNR specifies tracks from 0 to TODO: max track nr.
    public void setTrackNr(int trackNr) {
        // TODO: when adding display_list clear them when changing track nrs.
        this.trackNr = trackNr;
    }


    /**
     * Draws this track, based on the selected track number.
     */

    private void drawTestTrack(GL2 gl) {
        double dt = 0.01;

        Vector centreToInnerNext = Vector.Z.cross(getTangent(0)).normalized();
        Vector nextPoint = getPoint(0);
        Vector innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth / 2.0));
        Vector outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth / 2.0));
        for (double t = 0; t < 1; t = t + dt) {
            Vector centreToInner = centreToInnerNext;
            centreToInnerNext = Vector.Z.cross(getTangent(t+dt)).normalized();
            nextPoint = getPoint(t+dt);
            Vector innerRingPosCurrent = innerRingPosNext;
            innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth/2.0));
            Vector outerRingPosCurrent = outerRingPosNext;
            outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth/2.0));

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z<=1; z = z+0.25) {

                gl.glNormal3d(centreToInner.x(), centreToInner.y(), centreToInner.z());
                gl.glVertex3d(innerRingPosCurrent.x(),innerRingPosCurrent.y(),z);

                gl.glNormal3d(centreToInnerNext.x(),centreToInnerNext.y(),centreToInnerNext.z());
                gl.glVertex3d(innerRingPosNext.x(), innerRingPosNext.y(), z);
            }
            gl.glEnd();

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z<1; z = z+0.1) {

                gl.glNormal3d(-centreToInner.x(),-centreToInner.y(),-centreToInner.z());
                gl.glVertex3d(outerRingPosCurrent.x(),outerRingPosCurrent.y(),z);

                gl.glNormal3d(-centreToInnerNext.x(),-centreToInnerNext.y(),-centreToInnerNext.z());
                gl.glVertex3d(outerRingPosNext.x(), outerRingPosNext.y(), z);
            }
            gl.glEnd();

            gl.glNormal3d(0, 0, 1);
            drawTestTrackSurface(innerRingPosCurrent, centreToInner, innerRingPosNext, centreToInnerNext, gl);

            gl.glPushMatrix();
            gl.glTranslated(0,0,-trackHeight);
            gl.glNormal3d(0, 0, -1);
            drawTestTrackSurface(innerRingPosCurrent, centreToInner, innerRingPosNext, centreToInnerNext, gl);
            gl.glPopMatrix();
        }

    }

    private void drawTestTrackSurface(Vector innerRingPosCurrent, Vector centreToInner,
                                      Vector innerRingPosNext, Vector centreToInnerNext, GL2 gl) {
        double dw = 0.25;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            Vector innerPoint0 = innerRingPosCurrent.add(centreToInner.scale(-trackWidth * w));
            Vector innerPoint1  = innerRingPosNext.add(centreToInnerNext.scale(-trackWidth * w));
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());
        }
        gl.glEnd();
    }

    

    public void draw(GL2 gl) {
        // The test track is selected
        if (0 == trackNr) {
            if (displayList == 0) {
                displayList = gl.glGenLists(1);
                gl.glNewList(displayList, GL2.GL_COMPILE);
                drawTestTrack(gl);
//                gl.glTranslated(0, 0, 1);
//                drawSimpleTop(gl, true);
//                gl.glTranslated(0, 0, -2);
//                drawSimpleTop(gl, false);
                gl.glEndList();
                } else {
                gl.glCallList(displayList);
            }
            // code goes here ...

            // The O-track is selected
        } else if (1 == trackNr) {
            // code goes here ...

            // The L-track is selected
        } else if (2 == trackNr) {
            // code goes here ...

            // The C-track is selected
        } else if (3 == trackNr) {
            // code goes here ...

            // The custom track is selected
        } else if (4 == trackNr) {
            // code goes here ...

        }
    }

    /**
     * Returns the position of the curve at 0 <= {@code t} <= 1.
     */
    public Vector getPoint(double t) {
        if (0 == trackNr) {
            // A basic 2D (z=0) outline of oval shape.
            double x = ovalTrackCosRadius * Math.cos(Math.PI * 2 * t);
            double y = ovalTrackSinRadius * Math.sin(Math.PI * 2 * t);

            return new Vector(x, y, 1);
        } else if (1 == trackNr) {
            // code goes here ...

            // The L-track is selected
        } else if (2 == trackNr) {
            // code goes here ...

            // The C-track is selected
        } else if (3 == trackNr) {
            // code goes here ...

            // The custom track is selected
        } else if (4 == trackNr) {
            // code goes here ...

        }

        return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
    }

    /**
     * Calculate the point at the center of a walkable lane.
     *
     * @param laneNr number of the lane. Must be non-negative and smaller than this.trackLanes.
     * @return Point of center of the lane
     */
    public Vector getPositionOnLane(double t, int laneNr) {
        assert 0 <= laneNr && laneNr < trackLanes;

        // If (single track || middle track of uneven number of tracks) {
        if (trackLanes == 1 || ((trackLanes % 2 == 1) && laneNr == (trackLanes + 1) / 2)) {
            // This lane is centered on the track, getPoint already returns the center of the track.
            return getPoint(t);
        }

        // By taking the cross product of the tangent and the normal we get a vector along the surface from
        // the center of the track toward the outer bound of it.
        // No need to normalize, the tangent and normal are orthogonal and unit length.
        Vector normal = getNormal(t);
        Vector alongSurface = getTangent(t).cross(normal);

        double halfLaneWidth = trackWidth / (2 * trackLanes);
        double halfLanesFromCenter;
        if (laneNr < (trackLanes / 2)) {
            // we need an inner lane thus the vector must be reversed to point inward.
            alongSurface = alongSurface.scale((-1));

            halfLanesFromCenter = trackLanes - laneNr * 2 - 1;
        } else {
            halfLanesFromCenter = laneNr * 2 - trackLanes + 1;
        }

        // Get the center point add the displacement along the width and add displacement along the height of the track.
        return getPoint(t).add(alongSurface.scale(halfLaneWidth * halfLanesFromCenter));
    }

    /**
     * Returns the normalized tangent of the curve at 0 <= {@code t} <= 1.
     */
    public Vector getTangent(double t) {
        if (0 == trackNr) {
            // A tangent to a 2D outline of oval shape. The below functions are obtained by taking
            // dx/dt and dy/dt of the function in getPoint.
            double x = -2 * Math.PI * ovalTrackCosRadius * Math.sin(2 * Math.PI * t);
            double y = 2 * Math.PI * ovalTrackSinRadius * Math.cos(2 * Math.PI * t);

            return new Vector(x, y, 0).normalized();
        } else if (1 == trackNr) {
            // code goes here ...

            // The L-track is selected
        } else if (2 == trackNr) {
            // code goes here ...

            // The C-track is selected
        } else if (3 == trackNr) {
            // code goes here ...

            // The custom track is selected
        } else if (4 == trackNr) {
            // code goes here ...

        }

        return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
    }

    /**
     * Returns the (normalized) normal to the current track.
     */
    public Vector getNormal(double t) {
        if (0 == trackNr) {
            // The surface of the basic oval track is always coplanar with the XOY plane therefore the normal is
            // along the z-axis.
            return Vector.Z;
        } else if (1 == trackNr) {
            // code goes here ...

            // The L-track is selected
        } else if (2 == trackNr) {
            // code goes here ...

            // The C-track is selected
        } else if (3 == trackNr) {
            // code goes here ...

            // The custom track is selected
        } else if (4 == trackNr) {
            // code goes here ...

        }

        return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
    }
}