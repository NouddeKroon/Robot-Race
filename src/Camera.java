import robotrace.GlobalState;
import robotrace.Vector;

/**
 * Implementation of a camera with a position and orientation.
 */
class Camera {
    // Symbolic names for the different modes
    static final int DEFAULT = 0;
    static final int HELICOPTER = 1;
    static final int MOTORCYCLE = 2;
    static final int FIRST_PERSON = 3;
    static final int AUTO = 4;

    static final int MODE_MIN = DEFAULT;
    static final int MODE_MAX = AUTO;

    private int camMode = DEFAULT;
    // Keeps track whether the cam mode should periodically change
    private boolean autoMode;

    // The Default mode needs to retrieve camera position info from the Global State.
    // The other modes depend on vDist such that they respond to scrolling by the user.
    GlobalState gs;

    // The robots that are to be viewed/followed
    Robot[] robots;

    // The currently selected robot
    int robotIdx = 0;

    // The position of the camera.
    public Vector eye;

    // The point to which the camera is looking.
    public Vector center;

    // Object abstracting over the different camera modes
    private Cam cam;

    // When performing a transition store the duration (in nanoseconds), if non-positive there is no current transition.
    double transitionTime = -1;

    // Store the target of a transition. After a transition is completed replace the transition object by its target.
    private Cam transitionTarget;

    // Time elapsed since mode select or transition start. When larger than transitionTime the transition is completed.
    double timeElapsed;

    // The up vector.
    public Vector up = Vector.Z;

    /**
     * Initialize and set a reasonable default viewing angle.
     * @param gs Object abstracting over user input and basic camera information.
     * @param robots The robots to be focused on by the non-default camera modes.
     */
    Camera(GlobalState gs, Robot[] robots) {
        this.gs = gs;
        this.robots = robots;

        // Initialize the default mode parameters to some reasonable viewing angle on the track.
        gs.theta = (float) Math.PI / 4;
        gs.phi = (float) Math.PI / 4;
        gs.vDist = 10;

        cam = new DefaultCam(gs);
    }

    /**
     * Set the camera mode.
     * Check if the camera mode has changed, if so perform an animation of the camera to the new mode.
     *
     * @param mode The mode the camera could be set/should stay.
     */
    public void setCamMode(int mode) {
        assert MODE_MIN <= mode && mode <= MODE_MAX;

        // If mode is already set do nothing and when a transition is still underway do nothing
        if (camMode == mode || (mode == AUTO && autoMode) || transitionTime > 0) return;

        // In case of AUTO mode, select a next camera mode and set a flag that we are in auto mode.
        if (mode == AUTO) {
            autoMode = true;
            // Select the next robot (wrap around at the end).
            robotIdx = (robotIdx + 1) % robots.length;

            if (camMode == DEFAULT) {
                mode = HELICOPTER;
            } else if (camMode == HELICOPTER) {
                mode = MOTORCYCLE;
            } else if (camMode == MOTORCYCLE) {
                mode = FIRST_PERSON;
            } else if (camMode == FIRST_PERSON) {
                mode = HELICOPTER;
            }
        } else {
            autoMode = false;
        }

        // For the different cam modes set the target
        if (mode == HELICOPTER) {
            transitionTarget = new HelicopterRobotCam(gs, robots[robotIdx]);
        } else if (mode == MOTORCYCLE) {
            transitionTarget = new MotorcycleRobotCam(gs, robots[robotIdx]);
        } else if (mode == FIRST_PERSON) {
            // Find the last robot by looking for the robot which has traveled the least distance.
            double minDist = Double.MAX_VALUE;

            for (int idx = 0; idx < robots.length; idx++) {
                if (robots[idx].distCovered < minDist) {
                    minDist = robots[idx].distCovered;
                    robotIdx = idx;
                }
            }

            transitionTarget = new FirstPersonRobotCam(gs, robots[robotIdx]);
        } else {  // camMode == DEFAULT
            transitionTarget = new DefaultCam(gs);
        }

        camMode = mode;

        // Use a short animation.
        transitionTime = 1e9;  // one second in nanoseconds
        cam = new Transition(cam, transitionTarget, transitionTime);
        timeElapsed = 0;
    }

