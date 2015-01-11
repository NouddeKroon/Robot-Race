import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import robotrace.GlobalState;
import robotrace.Vector;

import javax.media.opengl.GL2;

import java.util.Random;

/**
 * Represents a Robot implemented according to the Assignments.
 */
class Robot {
    // Size of the spheres used during drawing of the stick figure
    static final double stickSphereRadius = 0.033f;

    // Translation of the position of the center of the robot on top of the xy-plane
    static final Vector torsoTrans = new Vector(0, 0, 0.75f);

    // Coordinates for limbs relative to center of robot's torso
    static final Vector neck = new Vector(0, 0, 0.25f);
    static final Vector rightShoulder = new Vector(0.3f, 0, 0.2f);
    static final Vector leftShoulder = new Vector(-0.3f, 0, 0.2f);
    static final Vector rightHip = new Vector(0.15f, 0, -0.25f);
    static final Vector leftHip = new Vector(-0.15f, 0, -0.25f);

    // Coordinates for limbs relative to other limbs
    static final Vector upperToLowerArm = new Vector(0, 0, -0.275);
    static final Vector lowerArmToHand = new Vector(0, 0, -0.25);
    static final Vector upperToLowerLeg = new Vector(0, 0, -0.19);
    static final Vector lowerLegToFoot = new Vector(0, 0, -0.19);

    // Dimensions for the head, also used by the camera to figure out first person view point
    static final double headWidth = 1.0;
    static final double headHeight = 0.5;
    static final double headDepth = 0.25;

    // Calculate the height defined by the previous dimensions, used by the camera.
    static final double height = torsoTrans.z() + (neck.z() - rightHip.z()) / 2 + neck.z() + headHeight;

    // Head colors
    float[] neckColor;
    float[] headColor;
    float[] scleraColor = {1, 1, 1};
    float[] irisColor = {0, 0, 0};
    float[] mouthColor = {0, 0, 0};
    // Torso colors
    float[] torsoColor;
    float[] torsoScreenColor;
    // Upper arm colors
    float[] armColor;
    float[] elbowColor;
    // Lower arm colors
    float[] lowerArmColor = {0.2f, 0.2f, 0.2f};
    float[] hexPartColor = {0.1f, 0.1f, 0.1f};
    // Hand colors
    float[] handCylinderColor;
    // Claw colors
    float[] clawColor;
    // Upper leg colors
    float[] torsoJointColor;
    float[] upperLegColor;
    // Lower leg colors
    float[] jointUpperLowerLegColor;
    float[] lowerLegColor;
    float[] footColor;

    // Reference to the global state, to check if the track changed and determine whether to draw stick figures or not.
    GlobalState gs;
    // Keep a reference to the track the robot is on.
    private RaceTrack track;
    // Maintain in which lane the robot is running.
    private int trackLane;

    //Maintain which track is currently selected.
    private int trackNr;

    // The textures to be mapped to the head and torso respectively.
    static Texture headTex;
    static Texture torsoTex;

    // The speed with which the robot will run on a non-inclined path.
    double baseSpeed;
    // Factor with which the robot's speed increases/decreases on inclines.
    double inclinationFactor;

    double distCovered = 0; // In meters, since the start of the current race.

    // Keep track of the last position, last tangent and last normal.
    // This information along with the speed and frame time allow us to calculate the robot's new position and direction.
    private Vector pos;

    // Objects which abstract the calculations of linear front-to-back-to-front rotation of the limbs.
    LimbRotation upperArmRotate = new LimbRotation(upperToLowerArm.length(), -22.5, 37.5);
    LimbRotation lowerArmRotate = new LimbRotation(lowerArmToHand.length(), -10, 50);
    LimbRotation upperLegRotate = new LimbRotation(upperToLowerLeg.length(), 0, 40);
    LimbRotation lowerLegRotate = new LimbRotation(lowerLegToFoot.length(), -40, 5);

    // Display list for all major loops
    static int lowerArmConeDisplayList = 0;
    static int lowerArmHexPartDisplayList = 0;
    static int clawDisplayList = 0;
    static int headDisplayList = 0;

    // Used as a parameter during drawing to specify the side of the robot.
    enum Side {
        Left,
        Right
    }

    /**
     * The material from which this robot is built.
     */
    private final Material material;

    /**
     * Constructs the robot with initial parameters.
     */
    public Robot(Material material, RaceTrack track, int trackLane, GlobalState gs) {
        this.material = material;
        this.track = track;
        this.trackLane = trackLane;
        this.gs = gs;

        // Set a random speed of the robot on horizontal paths relative to common base speed of 15 m/s.
        baseSpeed = 15 + 4 * (new Random().nextDouble() - 0.5); // m/s
        // Set the factor with which the robot's speed varies depending on the inclination angle.
        inclinationFactor = 0.5 + 0.5 * new Random().nextDouble();

        // Add phase shift of 1/8 period as offset to the upper leg rotation for more natural looking movement.
        lowerLegRotate.addAngle(0.25 * lowerLegRotate.getMaxDelta());
    }

    // Getter for position of the robot
    public Vector getPosition() {
        return pos;
    }

    // Calculate the tangent to the current path of the robot.
    public Vector getTangent() {
        return track.getTangent(distCovered, trackLane);
    }

    // Calculate the normal to the current path of the robot.
    public Vector getNormal() {
        return track.getNormal(distCovered, trackLane);
    }

    /**
     * Calculate the new position and direction based on the current position, direction (normal, tangent of path),
     * robot's speed (dependent on the inclination angle) and the time difference since last frame.
     *
     * @param timeDiff the time difference between drawing the current frame and this frame.
     */
    public void updatePos(long timeDiff) {
        final double minimizeSlipCoEfficient = 0.33;

        //If a new track is selected, reset the start distance so the robots start back at the start line.
        if (this.trackNr != track.trackNr) {
            this.distCovered = 0;
            this.trackNr = track.trackNr;
        }

        // Get the previous tangent to calculate the incline
        Vector tangent = track.getTangent(distCovered, trackLane);

        // Calculate an incline factor by dividing the angle between the tangent and the projection of the tangent on
        // the XOY plane by PI / 2. The z coord will specify whether we are moving up an incline or down.
        // The final value of inclination will be in [-1, 1], from straight down to straight up.
        Vector projectedTangent = new Vector(tangent.x(), tangent.y(), 0).normalized();

        double posNegIncline = tangent.z() >= 0 ? 1 : -1;
        double inclination = posNegIncline * Math.asin(tangent.cross(projectedTangent).length()) / (0.5 * Math.PI);

        double speed = (1 + (-1) * inclinationFactor * inclination) * baseSpeed;
        double dist = (timeDiff / 10e9) * speed;

        distCovered += dist;

        // Tell the end of upper arm to travel a distance
        double angle = upperArmRotate.rotateDist(minimizeSlipCoEfficient * dist);

        // All the rotation have the same period therefore add the scaled angle to them all.
        lowerArmRotate.addAngle(angle / upperArmRotate.getMaxDelta() * lowerArmRotate.getMaxDelta());
        upperLegRotate.addAngle(angle / upperArmRotate.getMaxDelta() * upperLegRotate.getMaxDelta());
        lowerLegRotate.addAngle(angle / upperArmRotate.getMaxDelta() * lowerLegRotate.getMaxDelta());

        pos = track.getPositionOnLane(distCovered, trackLane);
    }

    /**
     * Draw the robot at the last calculated position and with the appropriate direction.
     */
    public void drawAtPos(GL2 gl, GLUT glut) {
        Vector normal = track.getNormal(distCovered, trackLane);
        Vector tangent = track.getTangent(distCovered, trackLane);

        gl.glPushMatrix();

        Util.translate(gl, pos);
        Util.rotate(gl, normal.cross(tangent), tangent, normal);
        draw(gl, glut);

        gl.glPopMatrix();
    }


