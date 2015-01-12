import com.jogamp.opengl.util.texture.Texture;
import robotrace.GlobalState;
import robotrace.Vector;

import javax.media.opengl.GL2;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Implementation of a race track that is made from Bezier segments and straight road segments, or the test track.
 */
public class RaceTrack {
    /**
     * Local pointer to the global game-state
     */
    private GlobalState gs;

    /**
     * Variable point to the display-list reserved for this object.
     */
    private int displayList;

    /**
     * Array containing the 5 different track objects.
     */
    private Track[] trackList = {
            new TestTrack(), new GenericTrack(TrackCoordinates.OTRACK), new GenericTrack(TrackCoordinates.LTRACK),
            new GenericTrack(TrackCoordinates.CTRACK), new GenericTrack(TrackCoordinates.CUSTOMTRACK)
    };

    /**
     * Keep track of which track this instance is to draw.
     */
    int trackNr;


    /**
     * Constructs the race track, sets up display lists.
     */
    public RaceTrack(GlobalState gs) {
        this.gs = gs;
    }

    /**
     * Method that draws the currently selected track.
     * @param gl OpenGL context
     */
    public void draw(GL2 gl) {
        if (displayList == 0) {     //If display-list hasn't been reserved yet, reserve a new display-list.
            displayList = gl.glGenLists(1);
            this.trackNr = -1;      //Set trackNr to -1 to make sure the display-list gets generated for the first time.
        }
        /**
         * Check if the selected track has changed. If so, update the local trackNr and generate a new display-list.
         * Otherwise just draw the already generated displaylist.
         */
        if ( this.trackNr != gs.trackNr) {
            this.trackNr = gs.trackNr;
            gl.glNewList(displayList, GL2.GL_COMPILE_AND_EXECUTE);
            trackList[trackNr].draw(gl);
            gl.glEndList();
        } else {
            gl.glCallList(displayList);
        }
    }

    /**
     * Method that returns a position vector on the track, corresponding to a certain lane and distance travelled.
     * @param s Distance travelled.
     * @param laneNr Lanenumber the distance was travelled on.
     * @return Vector pointing to the position corresponding to the parameters.
     */
    Vector getPositionOnLane(double s, int laneNr) {
        //The request gets deferred to the currently selected track object.
        return trackList[trackNr].getPositionOnLane(s, laneNr);
    }

    /**
     * Method that returns the tangent vector corresponding to a certain lane and distance travelled.
     * @param s Distance travelled.
     * @param laneNr Lanenumber the distance was travelled on.
     * @return Tangent vector.
     */
    public Vector getTangent(double s, int laneNr) {
        //The request gets deferred to the currently selected track object.
        return trackList[trackNr].getTangent(s, laneNr);
    }

    /**
     * Method that returns the normal vector corresponding to a certain lane and distance travelled.
     * @param s Distance travelled.
     * @param laneNr Lanenumber the distance was travelled on.
     * @return Normal vector.
     */
    public Vector getNormal(double s, int laneNr) {
        //The request gets deferred to the currently selected track object.
        return trackList[trackNr].getNormal(s, laneNr);
    }
}

/**
 * Abstract class defining the common features between the GenericTrack and the TestTrack classes.
 */
abstract class Track {
    /**
     * Texture object used for the brick on the sides of the track.
     */
    static Texture brick;

    /**
     * Texture object used for the top and bottom om the track.
     */
    static Texture track;

    abstract void draw(GL2 gl);

    abstract Vector getPositionOnLane(double s, int laneNr);

    abstract Vector getTangent(double s, int laneNr);

    abstract Vector getNormal(double s, int laneNr);

}


/**
 * Class holding an array with all the segments of a track, and a table which holds the distance of each segment. It
 * then passes the getPositionOnLane, getTangent and getNormal calls to the appropriate road segment.
 */
class GenericTrack extends Track {
    RoadSegment[] roadSegments;                 //The array of all the individual roadSegments of which the track consists.
    double[][] distancesTablePerLane;           //Table holding the length of each roadSegment, on a per lane basis.

    GenericTrack(TrackCoordinates track) {
        this.roadSegments = track.roadSegments;
    }

    /**
     * Method that calls each roadSegment to draw itself. These segments return an array containing the length of the segment
     * on a per lane basis. Record these in the distancesTablePerLane array.
     * @param gl OpenGL context
     */
    @Override
    void draw(GL2 gl) {
        TrackDrawingData data = new TrackDrawingData();
        distancesTablePerLane = new double[roadSegments.length + 1][4];
        for (int i = 0; i < roadSegments.length; i++) {
            roadSegments[i].draw(gl, data);
            for (int j = 0; j < 4; j++) {
                distancesTablePerLane[i + 1][j] = data.distanceData[j]  + distancesTablePerLane[i][j];
            }
        }
    }

