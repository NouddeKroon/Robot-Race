import javax.media.opengl.GL2;
import robotrace.Vector;

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
    int segments;
    double[][] segmentDistances;
    double trackWidth = 4;
    double dt;


    BezierRoadSegment(Vector point0, Vector point1, Vector point2, Vector point3, int segments) {
        this.point0 = point0;
        this.point1 = point1;
        this.point2 = point2;
        this.point3 = point3;
        this.segments = segments;
        segmentDistances = new double[4][segments+1];
    }

    @Override
    public Vector getCoordinate(double s, int laneNr) {
        Vector coordinate = null;
        Vector tangent = null;
        Vector centreToOuter = null;
        s = s % segmentDistances[laneNr][segments];
        int binarySearchResult = Arrays.binarySearch(segmentDistances[laneNr],s);
        if (binarySearchResult < 0) {
            int insertionPoint =  -1 -  binarySearchResult;
            double difference = segmentDistances[laneNr][insertionPoint] - segmentDistances[laneNr][insertionPoint-1];
            double t = (insertionPoint - 1 + ( s - segmentDistances[laneNr][insertionPoint-1] ) / difference)*dt;
            coordinate = Util.getCubicBezierPnt(t,point0,point1,point2,point3);
             tangent = Util.getCubicBezierTng(t, point0, point1, point2, point3);
        } else {
            coordinate = Util.getCubicBezierPnt(dt*binarySearchResult,point0,point1,point2,point3);
            tangent = Util.getCubicBezierTng(dt * binarySearchResult, point0, point1, point2, point3);
        }
        centreToOuter = Vector.Z.cross(tangent).normalized();
        coordinate = coordinate.subtract(centreToOuter.scale(-1.5));

        return coordinate.subtract(centreToOuter.scale((double)laneNr));


    }

    @Override
    double[] draw(GL2 gl) {
        dt = 1 / (double)segments;

        gl.glColor3f(1f, 1f, 0f);

        Vector vectorToLeftNext = Vector.Z.cross(Util.getCubicBezierTng(0, point0, point1, point2, point3)).normalized();
        Vector nextPoint = point0;
        Vector leftEdgePosNext = nextPoint.add(vectorToLeftNext.scale(trackWidth / 2.0));
        Vector rightEdgePosNext = nextPoint.add(vectorToLeftNext.scale(-trackWidth / 2.0));
        for (double t = 0; t<1; t = t + dt) {
            Vector vectorToLeft = vectorToLeftNext;
            vectorToLeftNext = Vector.Z.cross(Util.getCubicBezierTng(t + dt, point0, point1, point2, point3)).normalized();
            nextPoint = Util.getCubicBezierPnt(t + dt, point0, point1, point2, point3);
            Vector leftEdgePosCurrent = leftEdgePosNext;
            leftEdgePosNext = nextPoint.add(vectorToLeftNext.scale(trackWidth / 2.0));
            Vector rightEdgeCurrent = rightEdgePosNext;
            rightEdgePosNext = nextPoint.add(vectorToLeftNext.scale(-trackWidth / 2.0));

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z <= 1; z = z + 0.25) {
                gl.glNormal3d(vectorToLeft.x(), vectorToLeft.y(), vectorToLeft.z());
                gl.glVertex3d(leftEdgePosCurrent.x(), leftEdgePosCurrent.y(), leftEdgePosCurrent.z()-1+z);

                gl.glNormal3d(vectorToLeftNext.x(), vectorToLeftNext.y(), vectorToLeftNext.z());
                gl.glVertex3d(leftEdgePosNext.x(), leftEdgePosNext.y(), leftEdgePosNext.z()-1+z);
            }
            gl.glEnd();

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z < 1; z = z + 0.1) {

                gl.glNormal3d(-vectorToLeft.x(), -vectorToLeft.y(), -vectorToLeft.z());
                gl.glVertex3d(rightEdgeCurrent.x(), rightEdgeCurrent.y(), rightEdgeCurrent.z()-1+z);

                gl.glNormal3d(-vectorToLeftNext.x(), -vectorToLeftNext.y(), -vectorToLeftNext.z());
                gl.glVertex3d(rightEdgePosNext.x(), rightEdgePosNext.y(), rightEdgePosNext.z() -1 + z);
            }
            gl.glEnd();

            gl.glNormal3d(0, 0, 1);
            drawTrackSurface(leftEdgePosCurrent, vectorToLeft, leftEdgePosNext, vectorToLeftNext, gl, (int)Math.round(t/dt));

            gl.glPushMatrix();
            gl.glTranslated(0, 0, -2);
            gl.glNormal3d(0, 0, -1);
            drawTrackSurface(leftEdgePosCurrent, vectorToLeft, leftEdgePosNext, vectorToLeftNext, gl, (int)Math.round(t/dt));
            gl.glPopMatrix();
        }
        double[] distances = {segmentDistances[0][segments],segmentDistances[1][segments],
                segmentDistances[2][segments], segmentDistances[3][segments]};
        return distances;
    }

    @Override
    Vector getTangent(double s, int laneNr) {
        s = s % segmentDistances[laneNr][segments];
        int binarySearchResult = Arrays.binarySearch(segmentDistances[laneNr],s);
        if (binarySearchResult < 0) {
            int insertionPoint =  -1 -  binarySearchResult;
            double difference = segmentDistances[laneNr][insertionPoint] - segmentDistances[laneNr][(insertionPoint-1)];
            double t = (insertionPoint - 1 + ( s - segmentDistances[laneNr][insertionPoint-1] ) / difference)*dt;
            return Util.getCubicBezierTng(t, point0, point1, point2, point3).normalized();
        }
        else return Util.getCubicBezierTng(dt * binarySearchResult, point0, point1, point2, point3).normalized();
    }

    private void drawTrackSurface(Vector leftEdgePosCurrent, Vector vectorToLeft,
                                  Vector leftEdgeNext, Vector vectorToLeftNext, GL2 gl, int segment) {
        double dw = 0.125d;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            Vector innerPoint0 = leftEdgePosCurrent.add(vectorToLeft.scale(-trackWidth * w));
            Vector innerPoint1 = leftEdgeNext.add(vectorToLeftNext.scale(-trackWidth * w));
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());

            if ((w / dw) % 2 == 1) {
                int laneNr = (int) Math.round((w - 0.125) / 0.25);
                Vector distanceVector = innerPoint1.subtract(innerPoint0);
                segmentDistances[laneNr][(segment+1)] = segmentDistances[laneNr][segment] + distanceVector.length();
            }



        }
        gl.glEnd();
    }
}