    /**
     * Method that draws the head model. This method assumes that (0,0,0) coordinate is the joint connecting the head
     * to the body. You can easily change the variables defined at the start of the method to reshape the head.
     */
    private void drawHead(GL2 gl, GLUT glut) {
        double neckSize = 0.10;
        double eyeRadius = 0.12;
        double eyeDepth = 0.05;
        double irisDepth = 0.03;
        double antennaSize = 0.1;
        double neckAngle = 10;

        if (gs.showStick) {                                         //If gs.showStick is true, draw only stick-figure.
            gl.glColor3f(0, 0, 0);                                  //Stick-figure is always black.
            gl.glTranslated(0, 0, 0.5 * neckSize + 0.5 * headHeight);     //Translate up, to about the centre of the head
            glut.glutSolidSphere(stickSphereRadius, 10, 10);           //Draw a stickSphere-radius sphere
            gl.glTranslated(0, 0, -(0.5 * neckSize + 0.5 * headHeight));  //Translate back to the joint connecting neck and body

            //Draw a line from the base of the head to the stick figure sphere in the centre of the head.
            gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 0.5 * neckSize + 0.5 * headHeight);
            gl.glEnd();

        } else {
            double texHeightOffset = 0;
            double texWidthOffset = 0;
            double texWidth = 0.5d;
            double texHeight = 0.5d;

            //Draw the neck
            gl.glColor3d(neckColor[0], neckColor[1], neckColor[2]);         //Set the color to the neck color.
            gl.glPushMatrix();                                              //Push new matrix
            gl.glRotated(-neckAngle, 1, 0, 0);                      //Rotate around x axis with appropiate angle
            gl.glTranslated(0, 0, -0.05);           //Translate a bit downwards so entire cylinder is inside body
            //Draw cylinder with appropiate size. We make it a bit bigger than necksize, so there is no open space.
            glut.glutSolidCylinder(0.75 * neckSize, 1.5 * neckSize, 30, 1);
            gl.glPopMatrix();                                               //Restore to original matrix

                /* Draw the head. The head is built from 6 quads, together forming a figure like a frustum. We use the
                method makeFaceVertex4 to draw the quads, which automatically does the normal vectors, as long as we
                make sure to define the vertices in a counterclockwise fashion (otherwise normal is inverted). */
            gl.glColor3f(headColor[0], headColor[1], headColor[2]);         //Set color to color of head.
            /**
             * If a displayList for head is already generated, we call it. Otherwise we generate and compile it.
             */
            if (headDisplayList != 0) {
                gl.glCallList(headDisplayList);
            } else {
                headDisplayList = gl.glGenLists(1);
                gl.glNewList(headDisplayList, gl.GL_COMPILE_AND_EXECUTE);

                // The head texture image is split into four square the first (top left) is the front of the head,
                // top right is the back side, bottom left is one of the sides and bottom right is the top of the head.
                headTex.enable(gl);
                headTex.bind(gl);

                gl.glBegin(gl.GL_QUADS);                                           //Start drawing quads.

                final int FRONT = 0, BACK = 1, TOP_LEFT = 0, TOP_RIGHT = 1, BOTTOM_LEFT = 2, BOTTOM_RIGHT = 3;
                double[][][] coords = { /* coords of front face: */ {
                        {-0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,},   // Top Left
                        {0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,},    // Top Right
                        {-0.5 * headWidth, headDepth, 0.5 * neckSize,},                // Bottom left
                        {0.5 * headWidth, headDepth, 0.5 * neckSize,},                 // Bottom Right
                }, {
                    /* coords of back face: */
                        {-0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,},  // Top Left
                        {0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,},   // Top Right
                        {-0.3 * headWidth, -headDepth, neckSize,},                     // Bottom Left
                        {0.3 * headWidth, -headDepth, neckSize,},                      // Bottom Right
                }
                };

                //Draw front side of head
                Util.setNormalVertex3(gl,
                        coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                        coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                        coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);
                // Map all the coordinates to the top left square in the texture.
                gl.glTexCoord2d(0, 1.0);
                gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);
                gl.glTexCoord2d(0.5, 1.0);
                gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
                gl.glTexCoord2d(0.5, 0.5);
                gl.glVertex3d(coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);
                gl.glTexCoord2d(0, 0.5);
                gl.glVertex3d(coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);

                //Draw back side of head
                Util.setNormalVertex3(gl,
                        coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2],
                        coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2],
                        coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
                // Map all the coordinates to the top right square in the texture.
                gl.glTexCoord2d(0.5, 1.0);
                gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
                gl.glTexCoord2d(1, 1.0);
                gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
                gl.glTexCoord2d(1, 0.5);
                gl.glVertex3d(coords[BACK][BOTTOM_RIGHT][0], coords[BACK][BOTTOM_RIGHT][1], coords[BACK][BOTTOM_RIGHT][2]);
                gl.glTexCoord2d(0.5, 0.5);
                gl.glVertex3d(coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2]);

                //Draw left side of head
                Util.setNormalVertex3(gl,
                        coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                        coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2],
                        coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
                // Map all the coordinates to the bottom left square in the texture.
                gl.glTexCoord2d(0, 0.5);
                gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);
                gl.glTexCoord2d(0.5, 0.5);
                gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
                gl.glTexCoord2d(0.5, 0.0);
                gl.glVertex3d(coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2]);
                gl.glTexCoord2d(0, 0.0);
                gl.glVertex3d(coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);

                //Draw right side of head
                Util.setNormalVertex3(gl,
                        coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                        coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2],
                        coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);
                // Map all the coordinates to the bottom right square in the texture.
                gl.glTexCoord2d(0, 0.5);
                gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
                gl.glTexCoord2d(0.5, 0.5);
                gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
                gl.glTexCoord2d(0.5, 0.0);
                gl.glVertex3d(coords[BACK][BOTTOM_RIGHT][0], coords[BACK][BOTTOM_RIGHT][1], coords[BACK][BOTTOM_RIGHT][2]);
                gl.glTexCoord2d(0, 0.0);
                gl.glVertex3d(coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);

                //Draw top of head
                Util.setNormalVertex3(gl,
                        coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                        coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                        coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
                // Map all the coordinates to the bottom left square in the texture.
                gl.glTexCoord2d(0.5, 0.5);
                gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
                gl.glTexCoord2d(0.5, 0.0);
                gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
                gl.glTexCoord2d(1.0, 0.0);
                gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
                gl.glTexCoord2d(1.0, 0.5);
                gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);

                //draw bottom of head
                Util.makeFaceVertex4(gl, 0.5 * headWidth, headDepth, 0.5 * neckSize,
                        0.3 * headWidth, -headDepth, neckSize,
                        -0.3 * headWidth, -headDepth, neckSize,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize);

                gl.glEnd();             //We are done drawing quads.
                headTex.disable(gl);

                //Draw antenna.
                gl.glPushMatrix();                  //Push a new matrix.
                gl.glTranslated(-0.25 * headWidth, -0.25 * headDepth, neckSize + 0.8 * headHeight); //Translate to location of antenna.
                glut.glutSolidCone(0.2 * antennaSize, antennaSize, 20, 20);      //Draw a solidcone, using antennasize.
                gl.glTranslated(0, 0, antennaSize);                           //Translate to top of antenna.
                glut.glutSolidSphere(0.2 * antennaSize, 20, 20);            //Draw a small sphere, relative to antennasize.
                gl.glPopMatrix();                                           //Return to original matrix.

                //Draw right eye
                gl.glPushMatrix();                                              //Push a matrix to store current position.
                gl.glColor3d(scleraColor[0], scleraColor[1], scleraColor[2]);   //Set color to sclera color.
                gl.glTranslated(0.2 * headWidth, headDepth, 0.5 * neckSize + 0.6 * headHeight); //Translate to eye position.
                gl.glRotated(-90, 1, 0, 0);                                     //Rotate around x axis.
                glut.glutSolidCylinder(eyeRadius, eyeDepth, 100, 5);                //Draw the cylinder forming the sclera.
                gl.glTranslated(-0.3 * eyeRadius, 0, eyeDepth);                     //Translate to surface of sclera.
                gl.glColor3d(irisColor[0], irisColor[0], irisColor[0]);             //Set color to iriscolor.
                glut.glutSolidCylinder(0.7 * eyeRadius, irisDepth, 100, 5);         //Draw the iris.
                gl.glTranslated(0.3 * eyeRadius, 0, -eyeDepth);                     //Translate back to centre of eye.

                //Draw left eye
                gl.glColor3d(scleraColor[0], scleraColor[1], scleraColor[2]);   //Set color to sclera color.
                gl.glTranslated(-0.4 * headWidth, 0, 0);                        //Translate to opposite side of head.
                glut.glutSolidCylinder(eyeRadius, eyeDepth, 100, 5);            //Draw the cylinder forming the sclera.
                gl.glColor3d(irisColor[0], irisColor[1], irisColor[2]);         //Set color to iris color.
                gl.glTranslated(-0.3 * eyeRadius, 0, eyeDepth);                 //Translate to surface of sclera.
                glut.glutSolidCylinder(0.7 * eyeRadius, irisDepth, 100, 5);     //Draw the iris.
                gl.glPopMatrix();              //Done drawing eyes, pop the matrix.


                gl.glPushMatrix();              //Push a new matrix to store current position.
                gl.glColor3f(mouthColor[0], mouthColor[1], mouthColor[2]);          //Set color to mouth color.
                gl.glTranslated(0, headDepth + 0.015, 0.5 * neckSize + 0.3 * headHeight);   //Translate to mouth position.

                //The mouth is a polygon of which the top side is a straight line, and the bottom side are points on a circle.
                gl.glBegin(gl.GL_POLYGON);             //Begin drawing a polygon.
                gl.glNormal3d(0, 1, 0);               //Set normal vector
                //Draw the top side of the mouth.
                gl.glVertex3d(-0.15 * headWidth, 0, 0);
                gl.glVertex3d(0.15 * headWidth, 0, 0);
                //Calculate the centre coordinate and radius of the circle of which the bottom of the mouth is a subsection.
                double zOffsetCentre = Math.sin(Math.toRadians(15)) * 0.15 * headWidth;
                double radius = Math.sqrt(Math.pow(zOffsetCentre, 2) + Math.pow(0.15 * headWidth, 2));
                //In a loop we generate 50 points on the circle, together forming the bottom of the mouth.
                for (int i = 0; i < 50; i++) {
                    gl.glVertex3d(
                            Math.sin(Math.toRadians(75 - 3 * i)) * radius,
                            0,
                            zOffsetCentre - Math.cos(Math.toRadians(75 - 3 * i)) * radius);
                }
                gl.glEnd();                     //We are finished generating points for the polygon.
                gl.glPopMatrix();               //Return to original matrix.


                gl.glEndList();
            }
        }
    }

    /**
     * Draw the robot's arm.
     * The arm is drawn with its shoulder joint centered on the x-y plane hanging down from the origin
     *
     * @param gl OpenGl context
     * @param glut GLUT context
     * @param side The side used to determine whether to draw the animation out of phase.
     */
    private void drawArm(GL2 gl, GLUT glut, Side side) {
        double armRadius = 0.05;
        double elbowRadius = 0.055;

        if (gs.showStick) {
            // The stick figure arm is a black line from the shoulder joint to a black sphere at the elbow joint
            gl.glColor3f(0, 0, 0);
            Util.drawSphere(gl, glut, stickSphereRadius, upperToLowerArm);
            Util.drawLine(gl, Vector.O, upperToLowerArm);
        } else {
            // set elbow joint color
            gl.glColor3f(elbowColor[0], elbowColor[1], elbowColor[2]);
            // Draw the sphere of the elbow joint
            gl.glTranslated(0, 0, -1 * upperToLowerArm.length());
            glut.glutSolidSphere(elbowRadius, 10, 10);
            // Draw the cylinder representing the upper arm
            gl.glColor3f(armColor[0], armColor[1], armColor[2]);
            glut.glutSolidCylinder(armRadius, upperToLowerArm.length(), 10, 10);
            gl.glTranslated(0, 0, upperToLowerArm.length());
        }

        // Draw the lower arm at the position of the center of the elbow joint and apply a rotation
        gl.glPushMatrix();
        Util.translate(gl, upperToLowerArm);
        // Give the lower arm a base rotation for a more "natural" stance.
        gl.glRotated(20, 1, 1, 0);

        // Retrieve the current angle determined for the animation of the lower arm relative to the upper arm.
        // The two arms are exactly half a period out of phase with each other.
        double angle = side == Side.Right ? lowerArmRotate.getAngle() : lowerArmRotate.getAngleHalfPhaseShift();
        gl.glRotated(angle, 1, 0, 0);
        drawLowerArm(gl, glut, side);
        gl.glPopMatrix();
    }

    /**
     * Draw the robot's lower arm.
     * The lower arm is drawn centered on the x-y plane hanging down from the origin
     *
     * @param gl OpenGl context
     * @param glut GLUT context
     * @param side The side used to determine whether to draw the animation out of phase.
     */
    private void drawLowerArm(GL2 gl, GLUT glut, Side side) {
        // specify the top circle used for the "cut off" cone of the lower arm
        Vector topCirclePos = new Vector(0.02, 0, 0.2);
        double topCircleRadius = 0.1;

        // specify the bottom circle used for the "cut off" cone of the lower arm
        Vector bottomCirclePos = new Vector(0, 0, 0);
        double bottomCircleRadius = 0.075;

        // Specify the circle to be used to generate a tapering hex shape to be connected to the sphere of the elbow joint
        Vector hexPartPos = new Vector(0, 0, lowerArmToHand.length());
        double hexPartRadius = 0.05;

        if (gs.showStick) {
            // the lower arm stick figure is a black line connected to a black sphere
            gl.glColor3f(0, 0, 0);
            Util.drawSphere(gl, glut, stickSphereRadius, lowerArmToHand);
            Util.drawLine(gl, Vector.O, lowerArmToHand);
        } else {
            // translate to make the lower arm "hang" from the origin
            gl.glTranslated(lowerArmToHand.x(), lowerArmToHand.y(), lowerArmToHand.z());

            gl.glColor3f(lowerArmColor[0], lowerArmColor[1], lowerArmColor[2]);

            /**
             * If lowerArmConeDisplayList is already generated, we call it. Otherwise, we generate and compile the list.
             */
            if (lowerArmConeDisplayList != 0) {
                gl.glCallList(lowerArmConeDisplayList);
            } else {
                lowerArmConeDisplayList = gl.glGenLists(1);
                gl.glNewList(lowerArmConeDisplayList, gl.GL_COMPILE_AND_EXECUTE);


                // draw the circle which closes of the top of the "cut-off" cone
                gl.glBegin(gl.GL_TRIANGLE_FAN);

                // the normal for the top face is directed along z-axis
                gl.glNormal3d(0, 0, 1);

                // Initial vertex is the center of the circle
                gl.glVertex3d(topCirclePos.x(), topCirclePos.y(), topCirclePos.z());

                // for every 5 degrees draw a triangle from the center to the edge of the circle
                for (int i = 0; i <= 360; i+=5) {
                    // calculate the rad angle only once
                    double angle = Math.toRadians(i);

                    gl.glVertex3d(topCircleRadius * Math.cos(angle) + topCirclePos.x(),
                            topCircleRadius * Math.sin(angle) + topCirclePos.y(),
                            topCirclePos.z());
                }
                gl.glEnd();

                // draw the circle which closes of the bottom of the "cut-off" cone
                gl.glBegin(gl.GL_TRIANGLE_FAN);

                // the normal for the bottom face is directed along negative z-axis
                gl.glNormal3d(0, 0, -1);

                gl.glVertex3d(bottomCirclePos.x(), bottomCirclePos.y(), bottomCirclePos.z());

                for (int i = 0; i <= 360; i+=5) {
                    double angle = Math.toRadians(i);
                    gl.glVertex3d(bottomCircleRadius * Math.cos(angle) + bottomCirclePos.x(),
                            bottomCircleRadius * Math.sin(angle) + bottomCirclePos.y(),
                            bottomCirclePos.z());
                }
                gl.glEnd();

                /*
                Draw "cut-off"-cone formed by the two circles. To draw this cone we use a triangle strip,
                drawing a total of 720 triangles in a for loop, using some basic geometry to determine the position
                of the coordinates on the bottom and top circle-surfaces. We want smooth shading on this surface.
                To achieve this we define a normal for each vertex, which is the average of the normalized
                normals of all the triangles this vertex borders. Since GL_NORMALIZE is enabled, if we sum the
                3 normals of the surfaces bordering the vertex, we don't have to normalize ourselves. Consider the
                following ASCII image:
                  TopPrev        TopCur         TopNext
                    *              *              *
                    **             **             *
                    * *            * *            *
                    *  *           *  *           *
                    *   *     n2   *   *     n4   *
                    *    *         *    *         *
                    *     *        *     *        *
                    *      *       *      *       *
                    *       *      *       *      *
                    *        *     *        *     *
                    *   n1    *    *    n3   *    *
                    *          *   *          *   *
                    *           *  *           *  *
                    *            * *            * *
                    *             **             **
                    *              *              *
                 BotPrev        BotCur          BotNext

                 In this image BotPrev, BotCur and BotNext are coordinates on the bottom circle, respectively of the
                 previous, current and next loop (the current ones being the vertices that we are drawing in this loop
                 iteration). Similarly TopPrev, TopCur and TopNext are coordinates on the top circle. The normal for the
                 vertex on BotCur, will be the average of the normals n1, n2 and n3. The normal of the vertex on TopCur
                 will be the average of normals n2, n3 and n4. In order to obtain these normals we take cross product of
                 vectors going between the points.

                 In each iteration of the loop, the normals n3 and n4 can be reused. The vectors between
                 TopCur and BotCur, TopCur and BotNext and between TopNext and BotNext can be reused as well.
                 We calculate these parameters for the iteration i=-1 before the loop starts.
                */
                gl.glBegin(gl.GL_TRIANGLE_STRIP);


                /*
                Calculate reusable vectors and normals for iteration i=-1 before the loop starts. We always calculate
                the vector going from top to bottom. In the calculation of the normals we have to keep the right-hand
                rule for cross product in mind.
                */
                double stepSize = 5;
                double angle = Math.toRadians(-stepSize);
                double angleNext = Math.toRadians(0);
                Vector vectorTopCurToBotCur = new Vector(
                        bottomCircleRadius * Math.cos(angle) + bottomCirclePos.x() - topCircleRadius * Math.cos(angle) - topCirclePos.x(),
                        bottomCircleRadius * Math.sin(angle) + bottomCirclePos.y() - topCircleRadius * Math.sin(angle) - topCirclePos.y()
                        , bottomCirclePos.z() - topCirclePos.z());
                Vector vectorTopCurToBotNext = new Vector(
                        bottomCircleRadius * Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius * Math.cos(angle) - topCirclePos.x(),
                        bottomCircleRadius * Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius * Math.sin(angle) - topCirclePos.y()
                        , bottomCirclePos.z() - topCirclePos.z());
                Vector vectorTopNextToBotNext = new Vector(
                        bottomCircleRadius * Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius * Math.cos(angleNext) - topCirclePos.x(),
                        bottomCircleRadius * Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius * Math.sin(angleNext) - topCirclePos.y()
                        , bottomCirclePos.z() - topCirclePos.z());

                Vector n1;
                Vector n2;
                Vector n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                Vector n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();

                for (int i = 0; i <= 360; i+=stepSize) {
                    // calculate the angle only once for every degree:
                    angle = angleNext;
                    angleNext = Math.toRadians(i + stepSize);

                    //What was in the previous loop the vector from TopNext to BotNext, is in iteration of the loop the
                    //vector between TopCur and BotCur, so we can pass it on. Also calculate the other 2 necessary vectors:
                    vectorTopCurToBotCur = vectorTopNextToBotNext;
                    vectorTopCurToBotNext = new Vector(
                            bottomCircleRadius * Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius * Math.cos(angle) - topCirclePos.x(),
                            bottomCircleRadius * Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius * Math.sin(angle) - topCirclePos.y()
                            , bottomCirclePos.z() - topCirclePos.z());
                    vectorTopNextToBotNext = new Vector(
                            bottomCircleRadius * Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius * Math.cos(angleNext) - topCirclePos.x(),
                            bottomCircleRadius * Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius * Math.sin(angleNext) - topCirclePos.y()
                            , bottomCirclePos.z() - topCirclePos.z());


                    /*
                    What was in the previous iteration of the loop n3 is now n1, and what was n4 is now n2, so we pass
                    on these values. Also calculate the new normals n3 and n4 using cross product, taking the right
                    hand rule in mind. Normalize these vectors.
                    */
                    n1 = n3;
                    n2 = n4;
                    n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                    n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();


                    //Now we just add the relevant vectors up for the bottom normal and top normal.
                    Vector normalVectorBottom = n1.add(n2.add(n3));
                    Vector normalVectorTop = n2.add(n3.add(n4));


                    // Draw the bottom vertex using the calculated normal.
                    gl.glNormal3d(normalVectorBottom.x(), normalVectorBottom.y(), normalVectorBottom.z());
                    gl.glVertex3d(bottomCircleRadius * Math.cos(angle) + bottomCirclePos.x(),
                            bottomCircleRadius * Math.sin(angle) + bottomCirclePos.y(),
                            bottomCirclePos.z());
                    // Draw the top vertex using the calculated normal.
                    gl.glNormal3d(normalVectorTop.x(), normalVectorTop.y(), normalVectorTop.z());
                    gl.glVertex3d(topCircleRadius * Math.cos(angle) + topCirclePos.x(),
                            topCircleRadius * Math.sin(angle) + topCirclePos.y(),
                            topCirclePos.z());
                }

                gl.glEnd();                 //Finished drawing lower arm.
                gl.glEndList();
            }

                /* Now we draw the "hex part", connecting the lower arm to the elbow joint. The technique we use for
                * this is exactly the same as for the lower arm cone, so we refer to that for clarification. Now, the
                * top circle is on the elbow joint, and the bottom circle is the top of the lower arm cone (the coordinates
                * of this circle are called topCircle, which might be confusing). We use 60 degree steps, so we have 12
                * triangles total.
                */
            gl.glColor3f(hexPartColor[0], hexPartColor[1], hexPartColor[2]);        //Set color

            /**
             * If lowerArmHexPartDisplayList is already generated, we call it. Otherwise we generate and compile the list.
             */
            if (lowerArmHexPartDisplayList != 0) {
                gl.glCallList(lowerArmHexPartDisplayList);
            } else {
                lowerArmHexPartDisplayList = gl.glGenLists(1);
                gl.glNewList(lowerArmHexPartDisplayList, gl.GL_COMPILE_AND_EXECUTE);

                gl.glBegin(gl.GL_TRIANGLE_STRIP);

                //Again we calculate the vectors and normals that the loop reuses from the previous iteration i=-60:
                double angle = Math.toRadians(-60);
                double angleNext = Math.toRadians(0);

                Vector vectorTopCurToBotCur = new Vector(
                        topCircleRadius * Math.cos(angle) + topCirclePos.x() - hexPartRadius * Math.cos(angle) - hexPartPos.x(),
                        topCircleRadius * Math.sin(angle) + topCirclePos.y() - hexPartRadius * Math.sin(angle) - hexPartPos.y()
                        , topCirclePos.z() - hexPartPos.z());
                Vector vectorTopCurToBotNext = new Vector(
                        topCircleRadius * Math.cos(angleNext) + topCirclePos.x() - hexPartRadius * Math.cos(angle) - hexPartPos.x(),
                        topCircleRadius * Math.sin(angleNext) + topCirclePos.y() - hexPartRadius * Math.sin(angle) - hexPartPos.y()
                        , topCirclePos.z() - hexPartPos.z());
                Vector vectorTopNextToBotNext = new Vector(
                        topCircleRadius * Math.cos(angleNext) + topCirclePos.x() - hexPartRadius * Math.cos(angleNext) - hexPartPos.x(),
                        topCircleRadius * Math.sin(angleNext) + topCirclePos.y() - hexPartRadius * Math.sin(angleNext) - hexPartPos.y()
                        , topCirclePos.z() - hexPartPos.z());

                Vector n1, n2;
                Vector n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                Vector n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();

                for (int i = 0; i <= 360; i += 60) {
                    //Reuse angle from previous loop iteration.
                    angle = angleNext;
                    angleNext = Math.toRadians(i + 60);

                    //Reuse TopNext to BotNext vector from previous iteration as TopCur to BotCur vector. Also calculate
                    //calculate other 2 necessary vectors for normal calculation:
                    vectorTopCurToBotCur = vectorTopNextToBotNext;
                    vectorTopCurToBotNext = new Vector(
                            topCircleRadius * Math.cos(angleNext) + topCirclePos.x() - hexPartRadius * Math.cos(angle) - hexPartPos.x(),
                            topCircleRadius * Math.sin(angleNext) + topCirclePos.y() - hexPartRadius * Math.sin(angle) - hexPartPos.y()
                            , topCirclePos.z() - hexPartPos.z());
                    vectorTopNextToBotNext = new Vector(
                            topCircleRadius * Math.cos(angleNext) + topCirclePos.x() - hexPartRadius * Math.cos(angleNext) - hexPartPos.x(),
                            topCircleRadius * Math.sin(angleNext) + topCirclePos.y() - hexPartRadius * Math.sin(angleNext) - hexPartPos.y()
                            , topCirclePos.z() - hexPartPos.z());

                    //Just like before we can reuse n3 and n4 as n1 and n2 respectively. Calculate new normals using
                    //cross product keeping right hand rule in mind. Normalize.
                    n1 = n3;
                    n2 = n4;
                    n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                    n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();

                    //Add the vector n1, n2 and n3 together for the bottom normal vector, and n2, n3, n4 for the top one.
                    Vector normalVectorTopCircle = n1.add(n2.add(n3));
                    Vector normalVectorHexPart = n2.add(n3.add(n4));

                    //Use the calculated normals and draw the bottom coordinate.
                    gl.glNormal3d(normalVectorTopCircle.x(), normalVectorTopCircle.y(), normalVectorTopCircle.z());
                    gl.glVertex3d(topCircleRadius * Math.cos(angle) + topCirclePos.x(),
                            topCircleRadius * Math.sin(angle) + topCirclePos.y(),
                            topCirclePos.z());
                    //Use the calculated normal and draw the top coordinate.
                    gl.glNormal3d(normalVectorHexPart.x(), normalVectorHexPart.y(), normalVectorHexPart.z());
                    gl.glVertex3d(hexPartRadius * Math.cos(angle) + hexPartPos.x(),
                            hexPartRadius * Math.sin(angle) + hexPartPos.y(),
                            hexPartPos.z());
                }

                gl.glEnd();                 //Finish drawing *phew*.

                gl.glEndList();
            }
        }

        // draw the hand at the wrist joint
        drawHand(gl, glut);
    }


    /**
     * Draw the robot's hand.
     * The hand is drawn centered on the x-y plane hanging down from the origin
     *
     * @param gl OpenGl context
     * @param glut GLUT context
     */
    private void drawHand(GL2 gl, GLUT glut) {
        double diskHeight = 0.05;
        double diskRadius = 0.05;

        if (!gs.showStick) {
            gl.glPushMatrix();

            // The hand consists of a cylinder and the claws, start the cylinder drawing
            gl.glColor3f(handCylinderColor[0], handCylinderColor[1], handCylinderColor[2]);

            // "hang" the cylinder below the origin
            gl.glTranslated(0, 0, -diskHeight);
            glut.glutSolidCylinder(diskRadius, diskHeight, 24, 24);

            // position the claw such that it clips the the cylinder
            gl.glPushMatrix();
            gl.glColor3f(clawColor[0], clawColor[1], clawColor[2]);
            gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);

            /**
             * If clawDisplayList is already generated we call it. Otherwise we generate and compile the list.
             */
            if (clawDisplayList != 0){
                gl.glCallList(clawDisplayList);
            } else {
                clawDisplayList = gl.glGenLists(1);
                gl.glNewList(clawDisplayList, gl.GL_COMPILE_AND_EXECUTE);
                drawClaw(gl);
                gl.glEndList();
            }
            gl.glPopMatrix();

            // Draw another two claws each angled 120 degrees from one another
            gl.glPushMatrix();
            gl.glRotated(120, 0, 0, 1);
            gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);
            gl.glCallList(clawDisplayList);
            gl.glPopMatrix();

            gl.glPushMatrix();
            gl.glRotated(240, 0, 0, 1);
            gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);
            gl.glCallList(clawDisplayList);
            gl.glPopMatrix();

            gl.glPopMatrix();
        }
    }

    /**
     * The claw is drawn centered on the y-z plane on the side of the negative x-axis and down from the origin along the z-axis
     *
     * @param gl OpenGl context
     */
    private void drawClaw(GL2 gl) {
        double depth = 0.025;
        // the claw is composed of several circular segments, draw each of the segments according to the following
        // specifications and line them up to make a more complexly curved claw.
        double[] angles = {40, 15, 15, 30, 40};
        double[] radii = {0.025, 0.075, 0.05, 0.15, 0.2};
        double width = 0.05;
        // store the calculated x and z coordinates, these are reused by multiple glVertex calls
        double[][] vertices = new double[4][2];

        gl.glPushMatrix();
        // The below draw calls position the claw lying on the x-y plane. now rotate to "hang off" of the y-z plane
        gl.glRotated(-90, 0, 1, 0);
        // Scale the claw to a more appropriate size
        gl.glScaled(0.6, 0.6, 0.6);

        // Translate to position the claw centred above the origin.
        gl.glTranslated(-radii[0] - depth, 0, 0);
        // for every segment ...
        for (int angle_idx = 0; angle_idx < angles.length; angle_idx++) {

            // After the first segment line up the next segment with the previous one
            if (angle_idx != 0) {
                gl.glRotated(-angles[angle_idx - 1], 0, 1, 0);
                gl.glTranslated(radii[angle_idx - 1] - radii[angle_idx], 0, 0);
            }
            gl.glBegin(gl.GL_QUADS);

            // Split up the angle into 2 degree pieces and draw quads to realize the curvature
            for (int i = 0; i < angles[angle_idx]; i += 2) {
                // first 2D coordinate of inner circle
                vertices[0][0] = radii[angle_idx] * Math.cos(Math.toRadians(i));
                vertices[0][1] = radii[angle_idx] * Math.sin(Math.toRadians(i));
                // first coordinate of outer circle
                vertices[1][0] = (radii[angle_idx] + width) * Math.cos(Math.toRadians(i));
                vertices[1][1] = (radii[angle_idx] + width) * Math.sin(Math.toRadians(i));
                // second coordinate of inner circle
                vertices[2][0] = radii[angle_idx] * Math.cos(Math.toRadians(i + 2));
                vertices[2][1] = radii[angle_idx] * Math.sin(Math.toRadians(i + 2));
                // second coordinate of outer circle
                vertices[3][0] = (radii[angle_idx] + width) * Math.cos(Math.toRadians(i + 2));
                vertices[3][1] = (radii[angle_idx] + width) * Math.sin(Math.toRadians(i + 2));

                // front face with the normal along the y-axis
                gl.glNormal3d(0, 1, 0);
                gl.glVertex3d(vertices[0][0], depth, vertices[0][1]);
                gl.glVertex3d(vertices[1][0], depth, vertices[1][1]);
                gl.glVertex3d(vertices[3][0], depth, vertices[3][1]);
                gl.glVertex3d(vertices[2][0], depth, vertices[2][1]);
                // back face
                gl.glNormal3d(0, -1, 0);
                gl.glVertex3d(vertices[0][0], -depth, vertices[0][1]);
                gl.glVertex3d(vertices[1][0], -depth, vertices[1][1]);
                gl.glVertex3d(vertices[3][0], -depth, vertices[3][1]);
                gl.glVertex3d(vertices[2][0], -depth, vertices[2][1]);
                // inner facing side
                Util.makeFaceVertex4(gl, vertices[0][0], depth, vertices[0][1],
                        vertices[0][0], -depth, vertices[0][1],
                        vertices[2][0], -depth, vertices[2][1],
                        vertices[2][0], depth, vertices[2][1]);

                // outer facing side
                Util.makeFaceVertex4(gl, vertices[1][0], depth, vertices[1][1],
                        vertices[3][0], depth, vertices[3][1],
                        vertices[3][0], -depth, vertices[3][1],
                        vertices[1][0], -depth, vertices[1][1]);
            }

            gl.glEnd();
        }

        // close off the claw. The last coordinates in vertices are coordinates for the final piece of the final segment
        gl.glBegin(gl.GL_QUADS);
        Util.makeFaceVertex4(gl, vertices[2][0], -depth, vertices[2][1],
                vertices[3][0], -depth, vertices[3][1],
                vertices[3][0], depth, vertices[3][1],
                vertices[2][0], depth, vertices[2][1]);
        gl.glEnd();

        gl.glPopMatrix();

    }

    /**
     * Method that draws the upper leg, and then calls the lower leg method to draw itself. It assumes that the
     * coordinate (0,0,0) corresponds with the leg joint connecting the leg to the torso. It assumes that the z
     * direction is straight downwards.
     *
     * @param gl OpenGl context
     * @param glut GLUT context
     * @param side The side used to determine whether to draw the animation out of phase.
    */
    private void drawLeg(GL2 gl, GLUT glut, Side side) {
        double upperLegWidth = 0.2;
        double torsoJointRadius = 0.1;

        gl.glPushMatrix();

        // Retrieve the current angle determined for the animation of the upper leg relative to the torso.
        // The two legs are exactly half a period out of phase with each other.
        gl.glRotated(side == Side.Left ? upperLegRotate.getAngle() : upperLegRotate.getAngleHalfPhaseShift(), 1, 0, 0);

        if (gs.showStick) {
            gl.glColor3d(0, 0, 0);                            //Set color to black for stick figure.
            Util.drawSphere(gl, glut, stickSphereRadius, upperToLowerLeg);  //Draw a sphere on the knee joint using the upperToLowLeg vector.
            Util.drawLine(gl, Vector.O, upperToLowerLeg);          //Draw a line to the knee joint sphere.
        } else {
            //Set color to torsoJointColor and draw the sphere forming the joint.
            gl.glColor3f(torsoJointColor[0], torsoJointColor[1], torsoJointColor[2]);
            glut.glutSolidSphere(torsoJointRadius, 50, 50);

            //Draw the upper leg.
            gl.glColor3f(upperLegColor[0], upperLegColor[1], upperLegColor[2]);   //Set the color to upper leg color.
            gl.glPushMatrix();                                                  //Store the current matrix.
            gl.glTranslated(0, 0.0, -0.5 * upperToLowerLeg.length());           //Translate to centre of upper leg.
            gl.glScaled(upperLegWidth, upperLegWidth, upperToLowerLeg.length());//Scale coordinate system to leg size.
            glut.glutSolidCube(1);                                              //Draw a unit cube.
            gl.glPopMatrix();                                                   //Pop the matrix.
        }

        //Use upperToLowerLeg vector to translate to joint connecting upper and lower leg, then draw it.
        gl.glTranslated(upperToLowerLeg.x(), upperToLowerLeg.y(), upperToLowerLeg.z());
        drawLowerLeg(gl, glut, side);
        gl.glPopMatrix();       //Pop the matrix.
    }

    /**
     * Method that draws the lower leg, and calls the foot to draw itself. It assumes that the coordinate (0,0,0)
     * corresponds with the knee joint. It also assumes that the z direction is in the direction of the upper knee.
     *
     * @param gl OpenGl context
     * @param glut GLUT context
     * @param side The side used to determine whether to draw the animation out of phase.
     */
    private void drawLowerLeg(GL2 gl, GLUT glut, Side side) {
        double lowerLegWidth = 0.2;
        double angleFootLowerLeg = 10;
        // Retrieve the current angle determined for the animation of the lower leg relative to the upper leg.
        // The two legs are exactly half a period out of phase with each other.
        double kneeAngle = side == Side.Left ? lowerLegRotate.getAngle() : lowerLegRotate.getAngleHalfPhaseShift();

        if (gs.showStick) {
            gl.glColor3d(0, 0, 0);                    //Set color to black for stick figure.
            gl.glRotated(kneeAngle, 1, 0, 0);      //Rotate around the lowerLegAngle
            //Draw a sphere using the lowerLegToFoot vector on the joint connecting the foot and the lower leg,
            // and a line to it.
            Util.drawSphere(gl, glut, stickSphereRadius, lowerLegToFoot);
            Util.drawLine(gl, Vector.O, lowerLegToFoot);
        } else {
            gl.glPushMatrix();                      //Push the matrix.

            /*
             * Start drawing the joint connecting upper and lower leg. Translate to the rotation point of the lower
             * leg, and then rotate half the angle between lower and upper leg. Looked upon from the side the joint
             * is an isosceles triangle, where the vertex angle is the angle between the upper and lower leg, and
             * the length of the legs of the triangle is the leg-width. We make use of the makeFaceVertex function
             * to draw the shapes, which automatically sets the normals correctly, as long as we define the vertices
             * in counterclockwise fashion.
            */

            //Translate to the vertex of the isosceles triangle and rotate so that the y axis is along the median.
            gl.glTranslated(0, -0.5 * lowerLegWidth, 0);
            gl.glRotated(0.5 * kneeAngle, 1, 0, 0);

            //Set color, calculate the median and base lengths of triangle using leg width and kneeAngle between legs.
            gl.glColor3f(jointUpperLowerLegColor[0], jointUpperLowerLegColor[1], jointUpperLowerLegColor[2]);
            double medianLength = lowerLegWidth * Math.cos(Math.toRadians(0.5 * kneeAngle));
            double baseLength = 2 * lowerLegWidth * Math.sin(Math.toRadians(-0.5 * kneeAngle));
            //Draw the right side of the joint.
            gl.glBegin(gl.GL_TRIANGLES);
            Util.makeFaceVertex3(gl, 0.5 * lowerLegWidth, 0, 0,
                    0.5 * lowerLegWidth, medianLength, -0.5 * baseLength,
                    0.5 * lowerLegWidth, medianLength, 0.5 * baseLength);
            //Draw the left side of the joint.
            Util.makeFaceVertex3(gl, -0.5 * lowerLegWidth, 0, 0,
                    -0.5 * lowerLegWidth, medianLength, 0.5 * baseLength,
                    -0.5 * lowerLegWidth, medianLength, -0.5 * baseLength);
            gl.glEnd();

            //Draw the front side of the joint.
            gl.glBegin(gl.GL_QUADS);
            Util.makeFaceVertex4(gl, -0.5 * lowerLegWidth, medianLength, 0.5 * baseLength,
                    0.5 * lowerLegWidth, medianLength, 0.5 * baseLength,
                    0.5 * lowerLegWidth, medianLength, -0.5 * baseLength,
                    -0.5 * lowerLegWidth, medianLength, -0.5 * baseLength);
            gl.glEnd();

            //Now we have drawn the joint, we continue by drawing the actual lower leg itself.
            gl.glRotated(0.5 * kneeAngle, 1, 0, 0);                  //Rotate the remaining half kneeAngle.
            gl.glColor3f(lowerLegColor[0], lowerLegColor[1], lowerLegColor[2]);      //Set color to lower leg color.
            gl.glTranslated(0, 0.5 * lowerLegWidth, -0.5 * lowerLegToFoot.length());    //Translate to the centre of the lower leg.
            gl.glPushMatrix();                                                      //Store current matrix.
            gl.glScaled(lowerLegWidth, lowerLegWidth, lowerLegToFoot.length());        //Scale to the leg size.
            glut.glutSolidCube(1);                                                  //Draw a unit cube
            gl.glPopMatrix();                                                       //Return to previous matrix.

            /*
             * We draw 2 triangles and a quad as a joint connecting lower leg to foot. Seen from the side, this is
             * again a triangle, of which the bottom edge runs along the foot, and the top vertex aligns with the
             * back edge of the lower leg, of which the coordinates are defined by (x,yOffsetLowerLeg,zOffsetLowerLeg).
             * Using makeFaceVertex function so normals are done automatically, as long as we define vertices in
             * counterclockwise fashion.
            */
            gl.glTranslated(0, 0.5 * lowerLegWidth, -0.5 * lowerLegToFoot.length()); //Translate to corner of the joint.
            gl.glRotated(angleFootLowerLeg, 1, 0, 0);             //Rotate with the angle between foot and lower leg.
            //Calculate y and z coordinates of lower leg edge. Add 2mm to zOffsetLowerLeg to combat rounding errors.
            double yOffsetLowerLeg = -lowerLegWidth * Math.cos(Math.toRadians(angleFootLowerLeg));
            double zOffsetLowerLeg = lowerLegWidth * Math.sin(Math.toRadians(angleFootLowerLeg)) + 0.002;
            gl.glBegin(gl.GL_TRIANGLES);
            //Draw right side of joint
            Util.makeFaceVertex3(gl, 0.5 * lowerLegWidth, 0, 0,
                    0.5 * lowerLegWidth, yOffsetLowerLeg, zOffsetLowerLeg,
                    0.5 * lowerLegWidth, -lowerLegWidth, 0);
            //Draw left side of joint
            Util.makeFaceVertex3(gl, -0.5 * lowerLegWidth, 0, 0,
                    -0.5 * lowerLegWidth, -lowerLegWidth, 0,
                    -0.5 * lowerLegWidth, yOffsetLowerLeg, zOffsetLowerLeg);
            gl.glEnd();
            //Draw back side of joint
            gl.glBegin(gl.GL_QUADS);
            Util.makeFaceVertex4(gl, 0.5 * lowerLegWidth, -lowerLegWidth, 0,
                    0.5 * lowerLegWidth, yOffsetLowerLeg, zOffsetLowerLeg,
                    -0.5 * lowerLegWidth, yOffsetLowerLeg, zOffsetLowerLeg,
                    -0.5 * lowerLegWidth, -lowerLegWidth, 0);
            gl.glEnd();

            gl.glTranslated(0, -0.5 * lowerLegWidth, 0);        //Translate to centre of leg.
            drawFoot(gl);                                     //Draw foot.
            gl.glPopMatrix();                               //Return to previous matrix.

        }
    }

    /**
     * Method that draws the foot. It assumes that the (0,0,0) coordinate is on the joint connecting the foot
     * to the lower leg. It also assumes that the Z direction is straight upwards. The foot consists of 5 quads
     * (the top is open), together forming a box where the front side is slanted. We again use the
     * makeFaceVertex method to draw the quads, which does the normal for us, as long as we define the vertices in
     * clockwise fashion.
     */
    private void drawFoot(GL2 gl) {
        double width = 0.2;

        gl.glColor3f(footColor[0], footColor[1], footColor[2]);        //Set color to foot color.

        //Draw right side of foot
        gl.glBegin(gl.GL_QUADS);
        Util.makeFaceVertex4(gl, 0.5 * width, -0.5 * width, 0,
                0.5 * width, -0.5 * width, -0.07,
                0.5 * width, 0.65 * width, -0.07,
                0.5 * width, 0.5 * width, 0);


        //Draw left side of foot
        Util.makeFaceVertex4(gl, -0.5 * width, -0.5 * width, 0,
                -0.5 * width, 0.5 * width, 0,
                -0.5 * width, 0.65 * width, -0.07,
                -0.5 * width, -0.5 * width, -0.07);


        //Draw back of foot
        Util.makeFaceVertex4(gl, -0.5 * width, -0.5 * width, 0,
                -0.5 * width, -0.5 * width, -0.07,
                0.5 * width, -0.5 * width, -0.07,
                0.5 * width, -0.5 * width, 0);

        //Draw front of foot.
        Util.makeFaceVertex4(gl, 0.5 * width, 0.65 * width, -0.07,
                -0.5 * width, 0.65 * width, -0.07,
                -0.5 * width, 0.5 * width, 0,
                0.5 * width, 0.5 * width, 0);

        //Draw bottom of foot.
        Util.makeFaceVertex4(gl, 0.5 * width, -0.5 * width, -0.07,
                -0.5 * width, -0.5 * width, -0.07,
                -0.5 * width, 0.65 * width, -0.07,
                0.5 * width, 0.65 * width, -0.07);
        gl.glEnd();

    }

    // Draw a simple angular shoulder centered round the origin with an offset to line up with the shoulder joint of the torso.
    private void drawShoulder(GL2 gl) {
        gl.glBegin(gl.GL_TRIANGLES);
        // front face
        gl.glNormal3d(0, 1, 0);
        gl.glVertex3d(-0.05, 0.075, 0.05);
        gl.glVertex3d(0.1, 0.075, 0.05);
        gl.glVertex3d(-0.05, 0.075, -0.1);

        // back face
        gl.glNormal3d(0, -1, 0);
        gl.glVertex3d(-0.05, -0.075, 0.05);
        gl.glVertex3d(0.1, -0.075, 0.05);
        gl.glVertex3d(-0.05, -0.075, -0.1);
        gl.glEnd();

        gl.glBegin(gl.GL_QUADS);
        // top face
        gl.glNormal3d(0, 0, 1);
        gl.glVertex3d(-0.05, -0.075, 0.05);
        gl.glVertex3d(0.1, -0.075, 0.05);
        gl.glVertex3d(0.1, 0.075, 0.05);
        gl.glVertex3d(-0.05, 0.075, 0.05);

        // face connected to arm
        Util.makeFaceVertex4(gl, 0.1, -0.075, 0.05,
                -0.05, -0.075, -0.1,
                -0.05, 0.075, -0.1,
                0.1, 0.075, 0.05);
        gl.glEnd();
    }

    // Draw the main component of the robot
    private void drawTorso(GL2 gl, GLUT glut) {
        // The coordinate in the middle between the two hip joints
        Vector centerBottom = new Vector(0, 0, -0.25f);
        final double depth = 0.35 / 2;
        double spaceToShoulderJoint = 0.05;

        if (gs.showStick) {
            // Draw the several joints connected to the torso. The stick figure is black
            gl.glColor3f(0, 0, 0);
            Util.drawSphere(gl, glut, stickSphereRadius, neck);
            Util.drawSphere(gl, glut, stickSphereRadius, rightShoulder);
            Util.drawSphere(gl, glut, stickSphereRadius, leftShoulder);
            Util.drawSphere(gl, glut, stickSphereRadius, rightHip);
            Util.drawSphere(gl, glut, stickSphereRadius, leftHip);

            // Draw the lines connecting the joints
            Util.drawLine(gl, neck, rightShoulder);
            Util.drawLine(gl, neck, leftShoulder);
            Util.drawLine(gl, neck, centerBottom);
            Util.drawLine(gl, rightHip, leftHip);
        } else {
            gl.glPushMatrix();
            gl.glColor3f(torsoColor[0], torsoColor[1], torsoColor[2]);

            // draw the right shoulder
            Util.translate(gl, rightShoulder);
            drawShoulder(gl);
            Util.translate(gl, rightShoulder.scale(-1));

            // draw the left shoulder. The same as the right shoulder only now mirrored
            Util.translate(gl, leftShoulder);
            gl.glScaled(-1, 1, 1);
            drawShoulder(gl);
            gl.glScaled(-1, 1, 1);
            Util.translate(gl, leftShoulder.scale(-1));

            // Draw the main beam making up the torso. Scale according to above defined dimensions for the torso
            double width = (Math.abs(rightShoulder.x()) + Math.abs(leftShoulder.x()) - 2 * spaceToShoulderJoint) / 2;
            double height = (Math.abs(rightHip.z()) + neck.z()) / 2;

            final int FRONT = 0, BACK = 1, TOP_LEFT = 0, TOP_RIGHT = 1, BOTTOM_LEFT = 2, BOTTOM_RIGHT = 3;
            double[][][] coords = { /* coords of front face: */ {
                    {-width, depth, height,},   // Top Left
                    {width, depth, height,},    // Top Right
                    {-width, depth, -height,},  // Bottom left
                    {width, depth, -height,},   // Bottom Right
            }, {
                    /* coords of back face: */
                    {-width, -depth, height,},  // Top Left
                    {width, -depth, height,},   // Top Right
                    {-width, -depth, -height,}, // Bottom Left
                    {width, -depth, -height,},  // Bottom Right
            }
            };

            // The torso texture image is split into four square the first (top left) is the front of the torso,
            // top right is the back side, bottom left is one of the sides and bottom right is the top of the torso.
            torsoTex.enable(gl);
            torsoTex.bind(gl);
            gl.glBegin(gl.GL_QUADS);

            //Draw front side of head
            Util.setNormalVertex3(gl,
                    coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                    coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                    coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);
            gl.glTexCoord2d(0, 1.0);
            gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);
            gl.glTexCoord2d(0.5, 1.0);
            gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
            gl.glTexCoord2d(0.5, 0.5);
            gl.glVertex3d(coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);
            gl.glTexCoord2d(0, 0.5);
            gl.glVertex3d(coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);

            //Draw back side of head
            Util.setNormalVertex3(gl,
                    coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2],
                    coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2],
                    coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);

            gl.glTexCoord2d(0.5, 1.0);
            gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
            gl.glTexCoord2d(1, 1.0);
            gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
            gl.glTexCoord2d(1, 0.5);
            gl.glVertex3d(coords[BACK][BOTTOM_RIGHT][0], coords[BACK][BOTTOM_RIGHT][1], coords[BACK][BOTTOM_RIGHT][2]);
            gl.glTexCoord2d(0.5, 0.5);
            gl.glVertex3d(coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2]);

            //Draw left side of head
            Util.setNormalVertex3(gl,
                    coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                    coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2],
                    coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);

            gl.glTexCoord2d(0, 0.5);
            gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);
            gl.glTexCoord2d(0.5, 0.5);
            gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
            gl.glTexCoord2d(0.5, 0.0);
            gl.glVertex3d(coords[BACK][BOTTOM_LEFT][0], coords[BACK][BOTTOM_LEFT][1], coords[BACK][BOTTOM_LEFT][2]);
            gl.glTexCoord2d(0, 0.0);
            gl.glVertex3d(coords[FRONT][BOTTOM_LEFT][0], coords[FRONT][BOTTOM_LEFT][1], coords[FRONT][BOTTOM_LEFT][2]);

            //Draw right side of head
            Util.setNormalVertex3(gl,
                    coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                    coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2],
                    coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);

            gl.glTexCoord2d(0, 0.5);
            gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
            gl.glTexCoord2d(0.5, 0.5);
            gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
            gl.glTexCoord2d(0.5, 0.0);
            gl.glVertex3d(coords[BACK][BOTTOM_RIGHT][0], coords[BACK][BOTTOM_RIGHT][1], coords[BACK][BOTTOM_RIGHT][2]);
            gl.glTexCoord2d(0, 0.0);
            gl.glVertex3d(coords[FRONT][BOTTOM_RIGHT][0], coords[FRONT][BOTTOM_RIGHT][1], coords[FRONT][BOTTOM_RIGHT][2]);

            //Draw top of head
            Util.setNormalVertex3(gl,
                    coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2],
                    coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2],
                    coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);

            gl.glTexCoord2d(0.5, 0.5);
            gl.glVertex3d(coords[FRONT][TOP_RIGHT][0], coords[FRONT][TOP_RIGHT][1], coords[FRONT][TOP_RIGHT][2]);
            gl.glTexCoord2d(0.5, 0.0);
            gl.glVertex3d(coords[BACK][TOP_RIGHT][0], coords[BACK][TOP_RIGHT][1], coords[BACK][TOP_RIGHT][2]);
            gl.glTexCoord2d(1.0, 0.0);
            gl.glVertex3d(coords[BACK][TOP_LEFT][0], coords[BACK][TOP_LEFT][1], coords[BACK][TOP_LEFT][2]);
            gl.glTexCoord2d(1.0, 0.5);
            gl.glVertex3d(coords[FRONT][TOP_LEFT][0], coords[FRONT][TOP_LEFT][1], coords[FRONT][TOP_LEFT][2]);
            gl.glEnd();

            torsoTex.disable(gl);

            // Place a "screen" on the front face
            gl.glPushMatrix();
            gl.glColor3f(torsoScreenColor[0], torsoScreenColor[1], torsoScreenColor[2]);

            // Scale and translate relative to torso specification
            gl.glTranslated(0, depth, -0.75 * Math.abs(rightHip.z()) + neck.z());
            gl.glScaled((Math.abs(rightShoulder.x()) + Math.abs(leftShoulder.x()) - spaceToShoulderJoint) * 0.75,
                    0.02,
                    (Math.abs(rightHip.z()) + neck.z()) * 0.5);
            glut.glutSolidCube(1);
            gl.glPopMatrix();

            // Place a nob on the front face
            gl.glTranslated(0.55 * Math.abs(rightShoulder.x()), depth, 0.55 * rightHip.z());
            gl.glRotated(-90, 1, 0, 0);
            glut.glutSolidCylinder(0.02, 0.01, 10, 10);

            gl.glPopMatrix();

        }
    }

    //Sets color scheme and material properties, depending on material chosen during object construction. A base,
    //highlight and joint color are chosen depending on material, and assigned to different parts of the robot.
    void setMaterialProperties(GL2 gl) {
        float[] baseColor = new float[3];
        float[] highlightColor = new float[3];
        float[] jointColor = new float[3];

        switch (material) {
            case GOLD:
                baseColor = new float[]{0.9f, 0.9f, 0.0f};
                highlightColor = new float[]{1.0f, 0.84f, 0.0f};
                jointColor = new float[]{0.94f, 0.90f, 0.54f};
                break;
            case SILVER:
                baseColor = new float[]{0.75f, 0.75f, 0.75f};
                highlightColor = new float[]{0.83f, 0.83f, 0.83f};
                jointColor = new float[]{0.50f, 0.50f, 0.50f};
                break;
            case WOOD:
                baseColor = new float[]{0.71f, 0.61f, 0.30f};
                highlightColor = new float[]{0.51f, 0.32f, 0.0f};
                jointColor = new float[]{0.34f, 0.18f, 0.05f};
                break;
            case ORANGE: // Plastic
                baseColor = new float[]{1.0f, 0.65f, 0.0f};
                highlightColor = new float[]{1.0f, 0.55f, 0.0f};
                jointColor = new float[]{1.0f, 0.27f, 0.0f};
                break;
        }

        //Head colors
        neckColor = jointColor;
        headColor = baseColor;
        //Torso colors
        torsoColor = baseColor;
        torsoScreenColor = highlightColor;
        //Upper arm colors
        armColor = baseColor;
        elbowColor = jointColor;
        //Lower arm colors
        float[] lowerArmColor = {0.2f, 0.2f, 0.2f};
        float[] hexPartColor = {0.1f, 0.1f, 0.1f};
        //Hand colors
        handCylinderColor = highlightColor;
        //Claw colors
        clawColor = baseColor;
        //Upper leg colors
        torsoJointColor = jointColor;
        upperLegColor = baseColor;
        //Lower leg colors
        jointUpperLowerLegColor = jointColor;
        lowerLegColor = baseColor;
        footColor = highlightColor;

        //Set lighting properties depending on chosen material.
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_AMBIENT, material.ambient, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, material.specular, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_DIFFUSE, material.diffuse, 0);
        gl.glMaterialf(gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, material.shininess);
    }

    /**
     * Draws this robot (as a {@code stickfigure} if specified).
     */
    public void draw(GL2 gl, GLUT glut) {
        setMaterialProperties(gl);
        gl.glPushMatrix();

        // Start with drawing the torso (translated such that the completed robot will stand on top off the origin)
        gl.glTranslated(torsoTrans.x(), torsoTrans.y(), torsoTrans.z());
        drawTorso(gl, glut);

        // Translate the head relative to the torso
        gl.glTranslated(neck.x(), neck.y(), neck.z());
        drawHead(gl, glut);
        gl.glTranslated(-neck.x(), -neck.y(), -neck.z());

        // For the arms also apply a rotation
        gl.glPushMatrix();
        gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
        gl.glRotated(-20, 0, 1, 0);
        gl.glRotated(upperArmRotate.getAngle(), 1, 0, 0);
        drawArm(gl, glut, Side.Right);
        gl.glPopMatrix();
        // For the left arm mirror the right one
        gl.glPushMatrix();
        gl.glScaled(-1, 1, 1);
        gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
        gl.glRotated(-20, 0, 1, 0);
        gl.glRotated(upperArmRotate.getAngleHalfPhaseShift(), 1, 0, 0);
//        gl.glRotated(calcAngleBetween(upperArmAngle - upperArmAngleDelta, upperArmAngleMin + upperArmAngleDelta, upperArmAngleMin), 1, 0, 0);
        drawArm(gl, glut, Side.Left);
        gl.glPopMatrix();

        // draw the legs relative to the torso
        gl.glTranslated(rightHip.x(), rightHip.y(), rightHip.z());
        drawLeg(gl, glut, Side.Right);
        gl.glTranslated(-2 * rightHip.x(), 0, 0);
        // yet again the left limb is a mirror image of the right
        gl.glScaled(-1, 1, 1);
        drawLeg(gl, glut, Side.Left);

        gl.glPopMatrix();
    }
}