    /**
     * Method for returning position, given a distance and lane number. Use the distancesTablePerLane array to decide
     * which RoadSegment to pass the call to, giving that RoadSegment a relative distance on that segment.
     * @param s Distance travelled on the track.
     * @param laneNr Lane the robot is on.
     * @return Vector pointing to the position of the robot on the track.
     */
    @Override
    Vector getPositionOnLane(double s, int laneNr) {
        s = s % distancesTablePerLane[distancesTablePerLane.length - 1][laneNr];
        //Since there are only a few roadsegments per track we use a naive loop to find the needed segment.
        for (int i = 0; i < distancesTablePerLane.length; i++) {
            if (s < distancesTablePerLane[i][laneNr]) {
                s -= distancesTablePerLane[i - 1][laneNr];
                return roadSegments[i - 1].getCoordinate(s, laneNr);
            }
        }
        return null;
    }

    /**
     * Method for returning a tangent vector, given a distance and lane number. Use the distancesTablePerLane array to
     * decide which RoadSegment to pass the call to, giving that RoadSegment a relative distance on that segment.
     * @param s Distance travelled on the track.
     * @param laneNr Lane the robot is on.
     * @return Tangent vector corresponding to the given parameters.
     */
    @Override
    Vector getTangent(double s, int laneNr) {
        s = s % distancesTablePerLane[distancesTablePerLane.length - 1][laneNr];
        for (int i = 0; i < distancesTablePerLane.length; i++) {
            if (s < distancesTablePerLane[i][laneNr]) {
                s -= distancesTablePerLane[i - 1][laneNr];
                return roadSegments[i - 1].getTangent(s, laneNr);
            }
        }
        return null;
    }

    /**
     * Method for returning a normal vector, given a distance and lane number. Use the distancesTablePerLane array to
     * decide which RoadSegment to pass the call to, giving that RoadSegment a relative distance on that segment.
     * @param s Distance travelled on the track.
     * @param laneNr Lane the robot is on.
     * @return Normal vector corresponding to the parameters.
     */
    @Override
    Vector getNormal(double s, int laneNr) {
        s = s % distancesTablePerLane[distancesTablePerLane.length - 1][laneNr];
        for (int i = 0; i < distancesTablePerLane.length; i++) {
            if (s < distancesTablePerLane[i][laneNr]) {
                s -= distancesTablePerLane[i - 1][laneNr];
                return roadSegments[i - 1].getNormal(s, laneNr);
            }
        }
        return null;
    }
}

//Track class for drawing the test track from the assignment.
class TestTrack extends Track {
    // Dimension of the basic oval track specified in the assignment.
    final double ovalTrackCosRadius = 10;
    final double ovalTrackSinRadius = 14;
    double trackWidth = 4;
    double dt = 0.01;   //step size.

    /**
     * @param t Parameter corresponding running from 0 to 1.
     * @return Point on the centre line of the track.
     */
    public Vector getPoint(double t) {
        double x = ovalTrackCosRadius * Math.cos(Math.PI * 2 * t);
        double y = ovalTrackSinRadius * Math.sin(Math.PI * 2 * t);
        return new Vector(x, y, 1);
    }

    /**
     * Method returning a position coordinate on the oval, given a distance traveled and lane number. Note that lane
     * 0 is the rightmost lane on the track. Calculates a vector "toLeft" from the cross between the tangent and the normal
     * in order to create the appropriate offset for each lane.
     * @param s Distance travelled on the track.
     * @param laneNr the robot is on.
     * @return Vector pointing to the position of the robot.
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

        position = position.add(toLeft.scale(-3.0/8.0*trackWidth + laneNr));
        return position;
    }

    /**
     * A tangent to a 2D outline of oval shape. The below functions are obtained by taking dx/dt and dy/dt of the
     * function in getPoint. First converts a given distance s to a corresponding t used in the formula by approximation,
     * using the circumfence of the of each track.
     * @param s Distance travelled on the track.
     * @param laneNr Lane the robot is on.
     * @return Tangent vector corresponding to the parameters.
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

    /**
     * Overloads the getTangent vector for class-local calculations, taking t as an argument instead of a distance s.
     * @param t Parameter of the used formula of the track, from 0 to 1.
     * @return Tangent vector corresponding to the parameter.
     */
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
     * Method that draws the test track.
     */
    public void draw(GL2 gl) {
        TrackDrawingData data = new TrackDrawingData();
        gl.glColor3f(1f, 1f, 1f);

        /**
         * We loop over the entire segment, generating 2 points every iteration, and then use the TrackCrossSectionDrawer
         * to draw a cross-section connecting these 2 points. In each loop the previous secondPoint and secondPointTangent
         * get passed on to the firstPoint and firstPointTangent variables respectively, therefore before we begin we
         * pre-calculate these for the first iteration.
         */
        Vector secondPoint = getPoint(0);
        Vector secondPointTangent = getTangent(0);

        for (double t = 0; t < 1; t = t + dt) {
            //Pass on values from the "next" variables to the "current" variables. Calculate new "next" variables.
            Vector firstPointTangent = secondPointTangent;
            secondPointTangent = getTangent(t + dt);
            Vector firstPoint = secondPoint;
            secondPoint = getPoint(t + dt);

            //Draw the cross-section, storing the distance travelled on this cross-section in an array.
            TrackCrossSectionDrawer.drawCrossSection(gl, data, firstPoint, firstPointTangent, secondPoint,
                    secondPointTangent);
        }

    }
}


