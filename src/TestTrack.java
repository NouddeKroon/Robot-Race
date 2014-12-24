import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Created by Noud on 12/22/2014.
 */

//Track class for drawing the test track from the assignment.
public class TestTrack extends Track {
    // Dimension of the basic oval track specified in the assignment.
    final double ovalTrackCosRadius = 10;
    final double ovalTrackSinRadius = 14;
    double trackWidth = 4;

    //Method return a point on the oval using the formula given in assignment.
    public Vector getPoint(double t) {
        double x = ovalTrackCosRadius * Math.cos(Math.PI * 2 * t);
        double y = ovalTrackSinRadius * Math.sin(Math.PI * 2 * t);
        return new Vector(x, y, 1);
    }

    /**
     * Method returning a position coordinate on the oval, given a distance traveled and lane number. Note that lane
     * 0 is the rightmost lane on the track. Calculates a vector "toLeft" from the cross between the tangent and the normal
     * in order to create the appropriate offset for each lane.
    **/
    public Vector getPositionOnLane(double s, int laneNr) {
        double t;

        //Since tangent already converts a distance to a value t on the track itself, call this method before conversion.
        Vector tangent = getTangent(s, laneNr);

        //Convert a given distance s to a corresponding t, using the precalulated circumference of each track. Note
        // that since the curvature is not constant, this is an approximation.
        if (laneNr == 3) {
            t = s / 67.16;
        } else if (laneNr == 2) {
            t = s / 73.34;
        } else if (laneNr == 1) {
            t = s / 79.54;
        } else {
            t = s / 85.75;
        }
        Vector position = getPoint(t);
        Vector toLeft = Vector.Z.cross(tangent).normalized();

        position = position.add(toLeft.scale(-1.5 + laneNr));
        return position;
    }

    /**
     * A tangent to a 2D outline of oval shape. The below functions are obtained by taking dx/dt and dy/dt of the
     * function in getPoint. First converts a given distance s to a corresponding t used in the formula.
    **/
    public Vector getTangent(double s, int laneNr) {

        double t;
        if (laneNr ==3){
            t = s / 67.16;
        } else if (laneNr == 2) {
            t = s / 73.34;
        } else if (laneNr == 1) {
            t = s / 79.54;
        } else {
            t = s / 85.75;
        }
        double x = -2 * Math.PI * ovalTrackCosRadius * Math.sin(2 * Math.PI * t);
        double y = 2 * Math.PI * ovalTrackSinRadius * Math.cos(2 * Math.PI * t);

        return new Vector(x, y, 0).normalized();
    }

    //Overloads the getTangent vector for class-local calculations, taking t as an argument instead of a distance s.
    private Vector getTangent(double t) {
        double x = -2 * Math.PI * ovalTrackCosRadius * Math.sin(2 * Math.PI * t);
        double y = 2 * Math.PI * ovalTrackSinRadius * Math.cos(2 * Math.PI * t);

        return new Vector(x, y, 0).normalized();
    }

    //Method returning a normal vector.
    public Vector getNormal(double s, int laneNr) {
        // The surface of the basic oval track is always coplanar with the XOY plane therefore the normal is
        // along the z-axis.
        return Vector.Z;
    }

    /**
     * Method that draws the test track, using the following variables:
     *
     * toLeft: Vector pointing to the left relative to the tangent vector.
     * innerRingPos: Vector pointing to the inner edge of the track.
     * outRingPos: Vector pointing to the outer edge of the track.
     * nextPoint: Vector point to the next point.
     * Note: of most of these variables a "next" version is used as well, having the same meaning at the position t+dt instead
     * of at t.
     * t: The loop variable running over the segment from 0 to 1.
     * dt: The step interval of the loop.
     * The method draws triangles between the "current" positions, and the "next" positions.
     */
    public void draw(GL2 gl) {
        double dt = 0.01;   //step size.
        gl.glColor3f(1f, 1f, 0f);

        //Since the "next" variables are passed to the current variables in the loop, we need to calculate these before
        // we enter the loop:
        Vector toLeftNext = Vector.Z.cross(getTangent(0)).normalized();
        Vector nextPoint = getPoint(0);
        Vector innerRingPosNext = nextPoint.add(toLeftNext.scale(trackWidth / 2.0));
        Vector outerRingPosNext = nextPoint.add(toLeftNext.scale(-trackWidth / 2.0));

        for (double t = 0; t < 1; t = t + dt) {
            //Pass on the "next" variables to the "current" variables, as well as calculate the new "next" variables.
            Vector toLeft = toLeftNext;
            toLeftNext = Vector.Z.cross(getTangent((t + dt))).normalized();
            nextPoint = getPoint(t + dt);
            Vector innerRingPosCurrent = innerRingPosNext;
            innerRingPosNext = nextPoint.add(toLeftNext.scale(trackWidth / 2.0));
            Vector outerRingPosCurrent = outerRingPosNext;
            outerRingPosNext = nextPoint.add(toLeftNext.scale(-trackWidth / 2.0));

            //Draw the inner wall:
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z <= 1; z = z + 0.25) {
                gl.glNormal3d(toLeft.x(), toLeft.y(), toLeft.z());
                gl.glVertex3d(innerRingPosCurrent.x(), innerRingPosCurrent.y(), z);

                gl.glNormal3d(toLeftNext.x(), toLeftNext.y(), toLeftNext.z());
                gl.glVertex3d(innerRingPosNext.x(), innerRingPosNext.y(), z);
            }
            gl.glEnd();

            //Draw the outer wall:
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z < 1; z = z + 0.1) {

                gl.glNormal3d(-toLeft.x(), -toLeft.y(), -toLeft.z());
                gl.glVertex3d(outerRingPosCurrent.x(), outerRingPosCurrent.y(), z);

                gl.glNormal3d(-toLeftNext.x(), -toLeftNext.y(), -toLeftNext.z());
                gl.glVertex3d(outerRingPosNext.x(), outerRingPosNext.y(), z);
            }
            gl.glEnd();

            //Use the drawTestTrackSurface method to draw the top surface of the track.
            gl.glNormal3d(0, 0, 1);
            drawTestTrackSurface(innerRingPosCurrent, toLeft, innerRingPosNext, toLeftNext, gl);

            //Translate 2 meters downwards, and draw the bottom surface, using an inverted normal vector.
            gl.glPushMatrix();
            gl.glTranslated(0, 0, -2);
            gl.glNormal3d(0, 0, -1);
            drawTestTrackSurface(innerRingPosCurrent, toLeft, innerRingPosNext, toLeftNext, gl);
            gl.glPopMatrix();
        }
    }

    //Method that draws a surface of the appropriate width given the rightmost starting points and the vectors pointing
    // to the left.
    private void drawTestTrackSurface(Vector innerRingPosCurrent, Vector toLeft,
                                      Vector innerRingPosNext, Vector toLeftNext, GL2 gl) {
        double dw = 0.25;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            //The next two points to draw between are calculated by adding a scaled toLeft vector to the two innerRingPos
            // vectors.
            Vector firstPoint = innerRingPosCurrent.add(toLeft.scale(-trackWidth * w));
            Vector secondPoint = innerRingPosNext.add(toLeftNext.scale(-trackWidth * w));
            gl.glVertex3d(firstPoint.x(), firstPoint.y(), firstPoint.z());
            gl.glVertex3d(secondPoint.x(), secondPoint.y(), secondPoint.z());
        }
        gl.glEnd();
    }

}
