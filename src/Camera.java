import robotrace.GlobalState;
import robotrace.Vector;

/**
 * Implementation of a camera with a position and orientation.
 */
class Camera {
    GlobalState gs;

    // The robots that are to be viewed followed
    Robot[] robots;

    int robotIdx = 0;
    /**
     * The position of the camera.
     */
    public Vector eye = new Vector(3f, 6f, 5f);

    /**
     * The point to which the camera is looking.
     */
    public Vector center = Vector.O;

    Point point;
    Point transitionTarget;

    private int camMode;

    double transitionTime = -1;

    double timeElapsed;

    /**
     * The up vector.
     */
    public Vector up = Vector.Z;

    Camera(GlobalState gs, Robot[] robots) {
        this.gs = gs;
        this.robots = robots;
        point = new DefaultPoint(gs);
    }

    public void setCamMode(int mode) {
        assert 0 <= mode && mode < 5;

        if (camMode == mode) return;

        camMode = mode;

        transitionTime = -1;
        timeElapsed = 0;

        if (camMode == 1) {
            point = new HelicopterRobotPoint(robots[robotIdx]);
        } else if (camMode == 2) {
            point = new MotorcycleRobotPoint(robots[robotIdx]);
        } else if (camMode == 3) {
            double minDist = Double.MAX_VALUE;

            for (int idx = 0; idx < robots.length; idx++) {
                if (robots[idx].distCovered < minDist) {
                    minDist = robots[idx].distCovered;
                    robotIdx = idx;
                }
            }

            point = new FirstPersonRobotPoint(robots[robotIdx]);
        } else {
            point = new DefaultPoint(gs);
        }

        up = Vector.Z;
    }

    /**
     * Updates the camera viewpoint and direction based on the
     * selected camera mode.
     */
    public void update(double frameTime) {
        timeElapsed += frameTime;

        if (transitionTime > 0) {
            if (timeElapsed > transitionTime) {
                point = transitionTarget;
                transitionTime = -1;
            }
        } else if (timeElapsed > 2 * 10e9) {
            robotIdx = (robotIdx + 1) % robots.length;

            transitionTime = 10e9;
            if (camMode == 1) {
                transitionTarget = new HelicopterRobotPoint(robots[robotIdx]);
            } else if (camMode == 2) {
                transitionTarget = new MotorcycleRobotPoint(robots[robotIdx]);
            }
            point = new Transition(point, transitionTarget, transitionTime);
            timeElapsed = 0;
        }

        center = point.getCenterPoint(timeElapsed);
        eye = point.getEyePoint(timeElapsed);
        up = point.getUp(timeElapsed);
        gs.vDist = (float) eye.subtract(center).length();
    }
}

abstract class Point {
    abstract public Vector getCenterPoint(double t);
    abstract public Vector getEyePoint(double t);
    abstract public Vector getUp(double t);
}

/**
 * Computes {@code eye}, {@code center}, and {@code up}, based
 * on the camera's default mode.
 */
class DefaultPoint extends Point {
    GlobalState gs;

    DefaultPoint(GlobalState gs) {
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

class HelicopterRobotPoint extends Point {
    private Robot robot;

    HelicopterRobotPoint(Robot robot) {
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        return robot.pos;
    }

    public Vector getEyePoint(double t) {
        return robot.pos.add(new Vector(0.00001, 0, 10));
    }


    public Vector getUp(double t) {
        return robot.tangent;
    }
}

class MotorcycleRobotPoint extends Point {
    private Robot robot;

    MotorcycleRobotPoint(Robot robot) {
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        return robot.pos.add(robot.tangent.scale(4))
                        .add(robot.tangent.cross(robot.normal))
                        .add(new Vector(0, 0, 1.25));  //FIXME: hack for robot height
    }

    public Vector getEyePoint(double t) {
        return robot.pos.add(robot.tangent.scale(-1))
                        .add(robot.tangent.cross(robot.normal))
                        .add(new Vector(0, 0, 1.25));  //FIXME: hack for robot height
    }

    public Vector getUp(double t) {
        return robot.normal;
    }
}

class FirstPersonRobotPoint extends Point {
    private Robot robot;

    FirstPersonRobotPoint(Robot robot) {
        this.robot = robot;
    }

    public Vector getCenterPoint(double t) {
        return robot.pos.add(robot.tangent.scale(5))
                .add(new Vector(0, 0, 1));  //FIXME: hack for robot height
    }

    public Vector getEyePoint(double t) {
        return robot.pos.add(robot.tangent.scale(0.1))
                .add(new Vector(0, 0, 1.80));  //FIXME: hack for robot height
//        return robot.pos.add(new Vector(0, 0, 1.80));  //FIXME: hack for robot height
    }

    public Vector getUp(double t) {
        return robot.normal;
    }
}

class Transition extends Point {
    Point point0;
    Point point1;
    double time;

    Transition(Point p0, Point p1, double t) {
        point0 = p0;
        point1 = p1;
        time = t;
    }

    public Vector getCenterPoint(double t) {
        Vector p0 = point0.getCenterPoint(t);
        Vector p1 = point1.getCenterPoint(t);

        Vector direction = p1.subtract(p0);

        return p0.add(direction.scale(Math.min(t / time, 1)));
    }

    public Vector getEyePoint(double t) {
        Vector p0 = point0.getEyePoint(t);
        Vector p1 = point1.getEyePoint(t);

        Vector direction = p1.subtract(p0);

        return p0.add(direction.scale(Math.min(t / time, 1)));
    }

    public Vector getUp(double t) {
        Vector up0 = point0.getUp(t);
        Vector up1 = point1.getUp(t);

        return up0.scale(Math.max(1 - t/time, 0)).add(up1.scale(Math.min(t / time, 1))).normalized();
    }
}