/**
 * Class holding the relevant data during drawing of a track.
 */
class TrackDrawingData {
    double leftEdgeTexCoorFirstPoint;
    double leftEdgeTexCoorSecondPoint;
    double rightEdgeTexCoorFirstPoint;
    double rightEdgeTexCoorSecondPoint;
    double[][] roadTexCoors = new double[4][2];
    double[] distanceData;
}

/**
 * Class that draws a cross section of the track, with a depth of 1 polygon spanning between firstPoint and secondPoint.
 */
class TrackCrossSectionDrawer{
    static double trackWidth = 4.0;
    static int numberOfLanes = 4;

    /**
     * Draws a cross section of the track, going from a firstPointPos to a secondPointPos.
     * @param gl OpenGL context
     * @param data TrackDrawingData class holding the texture data used in this cross section
     * @param firstPointPos The position of the first point.
     * @param firstPointTangent The tangent to the track at the first point.
     * @param secondPointPos The position of the second point.
     * @param secondPointTangent The tangent of the track at the second point.
     * @return An array holding the distance travelled on this cross section from point 1 to point 2, for each lane.
     */
    static double[] drawCrossSection(GL2 gl, TrackDrawingData data, Vector firstPointPos, Vector firstPointTangent,
                          Vector secondPointPos, Vector secondPointTangent) {
        /**
         * First calculate the vectors pointing to the left (relative to the tangent) for the first and second points,
         * as well as the normals corresponding to those points:
         **/
        Vector firstPointToLeftVector = Vector.Z.cross(firstPointTangent).normalized();
        Vector secondPointToLeftVector = Vector.Z.cross(secondPointTangent).normalized();
        Vector firstPointNormal = firstPointTangent.cross(firstPointToLeftVector).normalized();
        Vector secondPointNormal = secondPointTangent.cross(secondPointToLeftVector).normalized();

        /**
         * Calculate the vectors pointing to the left and right edges of the road, at the first and second point.
         */
        Vector firstPointLeftEdge = firstPointPos.add(firstPointToLeftVector.scale(trackWidth / 2.0));
        Vector firstPointRightEdge = firstPointPos.add(firstPointToLeftVector.scale(-trackWidth / 2.0));
        Vector secondPointLeftEdge = secondPointPos.add(secondPointToLeftVector.scale(trackWidth / 2.0));
        Vector secondPointRightEdge = secondPointPos.add(secondPointToLeftVector.scale(-trackWidth / 2.0));

        //First set the texture for drawing the walls.
        Track.brick.enable(gl);
        Track.brick.bind(gl);


        /**
         * Draw the left edge of the road.
         */
        //Calculate the height of terrain at the left edge for the first and second point:
        float firstPointHeightAtLeftEdge = Terrain.heightAt((float) firstPointLeftEdge.x(), (float) firstPointLeftEdge.y());
        float secondPointHeightAtLeftEdge = Terrain.heightAt((float) secondPointLeftEdge.x(), (float) secondPointLeftEdge.y());
        /**
         * Calculate the new texture coordinates for the left edge of the road. If there is an overflow so we reach a
         * coordinate higher than 1.0, we reset the first coordinate back to 0 and calculate the second coordinate from there.
         */
        data.leftEdgeTexCoorFirstPoint = data.leftEdgeTexCoorSecondPoint;
        data.leftEdgeTexCoorSecondPoint += secondPointLeftEdge.subtract(firstPointLeftEdge).length() / 8.0;
        if (data.leftEdgeTexCoorSecondPoint > 1.0) {
            data.leftEdgeTexCoorFirstPoint = 0;
            data.leftEdgeTexCoorSecondPoint = secondPointLeftEdge.subtract(firstPointLeftEdge).length() / 8.0;
        }
        /**
         * Finally draw the left edge of the track. Set the normal vector in the same direction as the vector pointing to
         * the left.
         */
        gl.glBegin(GL_TRIANGLE_STRIP);
        for (double z = 1; z >= -1; z -= 0.25) {
            gl.glNormal3d(firstPointToLeftVector.x(), firstPointToLeftVector.y(), firstPointToLeftVector.z());
            gl.glTexCoord2d((z + 1) / 2.0, data.leftEdgeTexCoorFirstPoint);
            gl.glVertex3d(firstPointLeftEdge.x(), firstPointLeftEdge.y(), firstPointLeftEdge.z() - 1 + z);

            gl.glNormal3d(secondPointToLeftVector.x(), secondPointToLeftVector.y(), secondPointToLeftVector.z());
            gl.glTexCoord2d((z + 1) / 2.0, data.leftEdgeTexCoorSecondPoint);
            gl.glVertex3d(secondPointLeftEdge.x(), secondPointLeftEdge.y(), secondPointLeftEdge.z() - 1 + z);

            //If at any point both the vertex coordinates are below the terrain we can stop drawing.
            if (firstPointLeftEdge.z() - 1 + z < firstPointHeightAtLeftEdge &&
                    secondPointLeftEdge.z() - 1 + z < secondPointHeightAtLeftEdge) {
                break;
            }
        }
        gl.glEnd();

        /**
         * Draw right edge of the road.
         */
        //Calculate the height of the terrain af the right edge for the first and second point:
        float firstPointHeightAtRightEdge = Terrain.heightAt((float) firstPointRightEdge.x(), (float) firstPointRightEdge.y());
        float secondPointHeightAtRightEdge = Terrain.heightAt((float) secondPointRightEdge.x(), (float) secondPointRightEdge.y());
        /**
         * Calculate the new texture coordinates for the right edge of the road. If there is an overflow so we reach a
         * coordinate higher than 1.0, we reset the first coordinate back to 0 and calculate the second coordinate from there.
         */
        data.rightEdgeTexCoorFirstPoint = data.rightEdgeTexCoorSecondPoint;
        data.rightEdgeTexCoorSecondPoint += secondPointRightEdge.subtract(firstPointRightEdge).length() / 8.0;
        if (data.rightEdgeTexCoorSecondPoint > 1.0) {
            data.rightEdgeTexCoorFirstPoint = 0;
            data.rightEdgeTexCoorSecondPoint = secondPointRightEdge.subtract(firstPointRightEdge).length() / 8.0;
        }
        /**
         * Finally draw the right edge of the track. Set the normal vector in the same direction as the vector pointing to
         * the left, scaled with -1.
         */
        gl.glBegin(GL_TRIANGLE_STRIP);
        for (double z = 1; z >= -1; z -= 0.25) {
            gl.glNormal3d(-firstPointToLeftVector.x(), -firstPointToLeftVector.y(), -firstPointToLeftVector.z());
            gl.glTexCoord2d((z + 1) / 2.0, data.rightEdgeTexCoorFirstPoint);
            gl.glVertex3d(firstPointRightEdge.x(), firstPointRightEdge.y(), firstPointRightEdge.z() - 1 + z);

            gl.glNormal3d(-secondPointToLeftVector.x(), -secondPointToLeftVector.y(), -secondPointToLeftVector.z());
            gl.glTexCoord2d((z + 1) / 2.0, data.rightEdgeTexCoorSecondPoint);
            gl.glVertex3d(secondPointRightEdge.x(), secondPointRightEdge.y(), secondPointLeftEdge.z() - 1 + z);

            //If at any point both the vertex coordinates are below the terrain we can stop drawing.
            if (firstPointRightEdge.z() - 1 + z < firstPointHeightAtRightEdge &&
                    secondPointRightEdge.z() - 1 + z < secondPointHeightAtRightEdge) {
                break;
            }
        }
        gl.glEnd();
        Track.brick.disable(gl);


        /**
         * Draw the top and bottom of the track. We draw the track per lane, using the middle of the lane distances to
         * keep track of texture coordinates on a per-lane basis, to reduce distortion.
         */
        //Initialize the track texture.
        Track.track.enable(gl);
        Track.track.bind(gl);
        //Define an array containing the distances travelled on this piece of track, one for each lane:
        double[] distancesTravelled = new double[numberOfLanes];
        //Loop running over the 4 lanes.
        for (int i = 0; i < numberOfLanes; i++) {
            /**
             * We specify 6 points on this small piece of the track. It looks as follows, taking the tangent vector
             * as the x axis and ToLeftVector as the y-axis:
             *  topLeft      0000000000000000000   topRight
             *               0                 0
             *               0                 0
             *               0                 0
             *  middleLeft   0000000000000000000   middleRight
             *               0                 0
             *               0                 0
             *               0                 0
             *  bottomLeft   0000000000000000000   bottomRight
             *
             *  We use these points as coordinates for our triangle strip.
             */
            Vector topLeft = firstPointLeftEdge.add(firstPointToLeftVector.scale(-trackWidth * (double) i / 4.0));
            Vector topRight = secondPointLeftEdge.add(secondPointToLeftVector.scale(-trackWidth * (double) i / 4.0));
            Vector middleLeft = firstPointLeftEdge.add(firstPointToLeftVector.scale(-trackWidth * (double) i / 4.0 - 0.5));
            Vector middleRight = secondPointLeftEdge.add(secondPointToLeftVector.scale(-trackWidth * (double) i / 4.0 - 0.5));
            Vector bottomLeft = firstPointLeftEdge.add(firstPointToLeftVector.scale(-trackWidth * (double) i / 4.0 - 1.0));
            Vector bottomRight = secondPointLeftEdge.add(secondPointToLeftVector.scale(-trackWidth * (double) i / 4.0 - 1.0));
            /**
             * As distance travelled on this segment of track, we take the distance from middleLeft to middleRight points,
             * we also keep a distance value normalized to texture coordinates:
             */
            distancesTravelled[i] = (middleRight.subtract(middleLeft)).length();
            double distanceNormalized = distancesTravelled[i] / 12.0;
            /**
             * In the data object we kept the previous texture coordinates. Since the texture is 12 long, and we want
             * distance markers every 10 meters, we check if the texture coordinate of the right of the track exceeds
             * 10/12. If so, we restart at zero. Otherwise add the normalized distance to the right coordinate.
             */
            if (data.roadTexCoors[i][1] + distanceNormalized > 0.833333) {
                data.roadTexCoors[i][0] = 0;
                data.roadTexCoors[i][1] = distanceNormalized;
            } else {
                data.roadTexCoors[i][0] = data.roadTexCoors[i][1];
                data.roadTexCoors[i][1] += distanceNormalized;
            }
            //We finished all pre-calculations, draw the top of the track:
            gl.glBegin(GL_TRIANGLE_STRIP);
            gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][0]);
            gl.glNormal3d(firstPointNormal.x(), firstPointNormal.y(), firstPointNormal.z());
            gl.glVertex3d(topLeft.x(), topLeft.y(), topLeft.z());

            gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][1]);
            gl.glNormal3d(secondPointNormal.x(), secondPointNormal.y(), secondPointNormal.z());
            gl.glVertex3d(topRight.x(), topRight.y(), topRight.z());

            gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][0]);
            gl.glNormal3d(firstPointNormal.x(), firstPointNormal.y(), firstPointNormal.z());
            gl.glVertex3d(middleLeft.x(), middleLeft.y(), middleLeft.z());

            gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][1]);
            gl.glNormal3d(secondPointNormal.x(), secondPointNormal.y(), secondPointNormal.z());
            gl.glVertex3d(middleRight.x(), middleRight.y(), middleRight.z());


            gl.glTexCoord2d((double) i / 4.0 + 0.250, data.roadTexCoors[i][0]);
            gl.glNormal3d(firstPointNormal.x(), firstPointNormal.y(), firstPointNormal.z());
            gl.glVertex3d(bottomLeft.x(), bottomLeft.y(), bottomLeft.z());

            gl.glTexCoord2d((double) i / 4.0 + 0.25, data.roadTexCoors[i][1]);
            gl.glNormal3d(secondPointNormal.x(), secondPointNormal.y(), secondPointNormal.z());
            gl.glVertex3d(bottomRight.x(), bottomRight.y(), bottomRight.z());
            gl.glEnd();

            //We only draw the bottom of the track if the first point or second point given is above 1, since otherwise
            //we are assured that the bottom is not visible anyways.
            if (firstPointPos.z() > 1.001 || secondPointPos.z() > 1.001) {
                gl.glPushMatrix();
                gl.glTranslated(0, 0, -2);         //Translate 2 meters downwards.
                //Draw the 6 calculated points, with inverted normals:
                gl.glBegin(GL_TRIANGLE_STRIP);
                gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][0]);
                gl.glNormal3d(-firstPointNormal.x(), -firstPointNormal.y(), -firstPointNormal.z());
                gl.glVertex3d(topLeft.x(), topLeft.y(), topLeft.z());

                gl.glTexCoord2d((double) i / 4.0, data.roadTexCoors[i][1]);
                gl.glNormal3d(-secondPointNormal.x(), -secondPointNormal.y(), -secondPointNormal.z());
                gl.glVertex3d(topRight.x(), topRight.y(), topRight.z());

                gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][0]);
                gl.glNormal3d(-firstPointNormal.x(), -firstPointNormal.y(), -firstPointNormal.z());
                gl.glVertex3d(middleLeft.x(), middleLeft.y(), middleLeft.z());

                gl.glTexCoord2d((double) i / 4.0 + 0.125, data.roadTexCoors[i][1]);
                gl.glNormal3d(-secondPointNormal.x(), -secondPointNormal.y(), -secondPointNormal.z());
                gl.glVertex3d(middleRight.x(), middleRight.y(), middleRight.z());


                gl.glTexCoord2d((double) i / 4.0 + 0.250, data.roadTexCoors[i][0]);
                gl.glNormal3d(-firstPointNormal.x(), -firstPointNormal.y(), -firstPointNormal.z());
                gl.glVertex3d(bottomLeft.x(), bottomLeft.y(), bottomLeft.z());

                gl.glTexCoord2d((double) i / 4.0 + 0.25, data.roadTexCoors[i][1]);
                gl.glNormal3d(-secondPointNormal.x(), -secondPointNormal.y(), -secondPointNormal.z());
                gl.glVertex3d(bottomRight.x(), bottomRight.y(), bottomRight.z());
                gl.glEnd();
                gl.glPopMatrix();
            }
        }
        return distancesTravelled;   //Return the array holding the distances travelled on this cross-section.
    }
}

