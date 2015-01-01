import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Created by Noud on 12/22/2014.
 */

/**
 * Class holding an array with all the segments of track, and a table which holds the distance of each segment. It
 * then passes the getPositionOnLane, getTangent and getNormal calls to the appropriate road segment.
 */
public class GenericTrack extends Track {
    RoadSegment[] roadSegments;                 //The array of all the individual roadSegments of which the track consists.
    double[][] distancesTablePerLane;           //Table holding the length of each roadSegment, on a per lane basis.

    GenericTrack(TrackCoordinates track) {
        this.roadSegments = track.roadSegments;
    }

    /**
     * Method that calls each roadSegment to call itself. These segments return an array containing the length of the segment
     * on a per lane basis. Record these in the distancesTablePerLane array.
     */
    @Override
    public void draw(GL2 gl) {
        TextureData data = new TextureData();
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
     */
    @Override
    public Vector getPositionOnLane(double s, int laneNr) {
        s = s % distancesTablePerLane[distancesTablePerLane.length - 1][laneNr];
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
     */
    @Override
    public Vector getTangent(double s, int laneNr) {
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
     */
    @Override
    public Vector getNormal(double s, int laneNr) {
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
