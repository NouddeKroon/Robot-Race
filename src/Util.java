import com.jogamp.opengl.util.gl2.GLUT;
import robotrace.Vector;

import javax.media.opengl.GL2;

// Collection of utility functions
class Util {
    /**
     * Convenience function to draw a sphere centered at the position specified by {@code pos}.
     *
     * @param gl OpenGL context
     * @param glut GLUT context
     * @param radius radius of the sphere
     * @param pos center point of the sphere
     */
    static void drawSphere(GL2 gl, GLUT glut, double radius, Vector pos) {
        gl.glTranslated(pos.x(), pos.y(), pos.z());
        glut.glutSolidSphere(radius, 10, 10);
        gl.glTranslated(-pos.x(), -pos.y(), -pos.z());
    }

    /**
     * Convenience function to draw a line between two points.
     *
     * @param gl OpenGL context
     * @param p1 first point
     * @param p2 second point
     */
    static void drawLine(GL2 gl, Vector p1, Vector p2) {
        gl.glBegin(gl.GL_LINES);
        gl.glVertex3d(p1.x(), p1.y(), p1.z());
        gl.glVertex3d(p2.x(), p2.y(), p2.z());
        gl.glEnd();
    }

    /**
     * Convenience function to translate to the position of {@code tr} vector
     * @param gl OpenGl context
     * @param tr position vector to translate to.
     */
    static void translate(GL2 gl, Vector tr) {
        gl.glTranslated(tr.x(), tr.y(), tr.z());
    }

    /**
     * Convenience function to rotate the axis system to new orthonormal system specified by basis{X,Y,Z}.
     *
     * @param gl OpenGL context
     * @param basisX Vector to which the X-axis is rotated
     * @param basisY Vector to which the Y-axis is rotated
     * @param basisZ Vector to which the Z-axis is rotated
     */
    static void rotate(GL2 gl, Vector basisX, Vector basisY, Vector basisZ) {
        // Specify the rotation matrix in row Major order.
        double[] rotMatrix = {basisX.x(), basisX.y(), basisX.z(), 0,
                basisY.x(), basisY.y(), basisY.z(), 0,
                basisZ.x(), basisZ.y(), basisZ.z(), 0,
                0, 0, 0, 1};

        gl.glMultMatrixd(rotMatrix, 0);
    }

    /**
     * Calculate and set the normal in the OpenGL context. The normal is calculated by using the cross product on
     * two vectors specified by three points. The direction of the normal is determined by the right-hand rule.
     *
     * @param gl OpenGL context
     * @param v1x First point's x-coordinate
     * @param v1y First point's y-coordinate
     * @param v1z First point's z-coordinate
     * @param v2x Second point's x-coordinate
     * @param v2y Second point's y-coordinate
     * @param v2z Second point's z-coordinate
     * @param v3x Third point's x-coordinate
     * @param v3y Third point's y-coordinate
     * @param v3z Third point's z-coordinate
     */
    static void setNormalVertex3(GL2 gl,
                                double v1x, double v1y, double v1z,
                                double v2x, double v2y, double v2z,
                                double v3x, double v3y, double v3z) {
        // Calculate first vector
        double vec1x = v2x - v1x;
        double vec1y = v2y - v1y;
        double vec1z = v2z - v1z;

        // Calc second vector
        double vec2x = v3x - v1x;
        double vec2y = v3y - v1y;
        double vec2z = v3z - v1z;

        // Take cross product of the two vectors to get the normal of the face
        // because GL_NORMALIZE is enabled we do not need to normalize the normal vector
        double normalX = vec1y * vec2z - vec1z * vec2y;
        double normalY = vec1z * vec2x - vec1x * vec2z;
        double normalZ = vec1x * vec2y - vec1y * vec2x;

        gl.glNormal3d(normalX, normalY, normalZ);
    }

