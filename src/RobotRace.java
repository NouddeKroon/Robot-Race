/**
 * Computer Graphics - 2IV60 - Assignment 1
 *
 * Authors:
 * Rolf Morel
 * Noud de Kroon
 */

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import robotrace.Base;
import robotrace.Vector;

import java.io.File;
import java.io.IOException;

import static javax.media.opengl.GL2.*;

/**
 * Handles all of the RobotRace graphics functionality,
 * which should be extended per the assignment.
 * <p/>
 * OpenGL functionality:
 * - Basic commands are called via the gl object;
 * - Utility commands are called via the glu and
 * glut objects;
 * <p/>
 * GlobalState:
 * The gs object contains the GlobalState as described
 * in the assignment:
 * - The camera viewpoint angles, phi and theta, are
 * changed interactively by holding the left mouse
 * button and dragging;
 * - The camera view width, vWidth, is changed
 * interactively by holding the right mouse button
 * and dragging upwards or downwards;
 * - The center point can be moved up and down by
 * pressing the 'q' and 'z' keys, forwards and
 * backwards with the 'w' and 's' keys, and
 * left and right with the 'a' and 'd' keys;
 * - Other settings are changed via the menus
 * at the top of the screen.
 * <p/>
 * Textures:
 * Place your "track.jpg", "brick.jpg", "head.jpg",
 * and "torso.jpg" files in the same folder as this
 * file. These will then be loaded as the texture
 * objects track, bricks, head, and torso respectively.
 * Be aware, these objects are already defined and
 * cannot be used for other purposes. The texture
 * objects can be used as follows:
 * <p/>
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
 * <p/>
 * Note that it is hard or impossible to texture
 * objects drawn with GLUT. Either define the
 * primitives of the object yourself (as seen
 * above) or add additional textured primitives
 * to the GLUT object.
 */
public class RobotRace extends Base {

    /**
     * Array of the four robots.
     */
    private final Robot[] robots;

    /**
     * Instance of the camera.
     */
    private final Camera camera;

    int displayList;
    boolean robotsInitialized;

    /**
     * Instance of the race track.
     */
    private final RaceTrack raceTrack;

    /**
     * Instance of the terrain.
     */
    private final Terrain terrain;

    /**
     * Keep track of last time the scene was drawn.
     */
    private long lastTimeSceneDrawn = 0;

    /**
     * Hold the delta time of the start of drawing of the current frame and the start of drawing of the previous frame.
     *
     * At the very start of setView the difference is calculated and is use by the camera and by the animation of
     * the robots.
     */

    private Texture landscape;

    /**
     * Constructs this robot race by initializing robots,
     * camera, track, and terrain.
     */
    public RobotRace() {
        // Initialize the race track as the basic oval track and set lanes to 4.
        raceTrack = new RaceTrack(0, gs);

        // Create a new array of four robots
        robots = new Robot[4];

        // Initialize robot 0, telling it material (from which it takes its color), on which track it is and in which
        // lane on the track.
        robots[0] = new Robot(Material.GOLD, raceTrack, 0, gs);

        // Initialize robot 1
        robots[1] = new Robot(Material.SILVER, raceTrack, 1, gs);

        // Initialize robot 2
        robots[2] = new Robot(Material.WOOD, raceTrack, 2, gs);

        // Initialize robot 3
        robots[3] = new Robot(Material.ORANGE, raceTrack, 3, gs);

        // Initialize the camera
        camera = new Camera(gs, robots);
        camera.update(0);

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
//        TestTrack.brick = brick;
        head = loadTexture("head.jpg");
        torso = loadTexture("torso.jpg");

        Robot.headTex = head;
        Robot.torsoTex = torso;
        Track.brick = brick;
        Track.track = track;

//        startTimeDrawing = System.nanoTime();
        lastTimeSceneDrawn = System.nanoTime();

        //Try to load the terrain colour texture, give it to the terrain object.
        landscape = loadTexture("terrainTexture.jpg");
        terrain.setTexture(landscape);

        displayList = gl.glGenLists(1);
    }

    /**
     * Configures the viewing transform.
     */
    @Override
    public void setView() {
        // Calculate the difference in time since the previous moment we were here.
        long currentTime = System.nanoTime();
        long diffTimeFrames = currentTime - lastTimeSceneDrawn;
        lastTimeSceneDrawn = currentTime;

        for (int i = 0; i < 4; i++) {
            robots[i].updatePos(diffTimeFrames);
        }

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

        double fovAngley = Math.toDegrees(2 * Math.atan(gs.vWidth * ((float) gs.h / (float) gs.w) / (2 * gs.vDist)));
        glu.gluPerspective(fovAngley, (float) gs.w / (float) gs.h, 0.1 * gs.vDist, 10.0 * gs.vDist);

        // Let the camera check if camMode changed and change its mode accordingly.
        camera.setCamMode(gs.camMode);
        // The animation should be as smooth as possible and should not depend on the framerate, therefore use the time
        // delta between the frames to calculated the animations.
        camera.update(diffTimeFrames);

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
        glu.gluLookAt(camera.eye.x(), camera.eye.y(), camera.eye.z(),
                camera.center.x(), camera.center.y(), camera.center.z(),
                camera.up.x(), camera.up.y(), camera.up.z());
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

        // Draw race track
        raceTrack.draw(gl);

        // Draw the 4 robots.
        if (!robotsInitialized) {
            for (int i = 0; i < 4; i++) {
                robots[i].drawAtPos(gl, glut);
            }
            robotsInitialized = true;
        } else {
            gl.glNewList(displayList, GL_COMPILE_AND_EXECUTE);
            for (int i = 0; i < 4; i++) {
                robots[i].drawAtPos(gl, glut);
            }
            gl.glEndList();
        }

        // Draw terrain
        terrain.draw(gl);

        drawPictureInPicture();
    }

    /**
     * Draws the x-axis (red), y-axis (green), z-axis (blue),
     * and origin (yellow).
     */
    public void drawAxisFrame() {
        // Draw the 3 orthonormal axes, each the length of 1 unit (meter) and with their respective colors and a yellow origin.
        new AxisSystem().draw(gl, glut);
    }


    //Method drawing picture-in-picture, which is a static camera floating above the map.
    private void drawPictureInPicture(){
        //Define a new, square viewport, in the top-right corner, with a width that is 1/3th of the smallest of the length
        //and height of the window.
        int width = Math.min(gs.w,gs.h) / 3;
        gl.glViewport(gs.w-width, gs.h-width, width, width);

        // Set projection matrix. Since it's a static camera we can hardcode the fovangle.
        gl.glMatrixMode(GL_PROJECTION);
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        glu.gluPerspective(60, 1, 1.0, 100.0);

        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();

        //Set the static position of the camera.
        Vector lookAt = new Vector(0,0,0);
        Vector cameraPos =new Vector(0,0,35);
        Vector up = new Vector(0,1,0);

        // Update the view according to the camera mode
        glu.gluLookAt(cameraPos.x(), cameraPos.y(), cameraPos.z(),
                lookAt.x(), lookAt.y(), lookAt.z(),
                up.x(), up.y(), up.z());

        // Draw race track
        raceTrack.draw(gl);
//
//        for (int i = 0; i < 4; i++) {
//            robots[i].drawAtPos(gl, glut);
//        }
        gl.glCallList(displayList);

        // Draw terrain
        terrain.draw(gl);
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