// Abstraction on rotation of a limb with a length between two specified angles.
class LimbRotation {
    private double limbLength;
    // All angles are in degrees (as that is what openGL uses).
    private double angle;
    private double minAngle;
    private double maxDelta;
    private double currentDirection = 1;

    LimbRotation(double limbLength, double minAngle, double maxAngle) {
        this.limbLength = limbLength;
        this.minAngle = minAngle;
        this.angle = minAngle;
        this.maxDelta = maxAngle - minAngle;
    }

    public double getAngle() {
        return angle;
    }

    public double getMaxDelta() {
        return maxDelta;
    }

    /**
     * Calculate and add the angle for a distance traveled on the circle's path.
     *
     * @param dist distance moved on the path
     * @return calculated angle
     */
    public double rotateDist(double dist) {
        double circlePathLength = Math.toRadians(maxDelta) * limbLength;  // arc length of segment of a circle between the angles

        double travelAngle = Math.toDegrees((dist % (2 * circlePathLength)) / limbLength);  // Multiply path by 2 for a complete cycle
        addAngle(travelAngle);
        return travelAngle;
    }

    /**
     * Calculate the angle shifted by half the period.
     * @return the shifted angle (in degrees)
     */
    public double getAngleHalfPhaseShift() {
        double shiftedAngle = angle + maxDelta;

        // The angle might have become to large
        if (shiftedAngle > minAngle + maxDelta) {
            return 2 * (minAngle + maxDelta) - shiftedAngle;
        } else if (shiftedAngle < minAngle) {  // Or the angle might have become to small
            return 2 * minAngle - shiftedAngle;
        } else {  // Or the angle might be just right.
            return shiftedAngle;
        }
    }

    /**
     * Add angle to the current position in the segment of a circle. If the angle goes beyond one of the specified angles
     * it will calculate the difference between the new angle and the allowed angle and add negative difference to the
     * allowed angle.
     *
     * @param addAngle angle to be added (in degrees)
     */
    public void addAngle(double addAngle) {
        angle += currentDirection * (addAngle % (2 * maxDelta));

        if (angle > minAngle + maxDelta) {
            angle = 2 * (minAngle + maxDelta) - angle;
            currentDirection *= -1;
        } else if (angle < minAngle) {
            angle = 2 * minAngle - angle;
            currentDirection *= -1;
        }
    }
}