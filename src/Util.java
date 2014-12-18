import com.jogamp.opengl.util.gl2.GLUT;
import robotrace.Vector;

import javax.media.opengl.GL2;

class Util {
    // Convenience method to draw a sphere at the position specified by POS
    static void drawSphere(GL2 gl, GLUT glut, double diameter, Vector pos) {
        gl.glTranslated(pos.x(), pos.y(), pos.z());
        glut.glutSolidSphere(diameter, 10, 10);
        gl.glTranslated(-pos.x(), -pos.y(), -pos.z());
    }

    // convenience method to draw a line between two specified vectors
    static void drawLine(GL2 gl, Vector p1, Vector p2) {
        gl.glBegin(gl.GL_LINES);
        gl.glVertex3d(p1.x(), p1.y(), p1.z());
        gl.glVertex3d(p2.x(), p2.y(), p2.z());
        gl.glEnd();
    }

    // convenience method to translate to the position of tr vector
    static void translate(GL2 gl, Vector tr) {
        gl.glTranslated(tr.x(), tr.y(), tr.z());
    }

    // convenience method to rotate the axis system to new orthonormal system specified by basis{X,Y,Z}.
    static void rotate(GL2 gl, Vector basisX, Vector basisY, Vector basisZ) {
        // Specify the rotation matrix in row Major order.
        double[] rotMatrix = { basisX.x(), basisX.y(), basisX.z(), 0,
                               basisY.x(), basisY.y(), basisY.z(), 0,
                               basisZ.x(), basisZ.y(), basisZ.z(), 0,
                               0,          0,          0,          1 };

        gl.glMultMatrixd(rotMatrix, 0);
    }

    // convenience method to rotate the axis system from a orthonormal system specified by basis{X,Y,Z}.
    static void undoRotate(GL2 gl, Vector basisX, Vector basisY, Vector basisZ) {
        double[] rotMatrix = { basisX.x(), basisY.x(), basisZ.x(), 0,
                               basisX.y(), basisY.y(), basisZ.y(), 0,
                               basisX.z(), basisY.z(), basisZ.z(), 0,
                               0,          0,          0,          1 };

        gl.glMultMatrixd(rotMatrix, 0);
    }

    // Method to calculate and set the normal for a face defined by 3 vertices
    static void makeFaceVertex3(GL2 gl,
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

        gl.glVertex3d(v1x, v1y, v1z);
        gl.glVertex3d(v2x, v2y, v2z);
        gl.glVertex3d(v3x, v3y, v3z);
    }

    // Wrapper to set the normal for faces defined by 4 vertices
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
}