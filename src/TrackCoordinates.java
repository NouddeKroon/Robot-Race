import robotrace.Vector;

/**
 * Created by Noud on 12/22/2014.
 */
public enum TrackCoordinates {
    OTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(-15, 0, 1), new Vector(-15, 30, 1), new Vector(15, 30, 1), new Vector(15, 0, 1), 50),
            new BezierRoadSegment(new Vector(15, 0, 1), new Vector(15, -30, 1), new Vector(-15, -30, 1), new Vector(-15, 0, 1), 50)
    }),

    CTRACK(new RoadSegment[]{
            new BezierRoadSegment(new Vector(-20, 0, 1), new Vector(-10, -10, 1), new Vector(10, -10, 1), new Vector(20, 0, 1), 50),
            new BezierRoadSegment(new Vector(20, 0, 1), new Vector(30, 10, 1), new Vector(30, 30, 1), new Vector(20, 20, 1), 100),
            new BezierRoadSegment(new Vector(20, 20, 1), new Vector(10, 10, 1), new Vector(-10, 10, 1), new Vector(-20, 20, 1), 50),
            new BezierRoadSegment(new Vector(-20, 20, 1), new Vector(-30, 30, 1), new Vector(-30, 10, 1), new Vector(-20, 0, 1), 100)}),

    LTRACK(new RoadSegment[]{
            new StraightRoadSegment(new Vector(-20, 0, 1), new Vector(20, 0, 1), 20),
            new BezierRoadSegment(new Vector(20, 0, 1), new Vector(25, 0, 1), new Vector(30, 5, 1), new Vector(30, 10, 1), 100),
            new StraightRoadSegment(new Vector(30, 10, 1), new Vector(30, 35, 1), 20),
            new BezierRoadSegment(new Vector(30, 35, 1), new Vector(30, 40, 1), new Vector(20, 40, 1), new Vector(20, 35, 1), 100),
            new StraightRoadSegment(new Vector(20, 35, 1), new Vector(20, 20, 1), 20),
            new BezierRoadSegment(new Vector(20, 20, 1), new Vector(20, 15, 1), new Vector(15, 10, 1), new Vector(10, 10, 1), 100),
            new StraightRoadSegment(new Vector(10, 10, 1), new Vector(-20, 10, 1), 20),
            new BezierRoadSegment(new Vector(-20, 10, 1), new Vector(-25, 10, 1), new Vector(-25, 0, 1), new Vector(-20, 0, 1), 100)
    }),
//    CUSTOMTRACK(new RoadSegment[]{
//            new BezierRoadSegment(new Vector(-20, -20, 1), new Vector(-40, -20, 1), new Vector(-40, 20, 1), new Vector(-20, 20, 1), 100),
//            new BezierRoadSegment(new Vector(-20, 20, 1), new Vector(-10, 20, 1), new Vector(10, -20, 1), new Vector(20, -20, 1), 100),
//            new BezierRoadSegment(new Vector(20, -20, 1), new Vector(40, -20, 1), new Vector(40, 20, 10), new Vector(20, 20, 10), 100),
//            new BezierRoadSegment(new Vector(20, 20, 10), new Vector(10, 20, 10), new Vector(-10, -20, 1), new Vector(-20, -20, 1), 100)});


    CUSTOMTRACK(new RoadSegment[]{
        new BezierRoadSegment(new Vector(-10, -10, 1), new Vector(-20, -10, 1), new Vector(-20, 10, 1), new Vector(-10, 10, 1), 100),
                new BezierRoadSegment(new Vector(-10, 10, 1), new Vector(-5, 10, 1), new Vector(5, -10, 1), new Vector(10, -10, 1), 100),
                new BezierRoadSegment(new Vector(10, -10, 1), new Vector(20, -10, 1), new Vector(20, 10, 10), new Vector(10, 10, 10), 100),
                new BezierRoadSegment(new Vector(10, 10, 10), new Vector(5, 10, 10), new Vector(-5, -10, 1), new Vector(-10, -10, 1), 100)});

    RoadSegment[] roadSegments;

    TrackCoordinates(RoadSegment[] roadSegments) {
        this.roadSegments = roadSegments;
    }
}