    /**
     * Updates the camera viewpoint and direction based on the selected
     * camera mode and the time elapse since the last frame.
     *
     * @param frameTime The time difference between the previous frame and the current one.
     */
    public void update(double frameTime) {
        timeElapsed += frameTime;

        if (transitionTime > 0) {                // If there is a current transition:
            if (timeElapsed > transitionTime) {  // Is the animation done?
                cam = transitionTarget;          // Set the cam to the target.
                transitionTime = -1;
            }
        } else if (timeElapsed > 7.5e9) {  // If no transition than mix it up after some time (7.5 seconds)
            robotIdx = (robotIdx + 1) % robots.length;

            if (autoMode) {
                // Only switch modes every other transition
                if ((robotIdx & 1) == 0) {
                    if (camMode == DEFAULT) {
                        camMode = HELICOPTER;
                    } else if (camMode == HELICOPTER) {
                        camMode = MOTORCYCLE;
                    } else if (camMode == MOTORCYCLE) {
                        camMode = FIRST_PERSON;
                    } else if (camMode == FIRST_PERSON) {
                        camMode = HELICOPTER;
                    }
                }

                // The robot from which we get First Person view is only variable in auto mode.
                if (camMode == FIRST_PERSON) {
                    transitionTarget = new FirstPersonRobotCam(gs, robots[robotIdx]);
                }
            }

            if (camMode != DEFAULT) {  // There are no meaningful transitions within DEFAULT mode
                if (camMode == HELICOPTER) {
                    transitionTarget = new HelicopterRobotCam(gs, robots[robotIdx]);
                } else if (camMode == MOTORCYCLE) {
                    transitionTarget = new MotorcycleRobotCam(gs, robots[robotIdx]);
                }

                transitionTime = 2.5e9;  // 2.5 seconds
                cam = new Transition(cam, transitionTarget, transitionTime);
                timeElapsed = 0;
            }
        }

        center = cam.getCenterPoint(timeElapsed);
        eye = cam.getEyePoint(timeElapsed);
        up = cam.getUp(timeElapsed);
    }
}

/**
 * Define an object which is capable of returning the basic camera information.
 * The {@param t} in the below methods is used to pass in a difference in time since the start of a transition
 * It is up to the camera to decide it depends on this value.
 */
abstract class Cam {
    abstract public Vector getCenterPoint(double t);
    abstract public Vector getEyePoint(double t);
    abstract public Vector getUp(double t);
}

/**
 * Computes the camera's {@code eye}, {@code center}, and {@code up}, based
 * on the camera's default mode.
 */
class DefaultCam extends Cam {
    private GlobalState gs;

    DefaultCam(GlobalState gs) {
        this.gs = gs;
    }

    public Vector getCenterPoint(double t) {
        return gs.cnt;
    }

    public Vector getEyePoint(double t) {
        //Calculate the X, Y and Z coordinates of eye using spherical coordinates.
        double eyeX, eyeY, eyeZ;

        eyeX = Math.cos(gs.theta) * Math.cos(gs.phi) * gs.vDist;
        eyeY = Math.sin(gs.theta) * Math.cos(gs.phi) * gs.vDist;
        eyeZ = Math.sin(gs.phi) * gs.vDist;

        //Calculate the eyeDisplacement vector (which is a vector pointing from the centre point to the eye),
        //and add it to centre point to obtain the eye position vector relative to origin.
        Vector eyeDisplacement = new Vector(eyeX, eyeY, eyeZ);
        return gs.cnt.add(eyeDisplacement);
    }

    public Vector getUp(double t) {
        return Vector.Z;
    }
}

// A basic camera which "flies" above its selected robot.
class HelicopterRobotCam extends Cam {
    private GlobalState gs;
    private Robot robot;

    HelicopterRobotCam(GlobalState gs, Robot robot) {
        this.gs = gs;
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        return robot.pos;
    }

    public Vector getEyePoint(double t) {
        // Do not view from straight above, as this will give calculation problems (being orthogonal, etc.).
        return robot.pos.add(new Vector(0.00001, 0, gs.vDist));
    }

    public Vector getUp(double t) {
        // The robot's tangent is in the direction of its path, therefore we view it from above along its path.
        return robot.tangent;
    }
}

// A basic camera which hangs along side the selected robot ("as if the camera is located on a motor that drives along with them").
class MotorcycleRobotCam extends Cam {
    private GlobalState gs;
    private Robot robot;

    MotorcycleRobotCam(GlobalState gs, Robot robot) {
        this.gs = gs;
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        // The Robot's head is the focus of the camera.
        return robot.pos.add(new Vector(0, 0, 1.25));  //FIXME: hack for robot height
    }

    public Vector getEyePoint(double t) {
        // The eye is {@code gs.vDist} meters to the side of the robot.
        Vector directedOutward = robot.tangent.cross(robot.normal);  // These vectors are orthogonal and the result is unit length
        return robot.pos.add(directedOutward.scale(gs.vDist))
                        .add(new Vector(0, 0, 1.25));  //FIXME: hack for robot height
    }

