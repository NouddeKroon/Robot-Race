import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Created by Noud on 12/22/2014.
 */
public class TestTrack extends Track {
    // Dimension of the basic oval track specified in the assignment.
    final double ovalTrackCosRadius = 10;
    final double ovalTrackSinRadius = 14;
    double trackWidth = 4;
    private int trackLanes = 4;

    public Vector getPoint(double s) {
        // A basic 2D (z=0) outline of oval shape.

        double x = ovalTrackCosRadius * Math.cos(Math.PI * 2 * s);
        double y = ovalTrackSinRadius * Math.sin(Math.PI * 2 * s);
        return new Vector(x, y, 1);
    }

    public Vector getPositionOnLane(double s, int laneNr) {
        assert 0 <= laneNr && laneNr < trackLanes;


        Vector tangent = getTangent(s, laneNr);
        s = s / 50.0;
        Vector position = getPoint(s);
        Vector toLeft = Vector.Z.cross(tangent).normalized();

        position = position.add(toLeft.scale(-1.5));
        position = position.add(toLeft.scale(laneNr));
        return position;

//        // If (single track || middle track of uneven number of tracks) {
//        if (trackLanes == 1 || ((trackLanes % 2 == 1) && laneNr == (trackLanes + 1) / 2)) {
//            // This lane is centered on the track, getPoint already returns the center of the track.
//            return getPoint(s);
//        }
//
//        // By taking the cross product of the tangent and the normal we get a vector along the surface from
//        // the center of the track toward the outer bound of it.
//        // No need to normalize, the tangent and normal are orthogonal and unit length.
//        Vector normal = getNormal(s, laneNr);
//        Vector alongSurface = getTangent(s, laneNr).cross(normal);
//
//        double halfLaneWidth = trackWidth / (2 * trackLanes);
//        double halfLanesFromCenter;
//        if (laneNr < (trackLanes / 2)) {
//            // we need an inner lane thus the vector must be reversed to point inward.
//            alongSurface = alongSurface.scale((-1));
//
//            halfLanesFromCenter = trackLanes - laneNr * 2 - 1;
//        } else {
//            halfLanesFromCenter = laneNr * 2 - trackLanes + 1;
//        }
//
//        // Get the center point add the displacement along the width and add displacement along the height of the track.
//        return getPoint(s).add(alongSurface.scale(halfLaneWidth * halfLanesFromCenter));
    }

    public Vector getTangent(double t, int laneNr) {
        // A tangent to a 2D outline of oval shape. The below functions are obtained by taking
        // dx/dt and dy/dt of the function in getPoint.
        t = t / 50.0;
        double x = -2 * Math.PI * ovalTrackCosRadius * Math.sin(2 * Math.PI * t);
        double y = 2 * Math.PI * ovalTrackSinRadius * Math.cos(2 * Math.PI * t);

        return new Vector(x, y, 0).normalized();
    }

    public Vector getNormal(double s, int laneNr) {
        // The surface of the basic oval track is always coplanar with the XOY plane therefore the normal is
        // along the z-axis.
        return Vector.Z;
    }


    public void draw(GL2 gl) {
        double dt = 0.01;

        gl.glColor3f(1f, 1f, 0f);
        Vector centreToInnerNext = Vector.Z.cross(getTangent(0, 0)).normalized();
        Vector nextPoint = getPoint(0);
        Vector innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth / 2.0));
        Vector outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth / 2.0));
        for (double t = 0; t < 1; t = t + dt) {
            Vector centreToInner = centreToInnerNext;
            centreToInnerNext = Vector.Z.cross(getTangent((t + dt) * 50.0, 0)).normalized();
            nextPoint = getPoint(t + dt);
            Vector innerRingPosCurrent = innerRingPosNext;
            innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth / 2.0));
            Vector outerRingPosCurrent = outerRingPosNext;
            outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth / 2.0));

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z <= 1; z = z + 0.25) {

                gl.glNormal3d(centreToInner.x(), centreToInner.y(), centreToInner.z());
                gl.glVertex3d(innerRingPosCurrent.x(), innerRingPosCurrent.y(), z);

                gl.glNormal3d(centreToInnerNext.x(), centreToInnerNext.y(), centreToInnerNext.z());
                gl.glVertex3d(innerRingPosNext.x(), innerRingPosNext.y(), z);
            }
            gl.glEnd();

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z < 1; z = z + 0.1) {

                gl.glNormal3d(-centreToInner.x(), -centreToInner.y(), -centreToInner.z());
                gl.glVertex3d(outerRingPosCurrent.x(), outerRingPosCurrent.y(), z);

                gl.glNormal3d(-centreToInnerNext.x(), -centreToInnerNext.y(), -centreToInnerNext.z());
                gl.glVertex3d(outerRingPosNext.x(), outerRingPosNext.y(), z);
            }
            gl.glEnd();

            gl.glNormal3d(0, 0, 1);
            drawTestTrackSurface(innerRingPosCurrent, centreToInner, innerRingPosNext, centreToInnerNext, gl);

            gl.glPushMatrix();
            gl.glTranslated(0, 0, -2);
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
            Vector innerPoint1 = innerRingPosNext.add(centreToInnerNext.scale(-trackWidth * w));
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());
        }
        gl.glEnd();
    }

}
