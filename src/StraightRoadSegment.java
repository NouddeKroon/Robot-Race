import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Created by Noud on 12/20/2014.
 */

/**
 *
 */
public class StraightRoadSegment extends RoadSegment {
    Vector startPoint;                      //Vector poiting towards the start point of the road segment.
    Vector endPoint;                        //Vector pointing towards the end point of the road segment.
    int resolution;                         //Resolution of the road segment.
    double trackWidth = 4.0;
    double dt;
    Vector toLeft;                          //Vector pointing to the left in regard to the tangent vector.
    Vector differenceVector;                //Vector pointing from start point to end point.
    Vector normalVector;

    StraightRoadSegment(Vector startPoint, Vector endPoint, int resolution) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.resolution = resolution;
    }

    @Override
    /**
     * When given a distance s and laneNr give the appropriate coordinate. Use the difference vector the find
     * the appropriate point, by adding the differenceVector scaled with a factor s / differenceVector.length to the
     * startPoint. Then move to the side to account for the lane.
     */
    public Vector getCoordinate(double s, int laneNr) {
        Vector centrePointOnTrack = startPoint.add(differenceVector.scale(s / differenceVector.length()));
        Vector coordinate = centrePointOnTrack.add(toLeft.scale(0.375 * trackWidth - (double)laneNr));
        return coordinate;
    }




    /**
     * Method that draws the test track, using the following variables:
     *
     * toLeft: Vector pointing to the left relative to the tangent vector.
     * leftWallPos: Vector pointing to the left edge of the track, relative to the tangent vector.
     * rightWallPos: Vector pointing to the right edge of the track.
     * nextPoint: Vector point to the next point (at t+dt)
     * normalVector: normal vector.
     * Note: of most of these variables a "next" version is used as well, having the same meaning at the position t+dt instead
     * of at t.
     * differenceVector: The vector pointing from the begin point to the end point.
     * t: The loop variable running over the segment from 0 to 1.
     * dt: The step interval of the loop.
     *
     * The method draws triangles between the "current" positions, and the "next" positions.
     * The method returns an array of length 4 with the total length of each lane (which is just the length of the
     * distance vector is this case).
     */
    @Override
    double[] draw(GL2 gl) {
        dt = 1 / (double) resolution;
        gl.glColor3f(1f, 1f, 0f);

        //Calculate the difference vector, the normal and the toLeft vector, which are constant throughout the loop.
        differenceVector = endPoint.subtract(startPoint);
        toLeft = Vector.Z.cross(differenceVector).normalized();
        normalVector = differenceVector.cross(toLeft).normalized();

        //Precalculate the rightWallPosNext and leftWallPosNext coordinates of the first loop, since the loop passes these
        // to the "current" values in each iterations.
        Vector nextPoint = startPoint;
        Vector leftWallPosNext = nextPoint.add(toLeft.scale(trackWidth / 2.0));
        Vector rightWallPosNext = nextPoint.add(toLeft.scale(-trackWidth / 2.0));
        for (double t = 0; t < 1; t = t + dt) {
            //Pass on the "next" variables to the "current" variables, and calculate the new "next" values.
            nextPoint = nextPoint.add(differenceVector.scale(dt));
            Vector rightWallPosCurrent = leftWallPosNext;
            leftWallPosNext = nextPoint.add(toLeft.scale(trackWidth / 2.0));
            Vector leftWallPosCurrent = rightWallPosNext;
            rightWallPosNext = nextPoint.add(toLeft.scale(-trackWidth / 2.0));

            //Draw the right wall.
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z <= 1; z = z + 0.25) {
                gl.glNormal3d(toLeft.x(), toLeft.y(), toLeft.z());
                gl.glVertex3d(rightWallPosCurrent.x(), rightWallPosCurrent.y(), z);
                gl.glVertex3d(leftWallPosNext.x(), leftWallPosNext.y(), z);
            }
            gl.glEnd();

            //Draw the left wall.
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z < 1; z = z + 0.1) {
                gl.glNormal3d(-toLeft.x(), -toLeft.y(), -toLeft.z());
                gl.glVertex3d(leftWallPosCurrent.x(), leftWallPosCurrent.y(), z);
                gl.glVertex3d(rightWallPosNext.x(), rightWallPosNext.y(), z);
            }
            gl.glEnd();

            //Set normalVector and draw the top surface.
            gl.glNormal3d(normalVector.x(), normalVector.y(), normalVector.z());
            drawTrackSurface(rightWallPosCurrent, toLeft, leftWallPosNext, gl, (int) Math.round(t / dt));

            //Translate 2 meters downwards, and draw the bottom surface, setting inverted normal vector.
            gl.glPushMatrix();
            gl.glTranslated(0, 0, -2);
            gl.glNormal3d(-normalVector.x(), -normalVector.y(), -normalVector.z());
            drawTrackSurface(rightWallPosCurrent, toLeft, leftWallPosNext, gl, (int) Math.round(t / dt));
            gl.glPopMatrix();
        }

        //Return the length of each lane in an array, which is just the length of the differenceVector in this case.
        double[] distances = {differenceVector.length(), differenceVector.length(), differenceVector.length(), differenceVector.length()};
        return distances;
    }

    //Method to draw a surface from left to right.
    private void drawTrackSurface(Vector leftWallPosCurrent, Vector toLeft,
                                  Vector leftWallPosNext, GL2 gl, int segment) {
        double dw = 0.125d;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            Vector innerPoint0 = leftWallPosCurrent.add(toLeft.scale(-trackWidth * w));
            Vector innerPoint1 = leftWallPosNext.add(toLeft.scale(-trackWidth * w));
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());
        }
        gl.glEnd();
    }

    //Method returning the tangent vector (which is just the normalized differenceVector in this case.
    @Override
    Vector getTangent(double s, int laneNr) {
        return differenceVector.normalized();
    }

    //Method to return the normal vector.
    @Override
    Vector getNormal(double s, int LaneNr) {
        return normalVector;
    }

}