abstract class RoadSegment {
    //Give a relative distance traveled starting from the start point of this segment. Returns a vector with the coordinate
    // corresponding to that distance traveled.
    public abstract Vector getCoordinate(double s, int LaneNr);



    //Every road segment is able to draw itself. Returns the distance of the lanes in an array.
    abstract void draw(GL2 gl, TrackDrawingData data);

    //Every road segment is able to return a tangent vector, when given a relative distance travelled on the segment.
    abstract Vector getTangent(double s, int laneNr);

    //Every road segment is able to return a normal vector, when given a relative distance travelled on the segment.
    abstract Vector getNormal(double s, int laneNr);
}

class StraightRoadSegment extends RoadSegment {
    Vector startPoint;
    Vector endPoint;
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

        //Calculate the difference vector, the normal and the toLeft vector, which are constant throughout the loop.
        differenceVector = endPoint.subtract(startPoint);
        toLeft = Vector.Z.cross(differenceVector).normalized();
        normalVector = differenceVector.cross(toLeft).normalized();
    }

    @Override
    /**
     * When given a distance s and laneNr give the appropriate coordinate.
     * @param s Distance travelled on this road segment.
     * @param laneNr The lane the robot is in.
     * @return
     */
    public Vector getCoordinate(double s, int laneNr) {
        /**
         * Use the difference vector the find
         * the appropriate point, by adding the differenceVector scaled with a factor s / differenceVector.length to the
         * startPoint. Then move to the side to account for the lane.
         */
        Vector centrePointOnTrack = startPoint.add(differenceVector.scale(s / differenceVector.length()));
        return centrePointOnTrack.add(toLeft.scale(0.375 * trackWidth - (double)laneNr));
    }



    /**
     * Method that draws the straight road segment.
     * @param gl OpenGL context.
     * @param data TrackDrawingData object, containing texture data of previous road-segments.
     */
    @Override
    void draw(GL2 gl, TrackDrawingData data) {
        dt = 1 / (double) resolution;       //Step size is dependant on resolution.
        gl.glColor3f(1f, 1f, 1f);           //Set color to white to avoid interfering with texture.

        /**
         * We loop over the entire segment, generating 2 points every iteration, and then use the TrackCrossSectionDrawer
         * to draw a cross-section connecting these 2 points. In each loop the previous secondPoint gets passed on to the
         * firstPoint, so we precalculate this for the first iteration.
         */
        Vector secondPoint = startPoint;
        for (double t = 0; t < 1; t+=dt){
            /**
             * Pass on the value of the secondPoint to firstPoint, and calculate a new secondPoint.
             */
            Vector firstPoint = secondPoint;
            secondPoint = startPoint.add(differenceVector.scale((t+dt)/1.0));

            //Draw the cross-section.
            TrackCrossSectionDrawer.drawCrossSection(gl,data,firstPoint,differenceVector,secondPoint,differenceVector);
        }
        /**
         * We generate an array holding the distance travelled on each lane (which is needed by the caller to keep track
         * of distances of each segment.). For the case of the straight segment, the distance of each lane is just the
         * length of the difference vector
         */
        double distance = differenceVector.length();
        data.distanceData = new double[]{distance,distance,distance,distance};

    }


    /**
     * @param s Distance travelled on the lane.
     * @param laneNr Lane number the robot is on.
     * @return The tangent vector.
     */
    @Override
    Vector getTangent(double s, int laneNr) {
        return differenceVector.normalized();
    }

    /**
     * @param s Distance travelled on this road segment.
     * @param LaneNr Lane number the robot is on.
     * @return The normal vector.
     */
    @Override
    Vector getNormal(double s, int LaneNr) {
        return normalVector;
    }

}

