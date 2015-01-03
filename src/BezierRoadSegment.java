import robotrace.Vector;

import javax.media.opengl.GL2;
import java.util.Arrays;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Created by Noud on 12/20/2014.
 */
public class BezierRoadSegment extends RoadSegment {

    Vector point0;
    Vector point1;
    Vector point2;
    Vector point3;
    int resolution;
    //Table containing the distance of each segment, first index is the lane number, second index is the distance at a
    // t = index number * dt.
    double[][] segmentDistances;
    double trackWidth = 4;
    double dt;
    int[] previousDistanceIndex = new int[4];


    BezierRoadSegment(Vector point0, Vector point1, Vector point2, Vector point3, int resolution) {
        this.point0 = point0;
        this.point1 = point1;
        this.point2 = point2;
        this.point3 = point3;
        this.resolution = resolution;
        segmentDistances = new double[4][resolution + 1];
    }


    /**
     * Method returning a coordinate on a lane, when given a distance traveled on the segment and a lane. Uses the table
     * of stored distances created during drawing to calculate the position.
     */
    @Override
    public Vector getCoordinate(double s, int laneNr) {
        Vector coordinate;
        Vector tangent;
        Vector toLeft;
        double t;

        if (previousDistanceIndex[laneNr] > 0) {
            if (s < segmentDistances[laneNr][previousDistanceIndex[laneNr]-1]){
                previousDistanceIndex[laneNr] = 0;
            }
        }

        while (s >= segmentDistances[laneNr][previousDistanceIndex[laneNr]]) {
            previousDistanceIndex[laneNr]++;
        }


        double difference = segmentDistances[laneNr][previousDistanceIndex[laneNr]] - segmentDistances[laneNr][previousDistanceIndex[laneNr] - 1];
        t = (previousDistanceIndex[laneNr] - 1 + (s - segmentDistances[laneNr][previousDistanceIndex[laneNr]-1]) / difference) * dt;

//        /**
//         * Binary search the segmentDistances for distance we need. It will (almost never) find an exact match, but
//         * will return a negative result = insertionPoint - 1, where insertion point is the first index that is bigger
//         * than the given value.
//        **/
//        int binarySearchResult = Arrays.binarySearch(segmentDistances[laneNr], s);
//        //binarySearch did not find an exact match. Calculate the difference between the distance values in the table
//        //between insertion point - 1 and insertion point. Calculate the overflow. Interpolate a t value between
//        //insertion point - 1 and insertion point:.
//        if (binarySearchResult < 0) {
//            int insertionPoint = -1 - binarySearchResult;
//            double difference = segmentDistances[laneNr][insertionPoint] - segmentDistances[laneNr][insertionPoint - 1];
//            t = (insertionPoint - 1 + (s - segmentDistances[laneNr][insertionPoint - 1]) / difference) * dt;
//        } else {
//            //The binarySearch delivered an exact result.
//            t = binarySearchResult*dt;
//        }
        //Calculate the coordinate at t, the tangent, and the vector to the left in order to give the proper coordinate
        // on the given lane (note that lane 0 is the leftmost lane).
        coordinate = Util.getCubicBezierPnt(t, point0, point1, point2, point3);
        tangent = Util.getCubicBezierTng(t, point0, point1, point2, point3);
        toLeft = Vector.Z.cross(tangent).normalized();
        coordinate = coordinate.add(toLeft.scale(1.5 - (double) laneNr));

        return coordinate;
    }


