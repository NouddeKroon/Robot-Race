import com.jogamp.opengl.util.texture.Texture;
import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Created by Noud on 12/22/2014.
 */

/**
 * Abstract class defining the common features between the GenericTrack and the TestTrack classes.
 */
public abstract class Track {
    static Texture brick;
    static Texture track;

    public abstract void draw(GL2 gl);

    public abstract Vector getPositionOnLane(double s, int laneNr);

    public abstract Vector getTangent(double s, int laneNr);

    public abstract Vector getNormal(double s, int laneNr);

}