/**
 * Class containing the info of a bezier-spline road segment, being able to draw it and return relevant data.
 */
class BezierRoadSegment extends RoadSegment {

    /**
     * Control points of the bezier curve.
     */
    private Vector point0;
    private Vector point1;
    private Vector point2;
    private Vector point3;
    private int resolution;
    /**
     * Table containing the distance travelled in each cross-section of the track, first index is the lane number, second
     * index is the distance at t = index number * dt (where t is the variable from 0 to 1 over the bezier curve).
     */
    private double[][] segmentDistances;
    double trackWidth = 4;
    double dt;              //Interval size
    /**
     * Array holding the index in the segmentDistances array corresponding to the previous getDistance request on a per
     * lane basic. It always points to the first index in the segmentDistances that holds a bigger distance than the
     * previous request. Since the first element of that array is distance 0, we initialize it to 1.
     */
    int[] previousDistanceIndex = {1,1,1,1};


    BezierRoadSegment(Vector point0, Vector point1, Vector point2, Vector point3, int resolution) {
        this.point0 = point0;
        this.point1 = point1;
        this.point2 = point2;
        this.point3 = point3;
        this.resolution = resolution;
        segmentDistances = new double[4][resolution + 1];
    }


    /**
     * Method that draws the Bezier segment of the track.
     * @param data TrackDrawingData object
     * @param gl OpenGL context.
     */
    @Override
    void draw(GL2 gl, TrackDrawingData data) {
        dt = 1 / (double) resolution;    //Step size is determined by resolution.
        gl.glColor3f(1f, 1f, 1f);        //Set color to white to avoid interfering with texture.


        /**
         * We loop over the entire segment, generating 2 points every iteration, and then use the TrackCrossSectionDrawer
         * to draw a cross-section connecting these 2 points. In each loop the previous secondPoint and secondPointTangent
         * get passed on to the firstPoint and firstPointTangent variables respectively, therefore before we begin we
         * pre-calculate these for the first iteration.
         */
        Vector secondPointTangent = Util.getCubicBezierTng(0, point0, point1, point2, point3);
        Vector secondPoint = point0;
        for (double t = 0; t < 1; t = t + dt) {
            //Pass on values from the "next" variables to the "current" variables. Calculate new "next" variables.
            Vector firstPointTangent = secondPointTangent;
            secondPointTangent = Util.getCubicBezierTng(t+dt, point0, point1, point2, point3);
            Vector firstPoint = secondPoint;
            secondPoint = Util.getCubicBezierPnt(t + dt, point0, point1, point2, point3);

            //Draw the cross-section, storing the distance travelled on this cross-section in an array.
            double[] distancesCrossSection = TrackCrossSectionDrawer.drawCrossSection(gl, data,firstPoint, firstPointTangent, secondPoint,
                    secondPointTangent);
            for (int i = 0; i<4; i++) {
                segmentDistances[i][(int)Math.round(t/dt) + 1] = segmentDistances[i][(int)Math.round(t/dt)] + distancesCrossSection[i];
            }
        }

        //When drawing the top surface, we recorded the distance values of each lane. We return the final total length
        //of each lane to the calling object, by storing it in the data object.
        double[] distances = {segmentDistances[0][resolution], segmentDistances[1][resolution],
                segmentDistances[2][resolution], segmentDistances[3][resolution]};
        data.distanceData = distances;
    }

