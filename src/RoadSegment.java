import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Created by Noud on 12/20/2014.
 */
public abstract class RoadSegment {

    //Give a relative distance traveled starting from the start point of this segment. Returns a vector with the coordinate
    // corresponding to that distance traveled.
    public abstract Vector getCoordinate(double s, int LaneNr);

    //Every road segment is able to draw itself. Returns the distance of the lanes in an array.
    abstract double[] draw(GL2 gl);

    //Every road segment is able to return a tangent vector, when given a relative distance travelled on the segment.
    abstract Vector getTangent(double s, int laneNr);
}
