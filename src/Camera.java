import robotrace.GlobalState;
import robotrace.Vector;

/**
 * Implementation of a camera with a position and orientation.
 */
class Camera {
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
    public void update(GlobalState gs) {
        // Helicopter mode
        if (1 == gs.camMode) {
            setHelicopterMode();

            // Motor cycle gs.camMode
        } else if (2 == gs.camMode) {
            setMotorCycleMode();

            // First person gs.camMode
        } else if (3 == gs.camMode) {
            setFirstPersonMode();

            // Auto gs.camMode
        } else if (4 == gs.camMode) {
            // code goes here...

            // Default mode
        } else {
            setDefaultMode(gs);
        }
    }

    /**
     * Computes {@code eye}, {@code center}, and {@code up}, based
     * on the camera's default mode.
     */
    private void setDefaultMode(GlobalState gs) {
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