    /**
     * Calculate and set the normal in the OpenGL context for three OpenGl points.
     *
     * @param gl OpenGL context
     * @param v1x First point's x-coordinate
     * @param v1y First point's y-coordinate
     * @param v1z First point's z-coordinate
     * @param v2x Second point's x-coordinate
     * @param v2y Second point's y-coordinate
     * @param v2z Second point's z-coordinate
     * @param v3x Third point's x-coordinate
     * @param v3y Third point's y-coordinate
     * @param v3z Third point's z-coordinate
     */
    static void makeFaceVertex3(GL2 gl,
                                double v1x, double v1y, double v1z,
                                double v2x, double v2y, double v2z,
                                double v3x, double v3y, double v3z) {
        setNormalVertex3(gl,
                v1x, v1y, v1z,
                v2x, v2y, v2z,
                v3x, v3y, v3z);

        gl.glVertex3d(v1x, v1y, v1z);
        gl.glVertex3d(v2x, v2y, v2z);
        gl.glVertex3d(v3x, v3y, v3z);
    }

    /**
     * Wrapper to calculate and set the normal in the OpenGL context for four OpenGl points.
     *
     * @param gl OpenGL context
     * @param v1x First point's x-coordinate
     * @param v1y First point's y-coordinate
     * @param v1z First point's z-coordinate
     * @param v2x Second point's x-coordinate
     * @param v2y Second point's y-coordinate
     * @param v2z Second point's z-coordinate
     * @param v3x Third point's x-coordinate
     * @param v3y Third point's y-coordinate
     * @param v3z Third point's z-coordinate
     * @param v4x Fourth point's x-coordinate
     * @param v4y Fourth point's y-coordinate
     * @param v4z Fourth point's z-coordinate
     */
    static void makeFaceVertex4(GL2 gl,
                                double v1x, double v1y, double v1z,
                                double v2x, double v2y, double v2z,
                                double v3x, double v3y, double v3z,
                                double v4x, double v4y, double v4z) {
        makeFaceVertex3(gl,
                v1x, v1y, v1z,
                v2x, v2y, v2z,
                v3x, v3y, v3z);

        gl.glVertex3d(v4x, v4y, v4z);
    }

    //

    /**
     * Calculate a point on a cubic Bezier curve.
     *
     * @param t parameter within [0,1]
     * @param P0 Positional vector as first control point
     * @param P1 Positional vector as second control point
     * @param P2 Positional vector as third control point
     * @param P3 Positional vector as fourth control point
     * @return Point, as a positional vector, on the curve
     */
    static public Vector getCubicBezierPnt(double t, Vector P0, Vector P1, Vector P2, Vector P3) {

        //Calculate the individual contributions of the 3 control points.
        Vector p0Contribution = P0.scale(Math.pow(1 - t, 3));
        Vector p1Contribution = P1.scale(3 * t * Math.pow(1 - t, 2));
        Vector p2Contribution = P2.scale(3 * (1 - t) * Math.pow(t, 2));
        Vector p3Contribution = P3.scale(Math.pow(t, 3));

        //Add the all the contributions together and return the result.
        return p0Contribution.add(p1Contribution.add(p2Contribution.add(p3Contribution)));
    }

    /**
     * Calculate the slope of a tangent line at a point, specified by t, on a cubic Bezier curve.
     *
     * @param t parameter within [0,1]
     * @param P0 Positional vector as first control point
     * @param P1 Positional vector as second control point
     * @param P2 Positional vector as third control point
     * @param P3 Positional vector as fourth control point
     * @return Slope of the tangent line
     */
    static public Vector getCubicBezierTng(double t, Vector P0, Vector P1, Vector P2, Vector P3) {
        //The tangent of the cubic Bezier curve has 3 individual vector contributions, calculate these:
        Vector contribution1 = P1.subtract(P0);
        contribution1 = contribution1.scale(3 * Math.pow((1 - t), 2));

        Vector contribution2 = P2.subtract(P1);
        contribution2 = contribution2.scale(6 * t * (1 - t));

        Vector contribution3 = P3.subtract(P2);
        contribution3 = contribution3.scale(3 * Math.pow(t, 2));

        //Add the all the contributions together and return the result.
        return contribution1.add(contribution2.add(contribution3));
    }
}