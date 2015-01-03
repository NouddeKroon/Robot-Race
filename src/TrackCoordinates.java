import robotrace.Vector;

/**
 * Created by Noud on 12/22/2014.
 */

/**
 * Enum containing the coordinates and structure of each track.
 */
public enum TrackCoordinates {
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