    /**
     * Method that returns a variable t (used as variable from 0 to 1 in the bezier spline formula) when given a certain
     * distance and laneNumber.
     * @param s Distance travelled on the relevant lane.
     * @param laneNr Lane the robot is in.
     * @return the value t corresponding
     */
    private double getT(double s, int laneNr) {
        /**
         * previousDistanceIndex always points to the first stored distance that is bigger than the last distance travelled.
         * First we check if this is still the case, otherwise we start the index back at 1. This should only happen when
         * a robot is finished walking on the segment, and at some point later returns to the start of it.
         */
        if (s < segmentDistances[laneNr][previousDistanceIndex[laneNr]-1]){
            previousDistanceIndex[laneNr] = 1;
        }
        /**
         * Find the first element in the segmentDistancesArray that is bigger than the given distance. Since we start looking
         * at previousDistanceIndex, this should only be 0 or 1 step (assuming normal frame-rate / track resolution).
         */
        while (s >= segmentDistances[laneNr][previousDistanceIndex[laneNr]]) {
            previousDistanceIndex[laneNr]++;
        }
        /**
         * We now know that previousDistanceIndex points to the first stored distance that is greater than the request one.
         * Therefore we know that the required t is on the segment corresponding to (previousIndex-1)*dt and previousIndex*dt.
         * First calculate the size of this segment:
         */
        double sizeSegment = segmentDistances[laneNr][previousDistanceIndex[laneNr]] - segmentDistances[laneNr][previousDistanceIndex[laneNr] - 1];
        //Then calculate how much bigger s is than previousIndex-1:
        double overflow = s - segmentDistances[laneNr][previousDistanceIndex[laneNr]-1];
        //Now we use the overflow and sizeSegment to interpolate the requested t.
        return (previousDistanceIndex[laneNr] - 1 + overflow / sizeSegment) * dt;
    }

