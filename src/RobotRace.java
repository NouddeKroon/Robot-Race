
import javax.media.opengl.GL;
import static javax.media.opengl.GL2.*;
import robotrace.Base;
import robotrace.Texture1D;
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
    
    /**
     * Constructs this robot race by initializing robots,
     * camera, track, and terrain.
     */
    public RobotRace() {

        // Create a new array of four robots
        robots = new Robot[4];

        // Initialize robot 0
        robots[0] = new Robot(Material.GOLD
            /* add other parameters that characterize this robot */);

        // Initialize robot 1
        robots[1] = new Robot(Material.SILVER
            /* add other parameters that characterize this robot */);

        // Initialize robot 2
        robots[2] = new Robot(Material.WOOD
            /* add other parameters that characterize this robot */);

        // Initialize robot 3
        robots[3] = new Robot(Material.ORANGE
            /* add other parameters that characterize this robot */);

        // Initialize the camera
        camera = new Camera();

        // Initialize the race track
        raceTrack = new RaceTrack();

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

        // Set the perspective.
        // Modify this to meet the requirements in the assignment.
        //System.out.println(gs.vWidth + " " + gs.vDist + " " + gs.phi + " " + gs.theta);
        double fovAngle = Math.toDegrees(2 * Math.atan(gs.vWidth / (2 * gs.vDist)));
//        System.out.println(fovAngle+ " " + gs.vWidth + " " + gs.vDist);
        glu.gluPerspective(fovAngle, (float)gs.w / (float)gs.h, 0.1*gs.vDist, 10.0*gs.vDist);
        
        // Set camera.
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
               
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

        // Draw the first robot
        robots[0].draw();
        
        // Draw race track
        raceTrack.draw(gs.trackNr);
        
        // Draw terrain
        terrain.draw();

        // Unit box around origin.
        glut.glutWireCube(1f);

        // Move in x-direction.
        gl.glTranslatef(2f, 0f, 0f);
        
        // Rotate 30 degrees, around z-axis.
        gl.glRotatef(30f, 0f, 0f, 1f);
        
        // Scale in z-direction.
        gl.glScalef(1f, 1f, 2f);

        // Translated, rotated, scaled box.
        glut.glutWireCube(1f);

    }
    
    
    /**
     * Draws the x-axis (red), y-axis (green), z-axis (blue),
     * and origin (yellow).
     */

    private enum Axis3D {
        X_AXIS,
        Y_AXIS,
        Z_AXIS;
    }

    private class Axis {
        private Vector color;
        private Vector trans;
        private Vector scale;
        private Vector coneRotate;
        private double angle;

        public Axis(Axis3D axis) {
            switch (axis) {
                case X_AXIS:
                    color = new Vector(1.0f, 0f, 0f);
                    trans = new Vector(0.5f, 0f, 0f);
                    scale = new Vector(1.0f, 0.1f, 0.1f);
                    coneRotate = new Vector(0f, 1.0f, 0f);
                    angle = 90;
                    break;
                case Y_AXIS:
                    color = new Vector(0f, 1.0f, 0f);
                    trans = new Vector(0f, 0.5f, 0f);
                    scale = new Vector(0.1f, 1.0f, 0.1f);
                    coneRotate = new Vector(1.0f, 0f, 0f);
                    angle = -90;
                    break;
                case Z_AXIS:
                    color = new Vector(0f, 0f, 1.0f);
                    trans = new Vector(0f, 0f, 0.5f);
                    scale = new Vector(0.1f, 0.1f, 1.0f);
                    coneRotate = new Vector(0f, 0f, 0f);
                    angle = 90;
                    break;
            }
        }

        public void draw() {
            gl.glPushMatrix();
            gl.glColor3d(color.x(), color.y(), color.z());

            gl.glTranslated(trans.x(), trans.y(), trans.z());

            gl.glPushMatrix();
            gl.glScaled(scale.x(), scale.y(), scale.z());
            glut.glutSolidCube(1.0f);
            gl.glPopMatrix();

            gl.glTranslated(trans.x(), trans.y(), trans.z());
            gl.glRotated(angle, coneRotate.x(), coneRotate.y(), coneRotate.z());
            glut.glutSolidCone(0.2d, 0.2d, 64, 64);
            gl.glPopMatrix();
        }
    }

    private class Origin {
        private final float[] COLOR = { 1.0f, 1.0f, 0.0f };
        private final double RADIUS = 0.2f;

        public void draw() {
            gl.glPushMatrix();
            gl.glColor3f(COLOR[0], COLOR[1], COLOR[2]);

            glut.glutSolidSphere(RADIUS, 10, 10);

            gl.glPopMatrix();
        }
    }

    public void drawAxisFrame() {
        // Draw the 3 autonormal axis, each normalized and with their own color.
        new Axis(Axis3D.X_AXIS).draw();
        new Axis(Axis3D.Y_AXIS).draw();
        new Axis(Axis3D.Z_AXIS).draw();

        // Draw yellow sphere centred at origin.
        new Origin().draw();
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
            new float[] {0.8f, 0.8f, 0.8f, 1.0f},
            new float[] {0.0f, 0.0f, 0.0f, 1.0f}),
        
        /**
         * Silver material properties.
         * Modify the default values to make it look like silver.
         */
        SILVER (
            new float[] {0.8f, 0.8f, 0.8f, 1.0f},
            new float[] {0.0f, 0.0f, 0.0f, 1.0f}),
        
        /** 
         * Wood material properties.
         * Modify the default values to make it look like wood.
         */
        WOOD (
            new float[] {0.8f, 0.8f, 0.8f, 1.0f},
            new float[] {0.0f, 0.0f, 0.0f, 1.0f}),
        
        /**
         * Orange material properties.
         * Modify the default values to make it look like orange.
         */
        ORANGE (
            new float[] {0.8f, 0.8f, 0.8f, 1.0f},
            new float[] {0.0f, 0.0f, 0.0f, 1.0f});
        
        /** The diffuse RGBA reflectance of the material. */
        float[] diffuse;
        
        /** The specular RGBA reflectance of the material. */
        float[] specular;
        
        /**
         * Constructs a new material with diffuse and specular properties.
         */
        private Material(float[] diffuse, float[] specular) {
            this.diffuse = diffuse;
            this.specular = specular;
        }
    }
    
    /**
     * Represents a Robot, to be implemented according to the Assignments.
     */
    private class Robot {
        double stickSphereRad = 0.033f;
        Vector neck = new Vector(0, 0, 0.25f);
        Vector rightShoulder = new Vector(-0.25f, 0, 0.2f);
        Vector leftShoulder = new Vector(0.25f, 0, 0.2f);
        Vector rightHip = new Vector(-0.15f, 0, -0.25f);
        Vector leftHip = new Vector(0.15f, 0, -0.25f);

        Vector torsoTrans = new Vector(0, 0, 0.75f);

        Vector upperToLowerArm = new Vector(0, 0, -0.35);
        Vector lowerToHand = new Vector(0, 0, -0.25);

        Vector upperToLowerLeg = new Vector(0, 0, -0.35);
        Vector lowerToFoot = new Vector(0, 0, -0.25);

        /** The material from which this robot is built. */
        private final Material material;

        private void drawSphere(double diameter, Vector pos) {
            gl.glTranslated(pos.x(), pos.y(), pos.z());
            glut.glutSolidSphere(diameter, 10, 10);
            gl.glTranslated(-pos.x(), -pos.y(), -pos.z());
        }

        private void drawLine(Vector p1, Vector p2) {
            gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(p1.x(), p1.y(), p1.z());
            gl.glVertex3d(p2.x(), p2.y(), p2.z());
            gl.glEnd();
        }
        /**
         * Constructs the robot with initial parameters.
         */
        public Robot(Material material
            /* add other parameters that characterize this robot */) {
            this.material = material;
            
            // code goes here ...
        }

        private void drawHead() {
            if (gs.showStick) {
                gl.glTranslated(0, 0, 0.4d);
                glut.glutSolidSphere(stickSphereRad, 10, 10);

                gl.glTranslated(0, 0, -0.4d);

                gl.glBegin(gl.GL_LINES);
                gl.glVertex3f(0, 0, 0);
                gl.glVertex3f(0, 0, 0.4f);
                gl.glEnd();

            }
        }

        private void drawArm() {
            if (gs.showStick) {
                drawSphere(stickSphereRad, upperToLowerArm);
                drawLine(Vector.O, upperToLowerArm);
            }
            gl.glTranslated(upperToLowerArm.x(), upperToLowerArm.y(), upperToLowerArm.z());
            gl.glRotated(-10, 0, 1, 0);
            drawLowerArm();
            gl.glRotated(10, 0, 1, 0);
            gl.glTranslated(-upperToLowerArm.x(), -upperToLowerArm.y(), -upperToLowerArm.z());
        }

        private void drawLowerArm() {
            if (gs.showStick) {
                drawSphere(stickSphereRad, lowerToHand);
                drawLine(Vector.O, lowerToHand);
            }
        }

        private void drawLeg() {
            if (gs.showStick) {
                drawSphere(stickSphereRad, upperToLowerLeg);
                drawLine(Vector.O, upperToLowerLeg);
            }
            gl.glTranslated(upperToLowerLeg.x(), upperToLowerLeg.y(), upperToLowerLeg.z());
            gl.glRotated(-10, 0, 1, 0);
            drawLowerLeg();
            gl.glRotated(10, 0, 1, 0);
            gl.glTranslated(-upperToLowerLeg.x(), -upperToLowerLeg.y(), -upperToLowerLeg.z());
        }

        private void drawLowerLeg() {
            if (gs.showStick) {
                drawSphere(stickSphereRad, lowerToFoot);
                drawLine(Vector.O, lowerToFoot);
            }
        }

        private void drawTorso() {
            Vector centerBottom = new Vector(0, 0, -0.25f);

            if (gs.showStick) {
                drawSphere(stickSphereRad, neck);
                drawSphere(stickSphereRad, rightShoulder);
                drawSphere(stickSphereRad, leftShoulder);
                drawSphere(stickSphereRad, rightHip);
                drawSphere(stickSphereRad, leftHip);

                drawLine(neck, rightShoulder);
                drawLine(neck, leftShoulder);
                drawLine(neck, centerBottom);
                drawLine(rightHip, leftHip);
            }
        }



        /**
         * Draws this robot (as a {@code stickfigure} if specified).
         */
        public void draw() {
            gl.glPushMatrix();
            gl.glColor3f(0, 0, 0);

            gl.glTranslated(torsoTrans.x(), torsoTrans.y(), torsoTrans.z());
            drawTorso();

            gl.glTranslated(neck.x(), neck.y(), neck.z());
            drawHead();
            gl.glTranslated(-neck.x(), -neck.y(), -neck.z());

            gl.glPushMatrix();
            gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
            gl.glRotated(20, 0, 1, 0);
            drawArm();
            gl.glPopMatrix();
            gl.glPushMatrix();
            gl.glScaled(-1, 1, 1);
            gl.glTranslated(rightShoulder.x(), rightShoulder.y(), rightShoulder.z());
            gl.glRotated(20, 0, 1, 0);
            drawArm();
            gl.glPopMatrix();

            gl.glPushMatrix();
            gl.glTranslated(rightHip.x(), rightHip.y(), rightHip.z());
            gl.glRotated(20, 0, 1, 0);
            drawLeg();
            gl.glPopMatrix();
            gl.glPushMatrix();
            gl.glScaled(-1, 1, 1);
            gl.glTranslated(rightHip.x(), rightHip.y(), rightHip.z());
            gl.glRotated(20, 0, 1, 0);
            drawLeg();
            gl.glPopMatrix();

//
//            gl.glScaled(-1, 1, 1);
//            drawArm();


//
//            drawLegs();
//
//            drawArms();
            gl.glPopMatrix();

            // code goes here ...
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

            double eyeX, eyeY, eyeZ;

            eyeX = Math.cos(gs.theta) * Math.cos(gs.phi) *  gs.vDist;
            eyeY = Math.sin(gs.theta) * Math.cos(gs.phi) *  gs.vDist;
            eyeZ = Math.sin(gs.phi) * gs.vDist;

            Vector eyeDisplacement = new Vector(eyeX, eyeY, eyeZ);

            eye = center.add(eyeDisplacement);

            // code goes here ...
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
        public RaceTrack() {
            // code goes here ...
        }
        
        /**
         * Draws this track, based on the selected track number.
         */
        public void draw(int trackNr) {
            
            // The test track is selected
            if (0 == trackNr) {
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
            return Vector.O; // <- code goes here
        }
        
        /**
         * Returns the tangent of the curve at 0 <= {@code t} <= 1.
         */
        public Vector getTangent(double t) {
            return Vector.O; // <- code goes here
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