    public Vector getUp(double t) {
        return robot.normal;
    }
}

// A basic camera which acts as if it is mounted on top of the selected robot's head.
class FirstPersonRobotCam extends Cam {
    private GlobalState gs;
    private Robot robot;

    FirstPersonRobotCam(GlobalState gs, Robot robot) {
        this.gs = gs;
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        // Look at a point some distance (gs.vDist) ahead in the current direction.
        return robot.pos.add(robot.tangent.scale(gs.vDist - 0.25))
                        .add(new Vector(0, 0, 1.80));  //FIXME: hack for robot height
    }

    public Vector getEyePoint(double t) {
        // Look from just the tip of the robot head.
        return robot.pos.add(robot.tangent.scale(0.25))
                        .add(new Vector(0, 0, 1.80));  //FIXME: hack for robot height
    }

    public Vector getUp(double t) {
        return robot.normal;
    }
}

// Cam that linearly interpolates (based on time difference) between two other
// cam objects to create a transition animation between the two.
class Transition extends Cam {
    Cam cam0;
    Cam cam1;
    // Time the animation takes, in nanoseconds.
    double time;

    // Distance between the camera and the center at both the very beginning and the very end.
    // This distance is also linearly interpolated.
    // This is done because in a naive implementation the distance between the interpolated centers and
    // the camera points may become become very close which will make the viewing distance very short.
    // This in turn will cause a large change in the view of field. As the transition occurs relatively fast
    // this will give a distorted feel to the transition.
    private double initDist;
    private double finalDist;

    /**
     * Initialize with the arguments and calculated the distance between the camera and the center and the start and
     * the end.
     *
     * @param cam0 Cam object which represents the starting point of the transition
     * @param cam1 Cam object which represents the end point of the transition
     * @param time Time in nanoseconds that it will take to complete the transition
     */
    Transition(Cam cam0, Cam cam1, double time) {
        this.cam0 = cam0;
        this.cam1 = cam1;
        this.time = time;

        initDist = cam0.getCenterPoint(0).subtract(cam0.getEyePoint(0)).length();
        finalDist = cam1.getCenterPoint(time).subtract(cam1.getEyePoint(time)).length();
    }

    /**
     * Calculate a weighted center point for the camera taking into account that the viewing distance,
     * the distance between the eye and center, is also interpolated.
     *
     * @param t time since start of the transition
     * @return Position of the center of point of the camera.
     */
    public Vector getCenterPoint(double t) {
        Vector p0 = cam0.getCenterPoint(t);
        Vector p1 = cam1.getCenterPoint(t);

        // Calculate a weighted center point between the 2 endpoints.
        Vector direction = p1.subtract(p0);
        // To be sure to not overshoot the transition the weighting is clamped at 1.
        Vector weightedCenter = p0.add(direction.scale(Math.min(t / time, 1)));

        // Calculate a vector from the (weighted) eye to the calculated center and normalize.
        Vector eyeTowardCenter = weightedCenter.subtract(getEyePoint(t)).normalized();
        // To also interpolate the viewing distance interpolate between the start and end distances.
        double weightCamDist = Math.max(1 - t/time, 0) * initDist + Math.min(t / time, 1) * finalDist;

        return getEyePoint(t).add(eyeTowardCenter.scale(weightCamDist));
    }

    /**
     * A basic clamped interpolation of the eye points.
     *
     * @param t time since start of the transition
     * @return Position of the eye point of the camera.
     */
    public Vector getEyePoint(double t) {
        Vector p0 = cam0.getEyePoint(t);
        Vector p1 = cam1.getEyePoint(t);

        Vector direction = p1.subtract(p0);

        return p0.add(direction.scale(Math.min(t / time, 1)));
    }

    /**
     * A basic clamped interpolation of the up vectors.
     *
     * @param t time since start of the transition
     * @return interpolated up vector
     */
    public Vector getUp(double t) {
        Vector up0 = cam0.getUp(t);
        Vector up1 = cam1.getUp(t);

        // Interpolate the up vectors by doing a weighing of the two from 0 to 1.
        // At t=0 the weight will all be at up0 and at t>=time the weight will all be at up1.
        // The resulting up is calculated by adding the two weighted up vectors and normalizing.
        return up0.scale(Math.max(1 - t/time, 0)).add(up1.scale(Math.min(t / time, 1))).normalized();
    }
}
