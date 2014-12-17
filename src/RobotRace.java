/**
 * Computer Graphics - 2IV60 - Assignment 1
 *
 * Authors:
 * Rolf Morel
 * Noud de Kroon
 */

import static javax.media.opengl.GL2.*;
import robotrace.Base;
import robotrace.Vector;

/**
 * Handles all of the RobotRace graphics functionality,
 * which should be extended per the assignment.
 * 
 * OpenGL functionality:
 * - Basic commands are called via the gl object;
 * - Utility commands are called via the glu and
 *   glut objects;
 * 
 * GlobalState:
 * The gs object contains the GlobalState as described
 * in the assignment:
 * - The camera viewpoint angles, phi and theta, are
 *   changed interactively by holding the left mouse
 *   button and dragging;
 * - The camera view width, vWidth, is changed
 *   interactively by holding the right mouse button
 *   and dragging upwards or downwards;
 * - The center point can be moved up and down by
 *   pressing the 'q' and 'z' keys, forwards and
 *   backwards with the 'w' and 's' keys, and
 *   left and right with the 'a' and 'd' keys;
 * - Other settings are changed via the menus
 *   at the top of the screen.
 * 
 * Textures:
 * Place your "track.jpg", "brick.jpg", "head.jpg",
 * and "torso.jpg" files in the same folder as this
 * file. These will then be loaded as the texture
 * objects track, bricks, head, and torso respectively.
 * Be aware, these objects are already defined and
 * cannot be used for other purposes. The texture
 * objects can be used as follows:
 * 
 * gl.glColor3f(1f, 1f, 1f);
 * track.bind(gl);
 * gl.glBegin(GL_QUADS);
 * gl.glTexCoord2d(0, 0);
 * gl.glVertex3d(0, 0, 0);
 * gl.glTexCoord2d(1, 0);
 * gl.glVertex3d(1, 0, 0);
 * gl.glTexCoord2d(1, 1);
 * gl.glVertex3d(1, 1, 0);
 * gl.glTexCoord2d(0, 1);
 * gl.glVertex3d(0, 1, 0);
 * gl.glEnd(); 
 * 
 * Note that it is hard or impossible to texture
 * objects drawn with GLUT. Either define the
 * primitives of the object yourself (as seen
 * above) or add additional textured primitives
 * to the GLUT object.
 */
public class RobotRace extends Base {
    
    /** Array of the four robots. */
    private final Robot[] robots;
    
    /** Instance of the camera. */
    private final Camera camera;
    
    /** Instance of the race track. */
    private final RaceTrack raceTrack;
    
    /** Instance of the terrain. */
    private final Terrain terrain;

    /** Keep track of last time the scene was drawn. */
    private long lastTimeSceneDrawn;

    private long startTimeDrawing;
    
    /**
     * Constructs this robot race by initializing robots,
     * camera, track, and terrain.
     */
    public RobotRace() {
        // Initialize the race track as the basic oval track and set lanes to 4.
        raceTrack = new RaceTrack(0, 4);

        // Create a new array of four robots
        robots = new Robot[4];

        // Initialize robot 0, telling it material (from which it takes its color), on which track it is and in which
        // lane on the track.
        robots[0] = new Robot(Material.GOLD, raceTrack, 0);

        // Initialize robot 1
        robots[1] = new Robot(Material.SILVER, raceTrack, 1);

        // Initialize robot 2
        robots[2] = new Robot(Material.WOOD, raceTrack, 2);

        // Initialize robot 3
        robots[3] = new Robot(Material.ORANGE, raceTrack, 3);

        // Initialize the camera
        camera = new Camera();

        // Initialize the terrain
        terrain = new Terrain();
    }
    
    /**
     * Called upon the start of the application.
     * Primarily used to configure OpenGL.
     */
    @Override
    public void initialize() {
		
        // Enable blending.
        gl.glEnable(GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Anti-aliasing can be enabled by uncommenting the following 4 lines.
		// This can however cause problems on some graphics cards.
        //gl.glEnable(GL_LINE_SMOOTH);
        //gl.glEnable(GL_POLYGON_SMOOTH);
        //gl.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        //gl.glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        
        // Enable depth testing.
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LESS);

        // Enable lighting and light0
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_LIGHT0);
		
	    // Normalize normals.
        gl.glEnable(GL_NORMALIZE);
        
        // Converts colors to materials when lighting is enabled.
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        
        // Enable textures. 
        gl.glEnable(GL_TEXTURE_2D);
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        gl.glBindTexture(GL_TEXTURE_2D, 0);
		
	    // Try to load four textures, add more if you like.
        track = loadTexture("track.jpg");
        brick = loadTexture("brick.jpg");
        head = loadTexture("head.jpg");
        torso = loadTexture("torso.jpg");

