import javax.media.opengl.GL2;
import robotrace.Vector;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

/**
 * Created by Noud on 12/20/2014.
 */
public class StraightRoadSegment extends RoadSegment {

    Vector startPoint;
    Vector endPoint;
    int segments;
    double trackWidth = 4.0;
    double dt;
    Vector vectorToLeft;
    Vector differenceVector;

    StraightRoadSegment (Vector startPoint, Vector endPoint, int segments) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.segments = segments;
    }

    @Override
    public Vector getCoordinate(double s, int laneNr) {
        Vector centrePointOnTrack = startPoint.add(differenceVector.scale(s / differenceVector.length()));
        Vector coordinate = centrePointOnTrack.add(vectorToLeft.scale(0.375 * trackWidth));
        coordinate = coordinate.add(vectorToLeft.scale(-laneNr));
        return coordinate;
    }


    @Override
    double[] draw(GL2 gl) {
        dt = 1 / (double)segments;
        gl.glColor3f(1f, 1f, 0f);

        differenceVector = endPoint.subtract(startPoint);
        vectorToLeft = Vector.Z.cross(differenceVector).normalized();
        Vector nextPoint = startPoint;
        Vector rightWallPosNext = nextPoint.add(vectorToLeft.scale(trackWidth / 2.0));
        Vector LeftWallPosNext = nextPoint.add(vectorToLeft.scale(-trackWidth / 2.0));
        for (double t = 0; t<1; t = t + dt) {
            nextPoint = nextPoint.add(differenceVector.scale(dt));
            Vector rightWallPosCurrent = rightWallPosNext;
            rightWallPosNext = nextPoint.add(vectorToLeft.scale(trackWidth / 2.0));
            Vector leftWallPosCurrent = LeftWallPosNext;
            LeftWallPosNext = nextPoint.add(vectorToLeft.scale(-trackWidth / 2.0));

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z <= 1; z = z + 0.25) {
                gl.glNormal3d(vectorToLeft.x(), vectorToLeft.y(), vectorToLeft.z());
                gl.glVertex3d(rightWallPosCurrent.x(), rightWallPosCurrent.y(), z);
                gl.glVertex3d(rightWallPosNext.x(), rightWallPosNext.y(), z);
            }
            gl.glEnd();

            gl.glBegin(GL_TRIANGLE_STRIP);
            for (double z = -1; z < 1; z = z + 0.1) {
                gl.glNormal3d(-vectorToLeft.x(), -vectorToLeft.y(), -vectorToLeft.z());
                gl.glVertex3d(leftWallPosCurrent.x(), leftWallPosCurrent.y(), z);
                gl.glVertex3d(LeftWallPosNext.x(), LeftWallPosNext.y(), z);
            }
            gl.glEnd();

            gl.glNormal3d(0, 0, 1);
            drawTrackSurface(rightWallPosCurrent, vectorToLeft, rightWallPosNext, gl, (int)Math.round(t/dt));

            gl.glPushMatrix();
            gl.glTranslated(0, 0, -2);
            gl.glNormal3d(0, 0, -1);
            drawTrackSurface(rightWallPosCurrent, vectorToLeft, rightWallPosNext, gl, (int)Math.round(t/dt));
            gl.glPopMatrix();
        }
        double[] distances = {differenceVector.length(),differenceVector.length(),differenceVector.length(),differenceVector.length()};
        return distances;
    }

    private void drawTrackSurface(Vector innerRingPosCurrent, Vector centreToInner,
                                  Vector innerRingPosNext,  GL2 gl, int segment) {
        double dw = 0.125d;
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        for (double w = 0; w <= 1.0d; w += dw) {
            Vector innerPoint0 = innerRingPosCurrent.add(centreToInner.scale(-trackWidth * w));
            Vector innerPoint1 = innerRingPosNext.add(centreToInner.scale(-trackWidth * w));
            gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
            gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());
        }
        gl.glEnd();
    }

    @Override
    Vector getTangent(double s, int laneNr) {
        return differenceVector.normalized();
    }


}
