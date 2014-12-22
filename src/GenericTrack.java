import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Created by Noud on 12/22/2014.
 */
public class GenericTrack extends Track {
    RoadSegment[] roadSegments;

    GenericTrack(TrackCoordinates track) {
        this.roadSegments = track.roadSegments;
    }

    double[][] distancesTablePerLane;

    public void draw(GL2 gl) {
        distancesTablePerLane = new double[roadSegments.length + 1][4];

        for (int i = 0; i < roadSegments.length; i++) {
            double[] distances = roadSegments[i].draw(gl);
            for (int j = 0; j < 4; j++) {
                distancesTablePerLane[i + 1][j] = distances[j] + distancesTablePerLane[i][j];
            }
        }
    }

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

    public Vector getNormal(double s, int laneNr) {
        return Vector.Z;
    }
}