        startTimeDrawing = System.nanoTime();
    }
    
    /**
     * Configures the viewing transform.
     */
    @Override
    public void setView() {
        // Select part of window.
        gl.glViewport(0, 0, gs.w, gs.h);
        
        // Set projection matrix.
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();

        /*
         * Set the perspective:
         * Since the field of view angle must be such that the gs.vWidth vector fills the screen, we can calculate the
         * angle of the field of view using arc-tangent and the distance to the centre point. Since the gluPerspective
         * call takes the vertical fov angle, we use the aspect ratio to calculate the height of the line we have to see
         * corresponding to a given gs.vWidth.
        */

        double fovAngley = Math.toDegrees(2 * Math.atan(gs.vWidth * ((float)gs.h / (float)gs.w) / (2 * gs.vDist)));
        glu.gluPerspective(fovAngley, (float)gs.w / (float)gs.h, 0.1*gs.vDist, 10.0*gs.vDist);

        // Set camera.
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();

        {
            // Set basic light properties of light 0
            final float[] AMBIENT = {0.1f, 0.1f, 0.1f, 1.0f};
            final float[] DIFFUSE = {1.0f, 1.0f, 1.0f, 1.0f};
            final float[] SPECULAR = {1.0f, 1.0f, 1.0f, 1.0f};
            // By positioning the light, relative to origin on the initial MODELVIEW matrix,
            // before the camera is positioned, the light will is positioned relative to the camera.
            // The positional light is set slightly to the left and slightly up (relative to the camera).
            final float[] POSITION = {-0.5f, 0.0f, 0.5f, 1.0f};

            gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, AMBIENT, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, DIFFUSE, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, SPECULAR, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, POSITION, 0);
        }
               
        // Update the view according to the camera mode
        camera.update(gs.camMode);
        glu.gluLookAt(camera.eye.x(),    camera.eye.y(),    camera.eye.z(),
                      camera.center.x(), camera.center.y(), camera.center.z(),
                      camera.up.x(),     camera.up.y(),     camera.up.z());
    }
    
    /**
     * Draws the entire scene.
     */
    @Override
    public void drawScene() {
        long currentTime = System.nanoTime();
        // TODO: might be useful, but not used for now
        long diffTimeFrames = currentTime - lastTimeSceneDrawn;

        lastTimeSceneDrawn = currentTime;

        // Background color.
        gl.glClearColor(1f, 1f, 1f, 0f);

        // Clear background.
        gl.glClear(GL_COLOR_BUFFER_BIT);
        
        // Clear depth buffer.
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        
        // Set color to black.
        gl.glColor3f(0f, 0f, 0f);
        
        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        // Draw the axis frame
        if (gs.showAxes) {
            drawAxisFrame();
        }

        gl.glColor3f(0f, 0f, 0f);

        // Draw the 4 robots.
        for (int i = 0; i < 4; i++) {
//            robots[i].incTime(diffTimeFrames);
            robots[i].drawAtPos(diffTimeFrames);
//            Vector pos = robots[i].track.getPositionOnLane(0.01 * i, i);
//            Vector tangent = robots[i].track.getTangent(0.01 * i);
//            Vector normal = robots[i].track.getNormal(0.01 * i);
//
//            gl.glPushMatrix();
//
//            translate(pos);
//            rotate(normal.cross(tangent), tangent, normal);
//            robots[i].draw();
//
//            gl.glPopMatrix();

        }

        // Draw race track
        raceTrack.draw();
        
        // Draw terrain
        terrain.draw();
    }

    // Implement drawing a orthonormal axes system with an origin and unit length arrows along the x-, y-, and z-axes.
    // Each of the arrows has its own color as has the origin.
    private class AxisSystem {
        private final double CONE_HEIGHT = 0.1f;
        private final double CONE_RADIUS = 0.1f;
        private final double BEAM_WIDTH = 0.05f;
        private final double BEAM_HEIGHT = 1 - CONE_HEIGHT;
        private final double ORIGIN_RADIUS = 0.075f;

        // Draw a simple sphere round the origin
        void drawOrigin() {
            gl.glColor3f(1.0f, 1.0f, 0.0f); // The origin is colored yellow.

            glut.glutSolidSphere(ORIGIN_RADIUS, 32, 32);
        }

        // Draw a arrow of unit (1 m) length along the z-axis
        void drawAxis() {
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
        public void draw() {
            // Draw a unit length arrow along the z-axis. The z-axis arrow is blue.
            gl.glColor3f(0, 0, 1.0f);
            drawAxis();

            // The x-axis arrow is colored red and is rotated 90 degrees along y-axis (relative to z-axis)
            gl.glColor3f(1.0f, 0, 0);
            gl.glRotated(90, 0, 1, 0);
            drawAxis();
            gl.glRotated(-90, 0, 1, 0);

            // The y-axis arrow is colored green and is rotated -90 degrees along x-axis
            gl.glColor3f(0, 1.0f, 0);
            gl.glRotated(-90, 1, 0, 0);
            drawAxis();
            gl.glRotated(90, 1, 0, 0);

            drawOrigin();
        }
    }

    /**
     * Draws the x-axis (red), y-axis (green), z-axis (blue),
     * and origin (yellow).
     */
    public void drawAxisFrame() {
        // Draw the 3 orthonormal axes, each the length of 1 unit (meter) and with their respective colors and a yellow origin.
        new AxisSystem().draw();
    }
    
    /**
     * Materials that can be used for the robots.
     */
    public enum Material {
        
        /** 
         * Gold material properties.
         * Modify the default values to make it look like gold.
         */
        GOLD (
            new float[] {0.76f, 0.61f, 0.23f, 1.0f},
            new float[] {0.63f, 0.56f, 0.37f, 1.0f},
            new float[] {0.25f, 0.20f, 0.07f, 1.0f},
            51.2f),
        
        /**
         * Silver material properties.
         * Modify the default values to make it look like silver.
         */
        SILVER (
            new float[] {0.51f, 0.51f, 0.51f, 1.0f},
            new float[] {0.51f, 0.51f, 0.51f, 1.0f},
            new float[] {0.19f, 0.19f, 0.19f, 1.0f},
            51.2f),
        
        /** 
         * Wood material properties.
         * Modify the default values to make it look like wood.
         */
        WOOD (
            new float[] {0.25f, 0.25f, 0.25f, 1f},
            new float[] {0.01f, 0.01f, 0.01f, 1.0f},
            new float[] {0.19f, 0.19f, 0.19f, 1.0f},
            0f),

        /**
         * Orange material properties.
         * Modify the default values to make it look like orange.
         */
        ORANGE (
            new float[] {0.01f, 0.01f, 0.01f, 1f},
            new float[] {0.5f, 0.5f, 0.5f, 1.0f},
            new float[] {0.1f, 0.1f, 0.1f, 1.0f},
            32f);
        
        /** The diffuse RGBA reflectance of the material. */
        float[] diffuse;
        
        /** The specular RGBA reflectance of the material. */
        float[] specular;

        /** The ambient RGBA reflectance of the material. */
        float[] ambient;

        /** The shininess RGBA reflectance of the material. */
        float shininess;
        /**
         * Constructs a new material with diffuse and specular properties.
         */
        private Material(float[] diffuse, float[] specular, float[] ambient, float shininess) {
            this.diffuse = diffuse;
            this.specular = specular;
            this.ambient = ambient;
            this.shininess = shininess;
        }
    }

    // Convenience method to draw a sphere at the position specified by POS
    private void drawSphere(double diameter, Vector pos) {
        gl.glTranslated(pos.x(), pos.y(), pos.z());
        glut.glutSolidSphere(diameter, 10, 10);
        gl.glTranslated(-pos.x(), -pos.y(), -pos.z());
    }

    // convenience method to draw a line between two specified vectors
    private void drawLine(Vector p1, Vector p2) {
        gl.glBegin(gl.GL_LINES);
        gl.glVertex3d(p1.x(), p1.y(), p1.z());
        gl.glVertex3d(p2.x(), p2.y(), p2.z());
        gl.glEnd();
    }

    // convenience method to translate to the position of tr vector
    private void translate(Vector tr) {
        gl.glTranslated(tr.x(), tr.y(), tr.z());
    }

    // convenience method to rotate the axis system to new orthonormal system specified by basis{X,Y,Z}.
    private void rotate(Vector basisX, Vector basisY, Vector basisZ) {
        // Specify the rotation matrix in row Major order.
        double[] rotMatrix = { basisX.x(), basisX.y(), basisX.z(), 0,
                               basisY.x(), basisY.y(), basisY.z(), 0,
                               basisZ.x(), basisZ.y(), basisZ.z(), 0,
                               0,          0,          0,          1 };

        gl.glMultMatrixd(rotMatrix, 0);
    }

    // convenience method to rotate the axis system from a orthonormal system specified by basis{X,Y,Z}.
    private void undoRotate(Vector basisX, Vector basisY, Vector basisZ) {
        double[] rotMatrix = { basisX.x(), basisY.x(), basisZ.x(), 0,
                               basisX.y(), basisY.y(), basisZ.y(), 0,
                               basisX.z(), basisY.z(), basisZ.z(), 0,
                               0,          0,          0,          1 };

        gl.glMultMatrixd(rotMatrix, 0);
    }

    // Method to calculate and set the normal for a face defined by 3 vertices
    private void makeFaceVertex3(double v1x, double v1y, double v1z,
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
    private void makeFaceVertex4(double v1x, double v1y, double v1z,
                                 double v2x, double v2y, double v2z,
                                 double v3x, double v3y, double v3z,
                                 double v4x, double v4y, double v4z) {
        makeFaceVertex3(v1x, v1y, v1z,
                        v2x, v2y, v2z,
                        v3x, v3y, v3z);

        gl.glVertex3d(v4x, v4y, v4z);
    }

    /**
     * Represents a Robot, to be implemented according to the Assignments.
     */
    private class Robot {
        // Keep a reference to the track the robot is on.
        private RaceTrack track;
        // Maintain in which lane the robot is running.
        private int trackLane;

        private long startTime = System.nanoTime();

        double stickSphereRadius = 0.033f;

        // Translation for to position the center of the robot on top of the xy-plane
        Vector torsoTrans = new Vector(0, 0, 0.75f);

        // Coordinates for limbs relative to center of robot's torso
        Vector neck = new Vector(0, 0, 0.25f);
        Vector rightShoulder = new Vector(0.3f, 0, 0.2f);
        Vector leftShoulder = new Vector(-0.3f, 0, 0.2f);
        Vector rightHip = new Vector(0.15f, 0, -0.25f);
        Vector leftHip = new Vector(-0.15f, 0, -0.25f);

        // Coordinates for limbs relative to other limbs
        Vector upperToLowerArm = new Vector(0, 0, -0.275);
        Vector lowerArmToHand = new Vector(0, 0, -0.25);

        Vector upperToLowerLeg = new Vector(0, 0, -0.19);
        Vector lowerLegToFoot = new Vector(0, 0, -0.19);

        // Angles between limbs
        double lowerLegAngle = -25;
        double upperLegAngle = 15;

        float[] lowerArmRotate = { 20 , 1, 1, 0 };  // Rotation for glRotate. This used by drawArm to rotate the lower arm

        //Head colors
        float[] neckColor;
        float[] headColor;
        float[] scleraColor = {1,1,1};
        float[] irisColor = {0,0,0};
        float[] mouthColor = {0,0,0};
        //Torso colors
        float[] torsoColor;
        float[] torsoScreenColor;
        //Upper arm colors
        float[] armColor;
        float[] elbowColor;
        //Lower arm colors
        float[] lowerArmColor = { 0.2f, 0.2f, 0.2f };
        float[] hexPartColor = { 0.1f, 0.1f, 0.1f };
        //Hand colors
        float[] handCylinderColor;
        //Claw colors
        float[] clawColor;
        //Upper leg colors
        float[] torsoJointColor;
        float[] upperLegColor;
        //Lower leg colors
        float[] jointUpperLowerLegColor;
        float[] lowerLegColor;
        float[] footColor;

        /** The material from which this robot is built. */
        private final Material material;


        /**
         * Constructs the robot with initial parameters.
         */
        public Robot(Material material, RaceTrack track, int trackLane) {
            this.material = material;
            this.track = track;
            this.trackLane = trackLane;
        }

        public void incTime(long timeDiff) {
            // TODO: move according to time difference.
        }

        public void drawAtPos(long timeDiff) {
            double sinceStart = System.nanoTime() - startTime;

            double t = (1 / (15 * 10e9)) * (sinceStart % (15 * 10e9));
            // FIXME: hack to show diffent speeds
            t *= (trackLane + 1);

            Vector pos = track.getPositionOnLane(t, trackLane);
            Vector tangent = track.getTangent(t);
            Vector normal = track.getNormal(t);

            gl.glPushMatrix();

            translate(pos);
            rotate(normal.cross(tangent), tangent, normal);
            draw();

            gl.glPopMatrix();
        }

        //Method that draws head model. This method assumes that (0,0,0) coordinate is the joint connecting the head
        //to the body. You can easily change the variables defined at the start of the method to reshape the head.
        private void drawHead() {
            double headWidth = 1.0;
            double headHeight = 0.5;
            double headDepth = 0.25;
            double neckSize = 0.10;
            double eyeRadius=0.12;
            double eyeDepth = 0.02;
            double irisDepth = 0.01;
            double antennaSize = 0.1;
            double neckAngle = 10;

            if (gs.showStick) {                                         //If gs.showStick is true, draw only stick-figure.
                gl.glColor3f(0, 0, 0);                                  //Stick-figure is always black.
                gl.glTranslated(0, 0, 0.5*neckSize+0.5*headHeight);     //Translate up, to about the centre of the head
                glut.glutSolidSphere(stickSphereRadius, 10, 10);           //Draw a stickSphere-radius sphere
                gl.glTranslated(0, 0, -(0.5*neckSize+0.5*headHeight));  //Translate back to the joint connecting neck and body

                //Draw a line from the base of the head to the stick figure sphere in the centre of the head.
                gl.glBegin(gl.GL_LINES);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, 0, 0.5*neckSize+0.5*headHeight);
                gl.glEnd();

            } else {

                //Draw the neck
                gl.glColor3d(neckColor[0], neckColor[1], neckColor[2]);         //Set the color to the neck color.
                gl.glPushMatrix();                                              //Push new matrix
                gl.glRotated(-neckAngle, 1, 0, 0);                      //Rotate around x axis with appropiate angle
                gl.glTranslated(0, 0, -0.05);           //Translate a bit downwards so entire cylinder is inside body
                //Draw cylinder with appropiate size. We make it a bit bigger than necksize, so there is no open space.
                glut.glutSolidCylinder(0.75*neckSize, 1.5 * neckSize, 30, 1);
                gl.glPopMatrix();                                               //Restore to original matrix

                /* Draw the head. The head is built from 6 quads, together forming a figure like a frustum. We use the
                method makeFaceVertex4 to draw the quads, which automatically does the normal vectors, as long as we
                make sure to define the vertices in a counterclockwise fashion (otherwise normal is inverted). */
                gl.glColor3f(headColor[0], headColor[1], headColor[2]);         //Set color to color of head.
                gl.glBegin(GL_QUADS);                                           //Start drawing quads.
                //Draw front side of head
                makeFaceVertex4(0.5 * headWidth, headDepth, 0.5 * neckSize,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,
                        0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight);
                //Draw back side of head
                makeFaceVertex4(0.3 * headWidth, -headDepth, neckSize,
                        0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,
                        -0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,
                        -0.3 * headWidth, -headDepth, neckSize);


                //Draw right side of head
                makeFaceVertex4(0.5 * headWidth, headDepth, 0.5 * neckSize,
                        0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,
                        0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,
                        0.3 * headWidth, -headDepth, neckSize);

                //Draw left side of head
                makeFaceVertex4(-0.5 * headWidth, headDepth, 0.5 * neckSize,
                        -0.3 * headWidth, -headDepth, neckSize,
                        -0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight);

                //Draw top of head
                makeFaceVertex4(0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize + headHeight,
                        -0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight,
                        0.3 * headWidth, -headDepth, neckSize + 0.8 * headHeight);

                //draw bottom of head
                makeFaceVertex4(0.5 * headWidth, headDepth, 0.5 * neckSize,
                        0.3 * headWidth, -headDepth, neckSize,
                        -0.3 * headWidth, -headDepth, neckSize,
                        -0.5 * headWidth, headDepth, 0.5 * neckSize);

                gl.glEnd();             //We are done drawing quads.

                //Draw antenna.
                gl.glPushMatrix();                  //Push a new matrix.
                gl.glTranslated(-0.25*headWidth,-0.25*headDepth,neckSize+0.8*headHeight); //Translate to location of antenna.
                glut.glutSolidCone(0.2*antennaSize,antennaSize,20,20);      //Draw a solidcone, using antennasize.
                gl.glTranslated(0,0,antennaSize);                           //Translate to top of antenna.
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

                //Draw mouth
                gl.glPushMatrix();              //Push a new matrix to store current position.
                gl.glColor3f(mouthColor[0], mouthColor[1], mouthColor[2]);          //Set color to mouth color.
                gl.glTranslated(0, headDepth + 0.005, 0.5 * neckSize + 0.3 * headHeight);   //Translate to mouth position.

                //The mouth is a polygon of which the top side is a straight line, and the bottom side are points on a circle.
                gl.glBegin(GL_POLYGON);             //Begin drawing a polygon.
                gl.glNormal3d(0,1,0);               //Set normal vector
                //Draw the top side of the mouth.
                gl.glVertex3d(-0.15 * headWidth, 0, 0);
                gl.glVertex3d(0.15 * headWidth, 0, 0);
                //Calculate the centre coordinate and radius of the circle of which the bottom of the mouth is a subsection.
                double zOffsetCentre = Math.sin(Math.toRadians(15))*0.15*headWidth;
                double radius = Math.sqrt(Math.pow(zOffsetCentre,2)+Math.pow(0.15*headWidth,2));
                //In a loop we generate 50 points on the circle, together forming the bottom of the mouth.
                for (int i = 0; i < 50; i++) {
                    gl.glVertex3d(
                            Math.sin(Math.toRadians(75-3*i))*radius,
                            0,
                            zOffsetCentre-Math.cos(Math.toRadians(75-3*i))*radius);
                }
                gl.glEnd();                     //We are finished generating points for the polygon.
                gl.glPopMatrix();               //Return to original matrix.
            }
        }

        // Draw the robot's arm.
        // The arm is drawn with its shoulder joint centered on the x-y plane hanging down from the origin
        private void drawArm() {
            double armRadius = 0.05;
            double elbowRadius = 0.055;

            if (gs.showStick) {
                // The stick figure arm is a black line from the shoulder joint to a black sphere at the elbow joint
                gl.glColor3f(0, 0, 0);
                drawSphere(stickSphereRadius, upperToLowerArm);
                drawLine(Vector.O, upperToLowerArm);
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
            translate(upperToLowerArm);
            gl.glRotated(lowerArmRotate[0], lowerArmRotate[1], lowerArmRotate[2], lowerArmRotate[3]);
            drawLowerArm();
            gl.glRotated(-lowerArmRotate[0], lowerArmRotate[1], lowerArmRotate[2], lowerArmRotate[3]);
            translate(upperToLowerArm.scale(-1));
        }

        // Draw the robot's lower arm.
        // The lower arm is drawn centered on the x-y plane hanging down from the origin
        private void drawLowerArm() {
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
                drawSphere(stickSphereRadius, lowerArmToHand);
                drawLine(Vector.O, lowerArmToHand);
            } else {
                // translate to make the lower arm "hang" from the origin
                gl.glTranslated(lowerArmToHand.x(), lowerArmToHand.y(), lowerArmToHand.z());

                gl.glColor3f(lowerArmColor[0], lowerArmColor[1], lowerArmColor[2]);

                // draw the circle which closes of the top of the "cut-off" cone
                gl.glBegin(gl.GL_TRIANGLE_FAN);

                // the normal for the top face is directed along z-axis
                gl.glNormal3d(0, 0, 1);

                // Initial vertex is the center of the circle
                gl.glVertex3d(topCirclePos.x(), topCirclePos.y(), topCirclePos.z());

                // for every degree draw a triangle from the center to the edge of the circle
                for (int i = 0; i <= 360; i++) {
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

                for (int i = 0; i <= 360; i++) {
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
                double angle = Math.toRadians(-1);
                double angleNext = Math.toRadians(0);
                Vector vectorTopCurToBotCur = new Vector(
                        bottomCircleRadius*Math.cos(angle) + bottomCirclePos.x() - topCircleRadius*Math.cos(angle) - topCirclePos.x(),
                        bottomCircleRadius*Math.sin(angle) + bottomCirclePos.y() - topCircleRadius*Math.sin(angle) - topCirclePos.y()
                        ,bottomCirclePos.z() - topCirclePos.z());
                Vector vectorTopCurToBotNext = new Vector(
                        bottomCircleRadius*Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius*Math.cos(angle) - topCirclePos.x(),
                        bottomCircleRadius*Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius*Math.sin(angle) - topCirclePos.y()
                        ,bottomCirclePos.z() - topCirclePos.z());
                Vector vectorTopNextToBotNext = new Vector(
                        bottomCircleRadius*Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius*Math.cos(angleNext) - topCirclePos.x(),
                        bottomCircleRadius*Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius*Math.sin(angleNext) - topCirclePos.y()
                        ,bottomCirclePos.z() - topCirclePos.z());

                Vector n1;
                Vector n2;
                Vector n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                Vector n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();

                for (int i = 0; i <= 360; i++) {
                    // calculate the angle only once for every degree:
                    angle = angleNext;
                    angleNext = Math.toRadians(i+1);

                    //What was in the previous loop the vector from TopNext to BotNext, is in iteration of the loop the
                    //vector between TopCur and BotCur, so we can pass it on. Also calculate the other 2 necessary vectors:
                    vectorTopCurToBotCur = vectorTopNextToBotNext;
                    vectorTopCurToBotNext = new Vector(
                            bottomCircleRadius*Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius*Math.cos(angle) - topCirclePos.x(),
                            bottomCircleRadius*Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius*Math.sin(angle) - topCirclePos.y()
                            ,bottomCirclePos.z() - topCirclePos.z());
                    vectorTopNextToBotNext = new Vector(
                            bottomCircleRadius*Math.cos(angleNext) + bottomCirclePos.x() - topCircleRadius*Math.cos(angleNext) - topCirclePos.x(),
                            bottomCircleRadius*Math.sin(angleNext) + bottomCirclePos.y() - topCircleRadius*Math.sin(angleNext) - topCirclePos.y()
                            ,bottomCirclePos.z() - topCirclePos.z());


                    /*
                    What was in the previous iteration of the loop n3 is now n1, and what was n4 is now n2, so we pass
                    on these values. Also calculate the new normals n3 and n4 using cross product, taking the right
                    hand rule in mind. Normalize these vectors.
                    */
                    n1=n3;
                    n2=n4;
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

                /* Now we draw the "hex part", connecting the lower arm to the elbow joint. The technique we use for
                * this is exactly the same as for the lower arm cone, so we refer to that for clarification. Now, the
                * top circle is on the elbow joint, and the bottom circle is the top of the lower arm cone (the coordinates
                * of this circle are called topCircle, which might be confusing). We use 60 degree steps, so we have 12
                * triangles total.
                */
                gl.glColor3f(hexPartColor[0], hexPartColor[1], hexPartColor[2]);        //Set color
                gl.glBegin(gl.GL_TRIANGLE_STRIP);

                //Again we calculate the vectors and normals that the loop reuses from the previous iteration i=-60:
                angle = Math.toRadians(-60);
                angleNext = Math.toRadians(0);

                vectorTopCurToBotCur = new Vector(
                        topCircleRadius * Math.cos(angle) + topCirclePos.x() - hexPartRadius * Math.cos(angle) - hexPartPos.x(),
                        topCircleRadius * Math.sin(angle) + topCirclePos.y() - hexPartRadius * Math.sin(angle) - hexPartPos.y()
                        ,topCirclePos.z() - hexPartPos.z());
                vectorTopCurToBotNext = new Vector(
                        topCircleRadius*Math.cos(angleNext) + topCirclePos.x() - hexPartRadius*Math.cos(angle) - hexPartPos.x(),
                        topCircleRadius*Math.sin(angleNext) + topCirclePos.y() - hexPartRadius*Math.sin(angle) - hexPartPos.y()
                        ,topCirclePos.z() - hexPartPos.z());
                vectorTopNextToBotNext = new Vector(
                        topCircleRadius*Math.cos(angleNext) + topCirclePos.x() - hexPartRadius*Math.cos(angleNext) - hexPartPos.x(),
                        topCircleRadius*Math.sin(angleNext) + topCirclePos.y() - hexPartRadius*Math.sin(angleNext) - hexPartPos.y()
                        ,topCirclePos.z() - hexPartPos.z());

                n3 = vectorTopCurToBotCur.cross(vectorTopCurToBotNext).normalized();
                n4 = vectorTopNextToBotNext.cross(vectorTopCurToBotNext).normalized();

                for (int i = 0; i <= 360; i += 60) {
                    //Reuse angle from previous loop iteration.
                    angle = angleNext;
                    angleNext = Math.toRadians(i+60);

                    //Reuse TopNext to BotNext vector from previous iteration as TopCur to BotCur vector. Also calculate
                    //calculate other 2 necessary vectors for normal calculation:
                    vectorTopCurToBotCur = vectorTopNextToBotNext;
                    vectorTopCurToBotNext = new Vector(
                            topCircleRadius*Math.cos(angleNext) + topCirclePos.x() - hexPartRadius*Math.cos(angle) - hexPartPos.x(),
                            topCircleRadius*Math.sin(angleNext) + topCirclePos.y() - hexPartRadius*Math.sin(angle) - hexPartPos.y()
                            ,topCirclePos.z() - hexPartPos.z());
                    vectorTopNextToBotNext = new Vector(
                            topCircleRadius*Math.cos(angleNext) + topCirclePos.x() - hexPartRadius*Math.cos(angleNext) - hexPartPos.x(),
                            topCircleRadius*Math.sin(angleNext) + topCirclePos.y() - hexPartRadius*Math.sin(angleNext) - hexPartPos.y()
                            ,topCirclePos.z() - hexPartPos.z());

                    //Just like before we can reuse n3 and n4 as n1 and n2 respectively. Calculate new normals using
                    //cross product keeping right hand rule in mind. Normalize.
                    n1=n3;
                    n2=n4;
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
            }

            // draw the hand at the wrist joint
            drawHand();
        }

        // Draw the robot's hand.
        // The hand is drawn centered on the x-y plane hanging down from the origin
        private void drawHand() {
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
                gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);
                drawClaw();
                gl.glPopMatrix();

                // Draw another two claws each angled 120 degrees from one another
                gl.glPushMatrix();
                gl.glRotated(120, 0, 0, 1);
                gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);
                drawClaw();
                gl.glPopMatrix();

                gl.glPushMatrix();
                gl.glRotated(240, 0, 0, 1);
                gl.glTranslated(-diskRadius + 0.005, 0, 0.5 * diskHeight);
                drawClaw();
                gl.glPopMatrix();

                gl.glPopMatrix();
            }
        }

        // The claw is drawn centered on the y-z plane on the side of the negative x-axis and down from the origin along the z-axis
        private void drawClaw() {
            double depth = 0.025;
            // the claw is composed of several circular segments, draw each of the segments according to the following
            // specifications and line the up to make more complex curved claw
            double[] angles = { 40, 15, 15, 30, 40 };
            double[] radii = { 0.025, 0.075, 0.05, 0.15, 0.2 };
            double width = 0.05;
            // store the calculated x and z coordinates, these are reused by multiple glVertex calls
            double[][] vertices = new double[4][2];

            gl.glPushMatrix();
            gl.glColor3f(clawColor[0], clawColor[1], clawColor[2]);

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
                    makeFaceVertex4(vertices[0][0], depth, vertices[0][1],
                                    vertices[0][0], -depth, vertices[0][1],
                                    vertices[2][0], -depth, vertices[2][1],
                                    vertices[2][0], depth, vertices[2][1]);

                    // outer facing side
                    makeFaceVertex4(vertices[1][0], depth, vertices[1][1],
                                    vertices[3][0], depth, vertices[3][1],
                                    vertices[3][0], -depth, vertices[3][1],
                                    vertices[1][0], -depth, vertices[1][1]);
                }

                gl.glEnd();
            }

            // close off the claw. The last coordinates in vertices are coordinates for the final piece of the final segment
            gl.glBegin(gl.GL_QUADS);
            makeFaceVertex4(vertices[2][0], -depth, vertices[2][1],
                            vertices[3][0], -depth, vertices[3][1],
                            vertices[3][0], depth, vertices[3][1],
                            vertices[2][0], depth, vertices[2][1]);
            gl.glEnd();

            gl.glPopMatrix();

        }

        /*
         * Method that draws the upper leg, and then calls the lower leg method to draw itself. It assumes that the
         * coordinate (0,0,0) corresponds with the leg joint connecting the leg to the torso. It assumes that the z
         * direction is straight downwards.
        */
        private void drawLeg() {
            double upperLegWidth = 0.2;
            double torsoJointRadius = 0.1;

            gl.glPushMatrix();
            gl.glRotated(upperLegAngle,1,0,0);                  //Rotate around the upperLegAngle

            if (gs.showStick) {
                gl.glColor3d(0,0,0);                            //Set color to black for stick figure.
                drawSphere(stickSphereRadius, upperToLowerLeg);  //Draw a sphere on the knee joint using the upperToLowLeg vector.
                drawLine(Vector.O, upperToLowerLeg);          //Draw a line to the knee joint sphere.
            } else {
                //Set color to torsoJointColor and draw the sphere forming the joint.
                gl.glColor3f(torsoJointColor[0], torsoJointColor[1], torsoJointColor[2]);
                glut.glutSolidSphere(torsoJointRadius, 50, 50);

                //Draw the upper leg.
                gl.glColor3f(upperLegColor[0],upperLegColor[1],upperLegColor[2]);   //Set the color to upper leg color.
                gl.glPushMatrix();                                                  //Store the current matrix.
                gl.glTranslated(0, 0.0, -0.5 * upperToLowerLeg.length());           //Translate to centre of upper leg.
                gl.glScaled(upperLegWidth, upperLegWidth, upperToLowerLeg.length());//Scale coordinate system to leg size.
                glut.glutSolidCube(1);                                              //Draw a unit cube.
                gl.glPopMatrix();                                                   //Pop the matrix.
            }

            //Use upperToLowerLeg vector to translate to joint connecting upper and lower leg, then draw it.
            gl.glTranslated(upperToLowerLeg.x(), upperToLowerLeg.y(), upperToLowerLeg.z());
            drawLowerLeg();
            gl.glPopMatrix();       //Pop the matrix.
        }

        //Method that draws the lower leg, and calls the foot to draw itself. It assums that the coordinate (0,0,0)
        //corresponds with the knee joint. It also assumes that the z direction is in the direction of the upper knee.
        private void drawLowerLeg() {
            double lowerLegWidth = 0.2;
            double angleFootLowerLeg = 10;

            if (gs.showStick) {
                gl.glColor3d(0, 0, 0);                    //Set color to black for stick figure.
                gl.glRotated(lowerLegAngle,1,0,0);      //Rotate around the lowerLegAngle
                //Draw a sphere using the lowerLegToFoot vector on the joint connecting the foot and the lower leg,
                // and a line to it.
                drawSphere(stickSphereRadius, lowerLegToFoot);
                drawLine(Vector.O, lowerLegToFoot);
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
                gl.glTranslated(0,-0.5*lowerLegWidth,0);
                gl.glRotated(0.5*lowerLegAngle,1,0,0);

                //Set color, calculate the median and base lengths of triangle using leg width and angle between legs.
                gl.glColor3f(jointUpperLowerLegColor[0], jointUpperLowerLegColor[1], jointUpperLowerLegColor[2]);
                double medianLength = lowerLegWidth*Math.cos(Math.toRadians(0.5*lowerLegAngle));
                double baseLength = 2*lowerLegWidth*Math.sin(Math.toRadians(-0.5*lowerLegAngle));
                //Draw the right side of the joint.
                gl.glBegin(GL_TRIANGLES);
                makeFaceVertex3(0.5*lowerLegWidth,0,0,
                        0.5*lowerLegWidth, medianLength, -0.5*baseLength,
                        0.5*lowerLegWidth, medianLength, 0.5*baseLength);
                //Draw the left side of the joint.
                makeFaceVertex3(-0.5*lowerLegWidth,0,0,
                        -0.5*lowerLegWidth, medianLength, 0.5*baseLength,
                        -0.5*lowerLegWidth, medianLength, -0.5*baseLength);
                gl.glEnd();

                //Draw the front side of the joint.
                gl.glBegin(GL_QUADS);
                makeFaceVertex4(-0.5*lowerLegWidth, medianLength, 0.5*baseLength,
                        0.5*lowerLegWidth, medianLength, 0.5*baseLength,
                        0.5*lowerLegWidth, medianLength, -0.5*baseLength,
                        -0.5*lowerLegWidth, medianLength, -0.5*baseLength);
                gl.glEnd();

                //Now we have drawn the joint, we continue by drawing the actual lower leg itself.
                gl.glRotated(0.5*lowerLegAngle,1,0,0);                  //Rotate the remaining half angle.
                gl.glColor3f(lowerLegColor[0],lowerLegColor[1],lowerLegColor[2]);      //Set color to lower leg color.
                gl.glTranslated(0, 0.5*lowerLegWidth, -0.5 * lowerLegToFoot.length());    //Translate to the centre of the lower leg.
                gl.glPushMatrix();                                                      //Store current matrix.
                gl.glScaled(lowerLegWidth, lowerLegWidth, lowerLegToFoot.length());        //Scale to the leg size.
                glut.glutSolidCube(1);                                                  //Draw a unit cube
                gl.glPopMatrix();                                                       //Return to previous matrix.

                /*
                We draw 2 triangles and a quad as a joint connecting lower leg to foot. Seen from the side, this is
                again a triangle, of which the bottom edge runs along the foot, and the top vertex aligns with the
                back edge of the lower leg, of which the coordinates are defined by (x,yOffsetLowerLeg,zOffsetLowerLeg).
                Using makeFaceVertex function so normals are done automatically, as long as we define vertices in
                counterclockwise fashion.
                */
                gl.glTranslated(0, 0.5 * lowerLegWidth, -0.5 * lowerLegToFoot.length()); //Translate to corner of the joint.
                gl.glRotated(angleFootLowerLeg, 1, 0, 0);             //Rotate with the angle between foot and lower leg.
                //Calculate y and z coordinates of lower leg edge. Add 2mm to zOffsetLowerLeg to combat rounding errors.
                double yOffsetLowerLeg = -lowerLegWidth*Math.cos(Math.toRadians(angleFootLowerLeg));
                double zOffsetLowerLeg = lowerLegWidth*Math.sin(Math.toRadians(angleFootLowerLeg))+0.002;
                gl.glBegin(GL_TRIANGLES);
                //Draw right side of joint
                makeFaceVertex3(0.5*lowerLegWidth,0,0,
                        0.5*lowerLegWidth,yOffsetLowerLeg, zOffsetLowerLeg,
                        0.5*lowerLegWidth,-lowerLegWidth,0);
                //Draw left side of joint
                makeFaceVertex3(-0.5*lowerLegWidth,0,0,
                        -0.5*lowerLegWidth,-lowerLegWidth,0,
                        -0.5*lowerLegWidth,yOffsetLowerLeg, zOffsetLowerLeg);
                gl.glEnd();
                //Draw back side of joint
                gl.glBegin(GL_QUADS);
                makeFaceVertex4(0.5*lowerLegWidth,-lowerLegWidth,0,
                        0.5*lowerLegWidth,yOffsetLowerLeg, zOffsetLowerLeg,
                        -0.5*lowerLegWidth,yOffsetLowerLeg, zOffsetLowerLeg,
                        -0.5*lowerLegWidth,-lowerLegWidth,0);
                gl.glEnd();

                gl.glTranslated(0,-0.5*lowerLegWidth,0);        //Translate to centre of leg.
                drawFoot();                                     //Draw foot.
                gl.glPopMatrix();                               //Return to previous matrix.

            }
        }

        /*
         * Method that draws the foot. It assumes that the (0,0,0) coordinate is on the joint connecting the foot
         * to the lower leg. It also assumes that the Z direction is straight upwards. The foot consists of 5 quads
         * (the top is open), together forming a box where the front side is slanted. We again use the
         * makeFaceVertex method to draw the quads, which does the normal for us, as long as we define the vertices in
         * clockwise fashion.
        */
        private void drawFoot() {
            double width = 0.2;

            gl.glColor3f(footColor[0],footColor[1],footColor[2]);        //Set color to foot color.

            //Draw right side of foot
            gl.glBegin(GL_QUADS);
            makeFaceVertex4(0.5 * width, -0.5*width, 0,
                    0.5 *width,-0.5*width,-0.07,
                    0.5 * width,0.65*width,-0.07,
                    0.5 * width,0.5*width,0);


            //Draw left side of foot
            makeFaceVertex4(-0.5 * width, -0.5*width, 0,
                    -0.5 * width,0.5*width,0,
                    -0.5 * width,0.65*width,-0.07,
                    -0.5 *width,-0.5*width,-0.07);


            //Draw back of foot
            makeFaceVertex4(-0.5 * width, -0.5*width, 0,
                    -0.5 *width,-0.5*width,-0.07,
                    0.5 *width,-0.5*width,-0.07,
                    0.5 * width, -0.5*width, 0);

            //Draw front of foot.
            makeFaceVertex4(0.5 * width,0.65*width,-0.07,
                    -0.5 * width,0.65*width,-0.07,
                    -0.5 * width,0.5*width,0,
                    0.5 * width,0.5*width,0);

            //Draw bottom of foot.
            makeFaceVertex4(0.5 *width,-0.5*width,-0.07,
                    -0.5 *width,-0.5*width,-0.07,
                    -0.5 * width,0.65*width,-0.07,
                    0.5 * width,0.65*width,-0.07);
            gl.glEnd();

        }

        // Draw a simple angular shoulder centered round the origin with an offset to line up with the shoulder joint of the torso.
        private void drawShoulder() {
            {
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
                makeFaceVertex4(0.1, -0.075, 0.05,
                                -0.05, -0.075, -0.1,
                                -0.05, 0.075, -0.1,
                                0.1, 0.075, 0.05);
                gl.glEnd();

            }
        }

        // Draw the main component of the robot
        private void drawTorso() {
            // The coordinate in the middle of the between the two hip joints
            Vector centerBottom = new Vector(0, 0, -0.25f);
            double depth = 0.35;
            double spaceToShoulderJoint = 0.05;

            if (gs.showStick) {
                // Draw the several joints connected to the torso. The stick figure is black
                gl.glColor3f(0, 0, 0);
                drawSphere(stickSphereRadius, neck);
                drawSphere(stickSphereRadius, rightShoulder);
                drawSphere(stickSphereRadius, leftShoulder);
                drawSphere(stickSphereRadius, rightHip);
                drawSphere(stickSphereRadius, leftHip);

                // Draw the lines connecting the joints
                drawLine(neck, rightShoulder);
                drawLine(neck, leftShoulder);
                drawLine(neck, centerBottom);
                drawLine(rightHip, leftHip);
            } else {
                gl.glPushMatrix();
                gl.glColor3f(torsoColor[0], torsoColor[1], torsoColor[2]);

                // draw the right shoulder
                translate(rightShoulder);
                drawShoulder();
                translate(rightShoulder.scale(-1));

                // draw the left shoulder. The same as the right shoulder only now mirrored
                translate(leftShoulder);
                gl.glScaled(-1, 1, 1);
                drawShoulder();
                gl.glScaled(-1, 1, 1);
                translate(leftShoulder.scale(-1));

                // Draw the main beam making up the torso. Scale according to above defined dimensions for the torso
                gl.glPushMatrix();
                gl.glScaled(Math.abs(rightShoulder.x()) + Math.abs(leftShoulder.x()) - spaceToShoulderJoint,
                                     depth,
                                     Math.abs(rightHip.z()) + neck.z());
                glut.glutSolidCube(1);
                gl.glPopMatrix();

                // Place a "screen" on the front face
                gl.glPushMatrix();
                gl.glColor3f(torsoScreenColor[0], torsoScreenColor[1], torsoScreenColor[2]);

                // Scale and translate relative to torso specification
                gl.glTranslated(0, 0.55 * depth, -0.75 * Math.abs(rightHip.z()) + neck.z());
                gl.glScaled((Math.abs(rightShoulder.x()) + Math.abs(leftShoulder.x()) - spaceToShoulderJoint) * 0.75,
                            0.02,
                            (Math.abs(rightHip.z()) + neck.z()) * 0.5);
                glut.glutSolidCube(1);
                gl.glPopMatrix();

                // Place a nob on the front face
                gl.glTranslated(0.55 * Math.abs(rightShoulder.x()), 0.5 * depth, 0.55 * rightHip.z());
                gl.glRotated(-90, 1, 0, 0);
                glut.glutSolidCylinder(0.02, 0.01, 10, 10);

                gl.glPopMatrix();

            }
        }

        //Sets color scheme and material properties, depending on material chosen during object construction. A base,
        //highlight and joint color are chosen depending on material, and assigned to different parts of the robot.
        void setMaterialProperties() {
            float[] baseColor = new float[3];
            float[] highlightColor = new float[3];
            float[] jointColor = new float[3];

            switch (material) {
                case GOLD:
                    baseColor = new float[]{0.9f, 0.9f, 0.0f};
                    highlightColor = new float[]{ 1.0f, 0.84f, 0.0f };
                    jointColor = new float[]{ 0.94f, 0.90f, 0.54f };
                    break;
                case SILVER:
                    baseColor = new float[]{0.75f, 0.75f, 0.75f};
                    highlightColor = new float[]{ 0.83f, 0.83f, 0.83f };
                    jointColor = new float[]{ 0.50f, 0.50f, 0.50f };
                    break;
                case WOOD:
                    baseColor = new float[]{0.71f, 0.61f, 0.30f};
                    highlightColor = new float[]{ 0.51f, 0.32f, 0.0f };
                    jointColor = new float[]{ 0.34f, 0.18f, 0.05f };
                    break;
                case ORANGE: // Plastic
                    baseColor = new float[]{1.0f, 0.65f, 0.0f};
                    highlightColor = new float[]{ 1.0f, 0.55f, 0.0f };
                    jointColor = new float[]{ 1.0f, 0.27f, 0.0f };
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
            float[] lowerArmColor = { 0.2f, 0.2f, 0.2f };
            float[] hexPartColor = { 0.1f, 0.1f, 0.1f };
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
        public void draw() {

            setMaterialProperties();
            gl.glPushMatrix();


            // Start with drawing the torso (translated such that the completed robot will stand on top off the origin)
            gl.glTranslated(torsoTrans.x(), torsoTrans.y(), torsoTrans.z());
            drawTorso();

            // Translate the head relative to the torso
            gl.glTranslated(neck.x(), neck.y(), neck.z());
            drawHead();
            gl.glTranslated(-neck.x(), -neck.y(), -neck.z());

            // For the arms also apply a rotation
            gl.glPushMatrix();
            gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
            gl.glRotated(-20, 0, 1, 0);
            drawArm();
            gl.glPopMatrix();
            // For the left arm mirror the right one
            gl.glPushMatrix();
            gl.glScaled(-1, 1, 1);
            gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
            gl.glRotated(-20, 0, 1, 0);
            drawArm();
            gl.glPopMatrix();

            // draw the legs relative to the torso
            gl.glTranslated(rightHip.x(), rightHip.y(), rightHip.z());
            drawLeg();
            gl.glTranslated(-2 * rightHip.x(), 0, 0);
            // yet again the left limb is a mirror image of the right
            gl.glScaled(-1, 1, 1);
            drawLeg();

            gl.glPopMatrix();
        }
    }
    
    /**
     * Implementation of a camera with a position and orientation. 
     */
    private class Camera {
        
        /** The position of the camera. */
        public Vector eye = new Vector(3f, 6f, 5f);
        
        /** The point to which the camera is looking. */
        public Vector center = Vector.O;
        
        /** The up vector. */
        public Vector up = Vector.Z;
        
        /**
         * Updates the camera viewpoint and direction based on the
         * selected camera mode.
         */
        public void update(int mode) {
            robots[0].toString();
            
            // Helicopter mode
            if (1 == mode) {  
                setHelicopterMode();
                
            // Motor cycle mode
            } else if (2 == mode) { 
                setMotorCycleMode();
                
            // First person mode
            } else if (3 == mode) { 
                setFirstPersonMode();
                
            // Auto mode
            } else if (4 == mode) { 
                // code goes here...
                
            // Default mode
            } else {
                setDefaultMode();
            }
        }
        
        /**
         * Computes {@code eye}, {@code center}, and {@code up}, based
         * on the camera's default mode.
         */
        private void setDefaultMode() {
            center = gs.cnt;

            //Calculate the X, Y and Z coordinates of eye using spherical coordinates.
            double eyeX, eyeY, eyeZ;

            eyeX = Math.cos(gs.theta) * Math.cos(gs.phi) *  gs.vDist;
            eyeY = Math.sin(gs.theta) * Math.cos(gs.phi) *  gs.vDist;
            eyeZ = Math.sin(gs.phi) * gs.vDist;

            //Calculate the eyeDisplacement vector (which is a vector pointing from the centre point to the eye),
            //and add it to centre point to obtain the eye position vector relative to origin.
            Vector eyeDisplacement = new Vector(eyeX, eyeY, eyeZ);
            eye = center.add(eyeDisplacement);
        }
        
        /**
         * Computes {@code eye}, {@code center}, and {@code up}, based
         * on the helicopter mode.
         */
        private void setHelicopterMode() {
            // code goes here ...
        }
        
        /**
         * Computes {@code eye}, {@code center}, and {@code up}, based
         * on the motorcycle mode.
         */
        private void setMotorCycleMode() {
            // code goes here ...
        }
        
        /**
         * Computes {@code eye}, {@code center}, and {@code up}, based
         * on the first person mode.
         */
        private void setFirstPersonMode() {
            // code goes here ...
        }
        
    }
    
    /**
     * Implementation of a race track that is made from Bezier segments.
     */
    private class RaceTrack {
        // Dimension of the basic oval track specified in the assignment.
        final double ovalTrackCosRadius = 10;
        final double ovalTrackSinRadius = 14;
        double trackWidth = 4;
        double trackHeight = 2;

        /** Keep track of which track this instance is to draw. */
        private int trackNr;

        /** Maintain number of available lanes to walk on. */
        private int trackLanes;

        /** Array with control points for the O-track. */
        private Vector[] controlPointsOTrack;
        
        /** Array with control points for the L-track. */
        private Vector[] controlPointsLTrack;
        
        /** Array with control points for the C-track. */
        private Vector[] controlPointsCTrack;
        
        /** Array with control points for the custom track. */
        private Vector[] controlPointsCustomTrack;
        
        /**
         * Constructs the race track, sets up display lists.
         */
        public RaceTrack(int trackNr, int trackLanes) {
            this.trackNr = trackNr;
            this.trackLanes = trackLanes;
        }

        // Specify (after instantiation) which of the tracks to use. TRACKNR specifies tracks from 0 to TODO: max track nr.
        public void setTrackNr(int trackNr) {
            // TODO: when adding display_list clear them when changing track nrs.
            this.trackNr = trackNr;
        }

        void drawSimpleTop(boolean normalUp) {
            final double dt = 0.01;
            final double dw = 0.25;
            final double widthFromCenter = 0.5 * trackWidth;

            // normal along Z
            gl.glNormal3d(0, 0, normalUp ? 1 : -1);

            Vector center = getPoint(0);
            Vector prevToOrigin = Vector.Z.cross(getTangent(0)).normalized();
            Vector prevInnerMost = center.add(prevToOrigin.scale(widthFromCenter));
            for (double t = dt; t <= 1.0d + dt; t += dt) {
                center = getPoint(t);
                Vector toOrigin = Vector.Z.cross(getTangent(t)).normalized();
                Vector innerMostPoint = center.add(toOrigin.scale(widthFromCenter));

                gl.glBegin(gl.GL_TRIANGLE_STRIP);
                for (double w = 0; w <= 1.0d; w += dw) {
                    Vector innerPoint0 = prevInnerMost.add(prevToOrigin.scale(-trackWidth * w));
//                    Vector outerPoint0 = innerPoint0.add(toOrigin.scale(-dw));
                    Vector innerPoint1 = innerMostPoint.add(toOrigin.scale(-trackWidth * w));
//                    Vector outerPoint1 = innerPoint1.add(toOrigin.scale(-dw));

                    gl.glVertex3d(innerPoint0.x(), innerPoint0.y(), innerPoint0.z());
//                    gl.glVertex3d(outerPoint0.x(), outerPoint0.y(), outerPoint0.z());

                    gl.glVertex3d(innerPoint1.x(), innerPoint1.y(), innerPoint1.z());
//                    gl.glVertex3d(outerPoint1.x(), outerPoint1.y(), outerPoint1.z());

                }

                gl.glEnd();

                prevInnerMost = innerMostPoint;
                prevToOrigin = toOrigin;
            }
        }

        /**
         * Draws this track, based on the selected track number.
         */

        public void drawTopandBottom() {
            double dt = 0.01;
            double trackWidth = 4;
            Vector centreToInnerNext = Vector.Z.cross(getTangent(0)).normalized();
            Vector nextPoint = getPoint(0);
            Vector innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth / 2.0));
            Vector outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth / 2.0));
            for (double t = 0; t < 1; t = t + dt) {
                Vector centreToInner = centreToInnerNext;
                centreToInnerNext = Vector.Z.cross(getTangent(t+dt)).normalized();
                Vector currentPoint = nextPoint;
                nextPoint = getPoint(t+dt);

                Vector innerRingPosCurrent = innerRingPosNext;
                innerRingPosNext = nextPoint.add(centreToInnerNext.scale(trackWidth/2.0));
                gl.glBegin(GL_TRIANGLE_STRIP);
                for (double z = -1; z<1; z = z+0.1) {

                    gl.glNormal3d(centreToInner.x(), centreToInner.y(), centreToInner.z());
                    gl.glVertex3d(innerRingPosCurrent.x(),innerRingPosCurrent.y(),z);

                    gl.glNormal3d(centreToInnerNext.x(),centreToInnerNext.y(),centreToInnerNext.z());
                    gl.glVertex3d(innerRingPosNext.x(), innerRingPosNext.y(), z);
                }
                gl.glEnd();

                Vector outerRingPosCurrent = outerRingPosNext;
                outerRingPosNext = nextPoint.add(centreToInnerNext.scale(-trackWidth/2.0));
                gl.glBegin(GL_TRIANGLE_STRIP);
                for (double z = -1; z<1; z = z+0.1) {

                    gl.glNormal3d(-centreToInner.x(),-centreToInner.y(),-centreToInner.z());
                    gl.glVertex3d(outerRingPosCurrent.x(),outerRingPosCurrent.y(),z);

                    gl.glNormal3d(-centreToInnerNext.x(),-centreToInnerNext.y(),-centreToInnerNext.z());
                    gl.glVertex3d(outerRingPosNext.x(), outerRingPosNext.y(), z);
                }
                gl.glEnd();
            }

        }

        public void draw() {
            
            // The test track is selected
            if (0 == trackNr) {
                drawTopandBottom();
                gl.glTranslated(0, 0, 1);
                drawSimpleTop(true);
                gl.glTranslated(0, 0, -2);
                drawSimpleTop(false);
                // code goes here ...
            
            // The O-track is selected
            } else if (1 == trackNr) {
                // code goes here ...
                
            // The L-track is selected
            } else if (2 == trackNr) {
                // code goes here ...
                
            // The C-track is selected
            } else if (3 == trackNr) {
                // code goes here ...
                
            // The custom track is selected
            } else if (4 == trackNr) {
                // code goes here ...
                
            }
        }
        
        /**
         * Returns the position of the curve at 0 <= {@code t} <= 1.
         */
        public Vector getPoint(double t) {
            if (0 == trackNr) {
                // A basic 2D (z=0) outline of oval shape.
                double x = ovalTrackCosRadius * Math.cos(Math.PI * 2 * t);
                double y = ovalTrackSinRadius * Math.sin(Math.PI * 2 * t);

                return new Vector(x, y, 0);
            } else if (1 == trackNr) {
                // code goes here ...

                // The L-track is selected
            } else if (2 == trackNr) {
                // code goes here ...

                // The C-track is selected
            } else if (3 == trackNr) {
                // code goes here ...

                // The custom track is selected
            } else if (4 == trackNr) {
                // code goes here ...

            }

            return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
        }

        /**
         * Calculate the point at the center of a walkable lane.
         *
         * @param laneNr number of the lane. Must be non-negative and smaller than this.trackLanes.
         * @return Point of center of the lane
         */
        public Vector getPositionOnLane(double t, int laneNr) {
            assert 0 <= laneNr && laneNr < trackLanes;

            // If (single track || middle track of uneven number of tracks) {
            if (trackLanes == 1 || ((trackLanes % 2 == 1) && laneNr == (trackLanes + 1) / 2)) {
                // This lane is centered on the track, getPoint already returns the center of the track.
                return getPoint(t);
            }

            // By taking the cross product of the tangent and the normal we get a vector along the surface from
            // the center of the track toward the outer bound of it.
            // No need to normalize, the tangent and normal are orthogonal and unit length.
            Vector normal = getNormal(t);
            Vector alongSurface = getTangent(t).cross(normal);

            double halfLaneWidth = trackWidth / (2 * trackLanes);
            double halfLanesFromCenter;
            if (laneNr < (trackLanes / 2)) {
                // we need an inner lane thus the vector must be reversed to point inward.
                alongSurface = alongSurface.scale((-1));

                halfLanesFromCenter = trackLanes - laneNr * 2 - 1;
            } else {
                halfLanesFromCenter = laneNr * 2 - trackLanes + 1;
            }

            // Get the center point add the displacement along the width and add displacement along the height of the track.
            return getPoint(t).add(alongSurface.scale(halfLaneWidth * halfLanesFromCenter))
                              .add(normal.scale(trackHeight / 2));
        }
        
        /**
         * Returns the normalized tangent of the curve at 0 <= {@code t} <= 1.
         */
        public Vector getTangent(double t) {
            if (0 == trackNr) {
                // A tangent to a 2D outline of oval shape. The below functions are obtained by taking
                // dx/dt and dy/dt of the function in getPoint.
                double x = -2 * Math.PI * ovalTrackCosRadius * Math.sin(2 * Math.PI * t);
                double y = 2 * Math.PI * ovalTrackSinRadius * Math.cos(2 * Math.PI * t);

                return new Vector(x, y, 0).normalized();
            } else if (1 == trackNr) {
                // code goes here ...

                // The L-track is selected
            } else if (2 == trackNr) {
                // code goes here ...

                // The C-track is selected
            } else if (3 == trackNr) {
                // code goes here ...

                // The custom track is selected
            } else if (4 == trackNr) {
                // code goes here ...

            }

            return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
        }

        /**
         * Returns the (normalized) normal to the current track.
         */
        public Vector getNormal(double t) {
            if (0 == trackNr) {
                // The surface of the basic oval track is always coplanar with the XOY plane therefore the normal is
                // along the z-axis.
                return Vector.Z;
            } else if (1 == trackNr) {
                // code goes here ...

                // The L-track is selected
            } else if (2 == trackNr) {
                // code goes here ...

                // The C-track is selected
            } else if (3 == trackNr) {
                // code goes here ...

                // The custom track is selected
            } else if (4 == trackNr) {
                // code goes here ...

            }

            return null; // As long as the invariant 0 <= trackNr <= 4 holds, this is unreachable.  TODO: set proper max track nr
        }
    }
    
    /**
     * Implementation of the terrain.
     */
    private class Terrain {
        
        /**
         * Can be used to set up a display list.
         */
        public Terrain() {
            // code goes here ...
        }
        
        /**
         * Draws the terrain.
         */
        public void draw() {
            // code goes here ...
        }
        
        /**
         * Computes the elevation of the terrain at ({@code x}, {@code y}).
         */
        public float heightAt(float x, float y) {
            return 0; // <- code goes here
        }
    }
    
    /**
     * Main program execution body, delegates to an instance of
     * the RobotRace implementation.
     */
    public static void main(String args[]) {
        RobotRace robotRace = new RobotRace();
        robotRace.run();
    }
    
}
