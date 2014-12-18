import com.jogamp.opengl.util.gl2.GLUT;

import javax.media.opengl.GL2;

// Implement drawing a orthonormal axes system with an origin and unit length arrows along the x-, y-, and z-axes.
// Each of the arrows has its own color as has the origin.
class AxisSystem {
    private final double CONE_HEIGHT = 0.1f;
    private final double CONE_RADIUS = 0.1f;
    private final double BEAM_WIDTH = 0.05f;
    private final double BEAM_HEIGHT = 1 - CONE_HEIGHT;
    private final double ORIGIN_RADIUS = 0.075f;

    // Draw a simple sphere round the origin
    void drawOrigin(GL2 gl, GLUT glut) {
        gl.glColor3f(1.0f, 1.0f, 0.0f); // The origin is colored yellow.

        glut.glutSolidSphere(ORIGIN_RADIUS, 32, 32);
    }

    // Draw a arrow of unit (1 m) length along the z-axis
    void drawAxis(GL2 gl, GLUT glut) {
        gl.glPushMatrix();
        // translate the arrow from the arrow head lying on top the origin to the bottom of the arrow resting on the origin
        gl.glTranslated(0, 0, BEAM_HEIGHT);

        // create a beam hanging down from the origin
        gl.glTranslated(0, 0, -0.5 * BEAM_HEIGHT);
        gl.glScaled(BEAM_WIDTH, BEAM_WIDTH, BEAM_HEIGHT);
        glut.glutSolidCube(1.0f);
        gl.glScaled(1 / BEAM_WIDTH, 1 / BEAM_WIDTH, 1 / BEAM_HEIGHT);
        gl.glTranslated(0, 0, 0.5 * BEAM_HEIGHT);

        // draw the cone which is the arrow's "head"
        glut.glutSolidCone(CONE_RADIUS, CONE_HEIGHT, 64, 64);
        gl.glPopMatrix();
    }

    // draw the complete axis-system
    public void draw(GL2 gl, GLUT glut) {
        // Draw a unit length arrow along the z-axis. The z-axis arrow is blue.
        gl.glColor3f(0, 0, 1.0f);
        drawAxis(gl, glut);

        // The x-axis arrow is colored red and is rotated 90 degrees along y-axis (relative to z-axis)
        gl.glColor3f(1.0f, 0, 0);
        gl.glRotated(90, 0, 1, 0);
        drawAxis(gl, glut);
        gl.glRotated(-90, 0, 1, 0);

        // The y-axis arrow is colored green and is rotated -90 degrees along x-axis
        gl.glColor3f(0, 1.0f, 0);
        gl.glRotated(-90, 1, 0, 0);
        drawAxis(gl, glut);
        gl.glRotated(90, 1, 0, 0);

        drawOrigin(gl, glut);
    }
}