    /**
     * Method returning a coordinate on a lane, when given a distance traveled on the segment and a lane. Uses the table
     * of stored distances created during drawing to calculate the position.
     * @param s Distance travelled on this road segment.
     * @param laneNr Lane the robot is in.
     * @return The coordinates of the robot on the track.
     */
    @Override
    public Vector getCoordinate(double s, int laneNr) {
        //Use the getT method to find the t used in the bezier curve formula corresponding to the given s and laneNr.
        double t=getT(s, laneNr);

        /**
         * Find the coordinate  of the given distance and laneNr at the middle line of the track, use the tangent
         * vector to move the robot an appropiate amount to the left, in order to put him in the correct lane.
         */
        Vector coordinate = Util.getCubicBezierPnt(t, point0, point1, point2, point3);
        Vector tangent = Util.getCubicBezierTng(t, point0, point1, point2, point3);
        Vector toLeft = Vector.Z.cross(tangent).normalized();
        return coordinate.add(toLeft.scale(trackWidth / 8.0 * 3.0 - (double) laneNr));
    }

    /**
     * Method that returns the tangent vector at a given s and laneNr.
     * @param s The distance travelled on this road segment.
     * @param laneNr The lane the robot is in.
     * @return The tangent vector corresponding to these parameters.
     */
    @Override
    Vector getTangent(double s, int laneNr) {
        //Use the getT method to find the t used in the bezier curve formula corresponding to the given s and laneNr.
        double t = getT(s, laneNr);
        return Util.getCubicBezierTng(t, point0, point1, point2, point3).normalized();
    }


    /**
     * Method that returns the normal vector at a given s and laneNr.
     * @param s The distance travelled on this road segment.
     * @param laneNr The lane the robot is in.
     * @return The normal vector corresponding to the parameters.
     */
    @Override
    public Vector getNormal(double s, int laneNr) {
        //Use the getT method to find the t used in the bezier curve formula corresponding to the given s and laneNr.
        double t = getT(s, laneNr);
        Vector tangent = Util.getCubicBezierTng(t, point0, point1, point2, point3);
        return tangent.cross(Vector.Z.cross(tangent)).normalized();
    }
}

/**
 * Enum containing the coordinates and structure of each track.
 */
enum TrackCoordinates {
    OTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(-7.5, 0, 1), new Vector(-7.5, 15, 1), new Vector(7.5, 15, 1), new Vector(7.5, 0, 1), 50),
            new BezierRoadSegment(new Vector(7.5, 0, 1), new Vector(7.5, -15, 1), new Vector(-7.5, -15, 1), new Vector(-7.5, 0, 1), 50)
    }),

    CTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(-10, -5, 1), new Vector(-5, -10, 1), new Vector(5, -10, 1), new Vector(10, -5, 1), 50),
            new BezierRoadSegment(new Vector(10, -5, 1), new Vector(20, 5, 1), new Vector(20, 20, 1), new Vector(10, 10, 1), 50),
            new BezierRoadSegment(new Vector(10, 10, 1), new Vector(5, 5, 1), new Vector(-5, 5, 1), new Vector(-10, 10, 1), 50),
            new BezierRoadSegment(new Vector(-10, 10, 1), new Vector(-20, 20, 1), new Vector(-20, 5, 1), new Vector(-10, -5, 1), 50)}),

    LTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(10, -10, 1), new Vector(12.5, -10, 1), new Vector(15, -7.5, 1), new Vector(15, -5, 1), 50),
            new StraightRoadSegment(new Vector(15, -5, 1), new Vector(15, 7.5, 1), 20),
            new BezierRoadSegment(new Vector(15, 7.5, 1), new Vector(15, 12.5, 1), new Vector(7.5, 12.5, 1), new Vector(7.5, 7.5, 1), 50),
            new StraightRoadSegment(new Vector(7.5, 7.5, 1), new Vector(7.5, 2.5, 1), 20),
            new BezierRoadSegment(new Vector(7.5, 2.5, 1), new Vector(7.5, 0, 1), new Vector(5, -2.5, 1), new Vector(2.5, -2.5, 1), 50),
            new StraightRoadSegment(new Vector(2.5, -2.5, 1), new Vector(-10, -2.5, 1), 20),
            new BezierRoadSegment(new Vector(-10, -2.5, 1), new Vector(-15, -2.5, 1), new Vector(-15, -10, 1), new Vector(-10, -10, 1), 50),
            new StraightRoadSegment(new Vector(-10, -10, 1), new Vector(10, -10, 1), 20)
    }),

    CUSTOMTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(-9, -9, 3), new Vector(-18, -9, 3), new Vector(-18, 9, 1), new Vector(-9, 9, 1), 76),
            new BezierRoadSegment(new Vector(-9, 9, 1), new Vector(0, 9, 1), new Vector(0, -9, 1), new Vector(9, -9, 1), 76),
            new BezierRoadSegment(new Vector(9, -9, 1), new Vector(18, -9, 1), new Vector(18, 9, 10), new Vector(9, 9, 10), 76),
            new BezierRoadSegment(new Vector(9, 9, 10), new Vector(0, 9, 10), new Vector(0, -9, 3), new Vector(-9, -9, 3), 76)});

    RoadSegment[] roadSegments;

    TrackCoordinates(RoadSegment[] roadSegments) {
        this.roadSegments = roadSegments;
    }
}