    /**
     * Method that draws the test track, using the following variables:
     *
     * toLeft: Vector pointing to the left relative to the tangent vector.
     * leftEdgePos: Vector pointing to the left edge of the track, relative to the tangent vector.
     * outRingPos: Vector pointing to the right edge of the track.
     * nextPoint: Vector point to the next point (at t+dt)
     * normal: normal vector.
     * Note: of most of these variables a "next" version is used as well, having the same meaning at the position t+dt instead
     * of at t.
     * t: The loop variable running over the segment from 0 to 1.
     * dt: The step interval of the loop.
     *
     * The method draws triangles between the "current" positions, and the "next" positions.
     * The method returns an array of length 4 with the total length of each lane.
     */
    @Override
    TextureData draw(GL2 gl, TextureData data) {
        dt = 1 / (double) resolution;    //Step size is determined by resolution.
        gl.glColor3f(1f, 1f, 1f);

        //In each iteration of the loop the values of the next loop are passed to the values of the current loop. We
        // pre calculate these for the first loop through:
        Vector tangentNext = Util.getCubicBezierTng(0, point0, point1, point2, point3);
        Vector toLeftNext = Vector.Z.cross(tangentNext).normalized();
        Vector nextPoint = point0;
        Vector leftEdgePosNext = nextPoint.add(toLeftNext.scale(trackWidth / 2.0));
        Vector rightEdgePosNext = nextPoint.add(toLeftNext.scale(-trackWidth / 2.0));
        Vector normalNext = tangentNext.cross(toLeftNext).normalized();
        for (double t = 0; t < 1; t = t + dt) {
            //Pass on values from the "next" variables to the "current" variables. Calculate new "next" variables.
            tangentNext = Util.getCubicBezierTng(t+dt, point0, point1, point2, point3);
            Vector toLeft = toLeftNext;
            toLeftNext = Vector.Z.cross(tangentNext).normalized();
            nextPoint = Util.getCubicBezierPnt(t + dt, point0, point1, point2, point3);
            Vector leftEdgePosCurrent = leftEdgePosNext;
            leftEdgePosNext = nextPoint.add(toLeftNext.scale(trackWidth / 2.0));
            Vector rightEdgeCurrent = rightEdgePosNext;
            rightEdgePosNext = nextPoint.add(toLeftNext.scale(-trackWidth / 2.0));
            Vector normal = normalNext;
            normalNext = tangentNext.cross(toLeftNext).normalized();

            Track.brick.enable(gl);
            Track.brick.bind(gl);
            //Draw the left edge of the road, setting the normal vectors in the direction of the toLeft vector.
            gl.glBegin(GL_TRIANGLE_STRIP);
            float heightAtLeftEdgeCurrent = Terrain.heightAt((float)leftEdgePosCurrent.x(),(float)leftEdgePosCurrent.y());
            float heightAtLeftEdgeNext = Terrain.heightAt((float)leftEdgePosNext.x(),(float)leftEdgePosNext.y());
            data.leftWallTexCoorLast = data.leftWallTexCoorNext;
            data.leftWallTexCoorNext += leftEdgePosNext.subtract(leftEdgePosCurrent).length() / 8.0;
            if (data.leftWallTexCoorNext > 1.0){
                data.leftWallTexCoorLast = 0;
                data.leftWallTexCoorNext = leftEdgePosNext.subtract(leftEdgePosCurrent).length() / 8.0;
            }
            for (double z = 1; z >= -1; z = z - 0.25) {
                gl.glNormal3d(toLeft.x(), toLeft.y(), toLeft.z());
                gl.glTexCoord2d((z+1)/2.0,data.leftWallTexCoorLast);
                gl.glVertex3d(leftEdgePosCurrent.x(), leftEdgePosCurrent.y(), leftEdgePosCurrent.z() - 1 + z);

                gl.glNormal3d(toLeftNext.x(), toLeftNext.y(), toLeftNext.z());
                gl.glTexCoord2d((z+1)/2.0,data.leftWallTexCoorNext);
                gl.glVertex3d(leftEdgePosNext.x(), leftEdgePosNext.y(), leftEdgePosNext.z() - 1 + z);
                if (leftEdgePosCurrent.z() - 1 + z < heightAtLeftEdgeCurrent && leftEdgePosNext.z() - 1 + z < heightAtLeftEdgeNext) {
                    break;
                }
            }
            gl.glEnd();

            //Draw the right edge of the road, setting the normal vectors in the direction of the toRight vector.
            gl.glBegin(GL_TRIANGLE_STRIP);
            data.rightWallTexCoorLast = data.rightWallTexCoorNext;
            data.rightWallTexCoorNext += rightEdgePosNext.subtract(rightEdgeCurrent).length() / 8.0;
            float heightAtRightEdgeCurrent = Terrain.heightAt((float)rightEdgeCurrent.x(), (float)rightEdgeCurrent.y());
            float heightAtRightEdgeNext = Terrain.heightAt((float)rightEdgePosNext.x(), (float)rightEdgePosNext.y());
            if (data.rightWallTexCoorNext > 1.0) {
                data.rightWallTexCoorLast = 0;
                data.rightWallTexCoorNext = rightEdgePosNext.subtract(rightEdgeCurrent).length() / 8.0;
            }
            for (double z = 1; z >= -1; z = z - 0.25) {
                gl.glNormal3d(-toLeft.x(), -toLeft.y(), -toLeft.z());
                gl.glTexCoord2d((z+1)/2.0,data.rightWallTexCoorLast);
                gl.glVertex3d(rightEdgeCurrent.x(), rightEdgeCurrent.y(), rightEdgeCurrent.z() - 1 + z);

                gl.glNormal3d(-toLeftNext.x(), -toLeftNext.y(), -toLeftNext.z());
                gl.glTexCoord2d((z+1)/2.0,data.rightWallTexCoorNext);
                gl.glVertex3d(rightEdgePosNext.x(), rightEdgePosNext.y(), rightEdgePosNext.z() - 1 + z);

                if (rightEdgeCurrent.z() - 1 + z < heightAtRightEdgeCurrent && rightEdgePosNext.z() - 1 + z < heightAtRightEdgeNext){
                    break;
                }
            }
            gl.glEnd();
            Track.brick.disable(gl);


            Track.track.enable(gl);
            Track.track.bind(gl);

            boolean drawBottom = false;
            if (leftEdgePosCurrent.z() > 1.001 || leftEdgePosNext.z() > 1.001){
                drawBottom = true;
            }
            for (int i = 0; i < 4; i++) {
                Vector firstPoint = leftEdgePosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0));
                Vector secondPoint = leftEdgePosNext.add(toLeftNext.scale(-trackWidth* (double)i / 4.0));
                Vector thirdPoint = leftEdgePosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0 - 0.5));
                Vector fourthPoint = leftEdgePosNext.add(toLeftNext.scale(-trackWidth* (double)i / 4.0 - 0.5));
                Vector fifthPoint = leftEdgePosCurrent.add(toLeft.scale(-trackWidth * (double)i / 4.0 - 1.0));
                Vector sixthPoint = leftEdgePosNext.add(toLeftNext.scale(-trackWidth* (double)i / 4.0 - 1.0));

                double length = (fourthPoint.subtract(thirdPoint)).length() / 12.0;

                segmentDistances[i][(int)Math.round(t/dt) + 1] = segmentDistances[i][(int)Math.round(t/dt)] + length * 12.0;

                if (data.roadTexCoors[i][1] + length > 0.83333) {
                    data.roadTexCoors[i][0] = 0;
                    data.roadTexCoors[i][1] = length;
                } else {
                    data.roadTexCoors[i][0] = data.roadTexCoors[i][1];
                    data.roadTexCoors[i][1] += length;
                }

                gl.glBegin(gl.GL_TRIANGLE_STRIP);
                gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][0]);
                gl.glNormal3d(normal.x(),normal.y(),normal.z());
                gl.glVertex3d(firstPoint.x(), firstPoint.y(), firstPoint.z());
                gl.glTexCoord2d((double)i / 4.0,data.roadTexCoors[i][1]);
                gl.glNormal3d(normalNext.x(),normalNext.y(),normalNext.z());
                gl.glVertex3d(secondPoint.x(), secondPoint.y(), secondPoint.z());


                gl.glTexCoord2d((double)i / 4.0 + 0.125,data.roadTexCoors[i][0]);
                gl.glNormal3d(normal.x(), normal.y(), normal.z());
                gl.glVertex3d(thirdPoint.x(), thirdPoint.y(), thirdPoint.z());
                gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][1]);
                gl.glNormal3d(normalNext.x(), normalNext.y(), normalNext.z());
                gl.glVertex3d(fourthPoint.x(), fourthPoint.y(), fourthPoint.z());


                gl.glTexCoord2d((double)i / 4.0+0.250,data.roadTexCoors[i][0]);
                gl.glNormal3d(normal.x(), normal.y(), normal.z());
                gl.glVertex3d(fifthPoint.x(), fifthPoint.y(), fifthPoint.z());
                gl.glTexCoord2d((double) i / 4.0 + 0.25, data.roadTexCoors[i][1]);
                gl.glNormal3d(normalNext.x(), normalNext.y(), normalNext.z());
                gl.glVertex3d(sixthPoint.x(), sixthPoint.y(), sixthPoint.z());
                gl.glEnd();

                if (drawBottom) {
                    gl.glPushMatrix();
                    gl.glTranslated(0, 0, -2);
                    gl.glBegin(gl.GL_TRIANGLE_STRIP);
                    gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][0]);
                    gl.glNormal3d(-normal.x(), -normal.y(), -normal.z());
                    gl.glVertex3d(firstPoint.x(), firstPoint.y(), firstPoint.z());
                    gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][1]);
                    gl.glNormal3d(-normalNext.x(), -normalNext.y(), -normalNext.z());
                    gl.glVertex3d(secondPoint.x(), secondPoint.y(), secondPoint.z());


                    gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][0]);
                    gl.glNormal3d(-normal.x(), -normal.y(), -normal.z());
                    gl.glVertex3d(thirdPoint.x(), thirdPoint.y(), thirdPoint.z());
                    gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][1]);
                    gl.glNormal3d(-normalNext.x(), -normalNext.y(), -normalNext.z());
                    gl.glVertex3d(fourthPoint.x(), fourthPoint.y(), fourthPoint.z());


                    gl.glTexCoord2d((double) i / 4.0 + 0.250, data.roadTexCoors[i][0]);
                    gl.glNormal3d(-normal.x(), -normal.y(), -normal.z());
                    gl.glVertex3d(fifthPoint.x(), fifthPoint.y(), fifthPoint.z());
                    gl.glTexCoord2d((double) i / 4.0 + 0.25, data.roadTexCoors[i][1]);
                    gl.glNormal3d(-normalNext.x(), -normalNext.y(), -normalNext.z());
                    gl.glVertex3d(sixthPoint.x(), sixthPoint.y(), sixthPoint.z());
                    gl.glEnd();

                    gl.glPopMatrix();
                }
            }
        }

        //When drawing the top surface, we recorded the distance values of each lane. We return the final total length
        //of each lane to the calling object.
        double[] distances = {segmentDistances[0][resolution], segmentDistances[1][resolution],
                segmentDistances[2][resolution], segmentDistances[3][resolution]};
        data.distanceData = distances;
        return data;
    }


    /**
     * Method that returns the tangent vector at a given s and laneNr. Uses the exact same method to find the coordinate
     * as the getCoordinate method (using binarySearch).
     */
    @Override
    Vector getTangent(double s, int laneNr) {
        s = s % segmentDistances[laneNr][resolution];
        int binarySearchResult = Arrays.binarySearch(segmentDistances[laneNr], s);
        if (binarySearchResult < 0) {
            int insertionPoint = -1 - binarySearchResult;
            double difference = segmentDistances[laneNr][insertionPoint] - segmentDistances[laneNr][(insertionPoint - 1)];
            double t = (insertionPoint - 1 + (s - segmentDistances[laneNr][insertionPoint - 1]) / difference) * dt;
            return Util.getCubicBezierTng(t, point0, point1, point2, point3).normalized();
        } else return Util.getCubicBezierTng(dt * binarySearchResult, point0, point1, point2, point3).normalized();
    }

    /**
     * Method that draws the top surface of the track, also if recordDistances is true, it will store the distance
     * values in the segmentDistances array.
     */
    private void drawTrackSurface(Vector leftEdgePosCurrent, Vector toLeft, Vector leftEdgeNext, Vector vectorToLeftNext,
                                    Vector normal, Vector normalNext, GL2 gl, int segment, boolean recordDistances) {
        double dw = 0.125d;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            //Substract the toLeft vector from the left edges to obtain coordinates to draw triangles between on the track.
            Vector innerPoint0 = leftEdgePosCurrent.add(toLeft.scale(-trackWidth * w));
            Vector innerPoint1 = leftEdgeNext.add(vectorToLeftNext.scale(-trackWidth * w));
            gl.glNormal3d(normal.x(),normal.y(),normal.z());
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glNormal3d(normalNext.x(),normalNext.y(),normalNext.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());

            //Use the difference vector between the points to calculate the distances, record them in the array.
            if ((w / dw) % 2 == 1 && recordDistances) {
                int laneNr = (int) Math.round((w - 0.125) / 0.25);
                Vector distanceVector = innerPoint1.subtract(innerPoint0);
                segmentDistances[laneNr][(segment + 1)] = segmentDistances[laneNr][segment] + distanceVector.length();
            }
        }
        gl.glEnd();
    }

    /**
     * Method that returns the normal vector at a given s and laneNr. First checks if all the points are on the same
     * plane, if not, it uses the exact same method to find the coordinate as the getCoordinate method (using binarySearch).
     * Finally use cross product to find the normal vector. We assume the track is not completely vertical.
     */
    @Override
    public Vector getNormal(double s, int laneNr){
        if (point0.z() == point1.z() && point0.z() == point2.z() && point0.z() == point3.z()) {
            return Vector.Z;
        } else {
            double t;
            Vector tangent = null;
            int binarySearchResult = Arrays.binarySearch(segmentDistances[laneNr], s);
            if (binarySearchResult < 0) {
                int insertionPoint = -1 - binarySearchResult;
                double difference = segmentDistances[laneNr][insertionPoint] - segmentDistances[laneNr][insertionPoint - 1];
                t = (insertionPoint - 1 + (s - segmentDistances[laneNr][insertionPoint - 1]) / difference) * dt;
            } else {
                t = dt * binarySearchResult;
            }
            tangent = Util.getCubicBezierTng(t, point0, point1, point2, point3);
            return tangent.cross(Vector.Z.cross(tangent)).normalized();
        }
    }
}

