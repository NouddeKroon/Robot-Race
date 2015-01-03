import com.jogamp.opengl.util.texture.Texture;
import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

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
        double outerWallNext = 0;
        double innerWallNext = 0;
        double[] laneDistances = new double [4];


        for (double t = 0; t < 1; t = t + dt) {
            //Pass on the "next" variables to the "current" variables, as well as calculate the new "next" variables.
            Vector toLeft = toLeftNext;
            toLeftNext = Vector.Z.cross(getTangent((t + dt))).normalized();
            nextPoint = getPoint(t + dt);
            Vector innerRingPosCurrent = innerRingPosNext;
            innerRingPosNext = nextPoint.add(toLeftNext.scale(trackWidth / 2.0));
            Vector outerRingPosCurrent = outerRingPosNext;
            outerRingPosNext = nextPoint.add(toLeftNext.scale(-trackWidth / 2.0));
            gl.glColor3d(1, 1, 1);
            double dw = 0.25;


            //Draw the inner wall:
            brick.enable(gl);
            brick.bind(gl);
            double innerWall = innerWallNext;
            float heightAtInnerWall = Terrain.heightAt((float)innerRingPosCurrent.x(),(float)innerRingPosCurrent.y());
            float heightAtInnerWallNext = Terrain.heightAt((float)innerRingPosNext.x(),(float)innerRingPosNext.y());
            innerWallNext += (innerRingPosNext.subtract(innerRingPosCurrent)).length() / 8.0;
            if (innerWallNext > 1.0) {
                innerWall = 0.0;
                innerWallNext = (innerRingPosNext.subtract(innerRingPosCurrent)).length() / 8.0;
            }
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = 1; z >= -1; z = z - 0.25) {
                gl.glNormal3d(toLeft.x(), toLeft.y(), toLeft.z());
                gl.glTexCoord2d((z + 1.0) / 2.0, innerWall);
                gl.glVertex3d(innerRingPosCurrent.x(), innerRingPosCurrent.y(), z);

                gl.glNormal3d(toLeftNext.x(), toLeftNext.y(), toLeftNext.z());
                gl.glTexCoord2d((z + 1.0) / 2.0, innerWallNext);
                gl.glVertex3d(innerRingPosNext.x(), innerRingPosNext.y(), z);

                if (z < heightAtInnerWall && z < heightAtInnerWallNext){
                    break;
                }
            }
            gl.glEnd();


            //Draw the outer wall:
            float heightAtOuterWall = Terrain.heightAt((float)outerRingPosCurrent.x(),(float)outerRingPosCurrent.y());
            float heightAtOuterWallNext = Terrain.heightAt((float)outerRingPosNext.x(),(float)outerRingPosNext.y());
            double outerWall = outerWallNext;
            outerWallNext = (outerWallNext + ((outerRingPosNext.subtract(outerRingPosCurrent)).length() / 8.0));
            if (outerWallNext > 1.0) {
                outerWall = 0.0;
                outerWallNext = ((outerRingPosNext.subtract(outerRingPosCurrent)).length() / 8.0);
            }
            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = 1; z >= -1; z = z - 0.25) {

                gl.glNormal3d(-toLeft.x(), -toLeft.y(), -toLeft.z());
                gl.glTexCoord2d((z + 1.0) / 2.0, outerWall);
                gl.glVertex3d(outerRingPosCurrent.x(), outerRingPosCurrent.y(), z);
                gl.glTexCoord2d((z + 1.0) / 2.0, outerWallNext);
                gl.glNormal3d(-toLeftNext.x(), -toLeftNext.y(), -toLeftNext.z());
                gl.glVertex3d(outerRingPosNext.x(), outerRingPosNext.y(), z);
                if (z < heightAtOuterWall && z < heightAtOuterWallNext) {
                    break;
                }
            }
            gl.glEnd();
            brick.disable(gl);


            track.enable(gl);
            track.bind(gl);
            gl.glNormal3d(0, 0, 1);

            for (int i = 0; i < 4; i++) {

                Vector firstPoint = innerRingPosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0));
                Vector secondPoint = innerRingPosNext.add(toLeftNext.scale(-trackWidth * (double)i / 4.0));
                Vector thirdPoint = innerRingPosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0 - 0.5));
                Vector fourthPoint = innerRingPosNext.add(toLeftNext.scale(-trackWidth * (double)i / 4.0 - 0.5));
                Vector fifthPoint = innerRingPosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0  - 1.0));
                Vector sixthPoint = innerRingPosNext.add(toLeftNext.scale(-trackWidth * (double)i / 4.0  - 1.0));

                double length = (fourthPoint.subtract(thirdPoint)).length() / 12.0;
                if (laneDistances[i]+length > 0.8333333) {
                    laneDistances[i] = 0;
                }

                gl.glBegin(gl.GL_TRIANGLE_STRIP);
                gl.glTexCoord2d((double)i / 4.0,laneDistances[i]);
                gl.glVertex3d(firstPoint.x(), firstPoint.y(), firstPoint.z());
                gl.glTexCoord2d((double)i / 4.0,laneDistances[i]+length);
                gl.glVertex3d(secondPoint.x(), secondPoint.y(), secondPoint.z());


                gl.glTexCoord2d((double)i / 4.0 + 0.125,laneDistances[i]);
                gl.glVertex3d(thirdPoint.x(), thirdPoint.y(), thirdPoint.z());
                gl.glTexCoord2d((double)i / 4.0 + 0.125,laneDistances[i]+length);
                gl.glVertex3d(fourthPoint.x(), fourthPoint.y(), fourthPoint.z());


                gl.glTexCoord2d((double)i / 4.0+0.250,laneDistances[i]);
                gl.glVertex3d(fifthPoint.x(), fifthPoint.y(), fifthPoint.z());
                gl.glTexCoord2d((double)i / 4.0+0.25,laneDistances[i]+length);
                gl.glVertex3d(sixthPoint.x(), sixthPoint.y(), sixthPoint.z());
                laneDistances[i]+=length;
                gl.glEnd();
            }

            track.disable(gl);
        }

    }
}
