import robotrace.GlobalState;
import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Implementation of a race track that is made from Bezier segments.
 */
class RaceTrack {
    GlobalState gs;
    boolean displayListDrawn;           //False if the display list is empty.
    int displayList;                    //Variable pointing to the display list.
    //Array containing the 5 different track objects
    Track[] trackList = {
            new TestTrack(), new GenericTrack(TrackCoordinates.OTRACK), new GenericTrack(TrackCoordinates.LTRACK),
            new GenericTrack(TrackCoordinates.CTRACK), new GenericTrack(TrackCoordinates.CUSTOMTRACK)};

    /**
     * Keep track of which track this instance is to draw.
     */
    int trackNr;


    /**
     * Constructs the race track, sets up display lists.
     */
    public RaceTrack(int trackNr, GlobalState gs) {
        this.trackNr = trackNr;
        this.gs = gs;
    }

    /**
     * Method first checks if there has been a change in track selection, by calling updateTrackList method. If so,
     * it generates a display list, and calls the appropiate track object to draw itself, storing it in the display list.
     * If the displayListDrawn boolean is already set to true, it just draws the stored list.
     */
    public void draw(GL2 gl) {
        if (displayList == 0) {
            displayList = gl.glGenLists(1);
        }
        updateTrackList(gl);
        if (!displayListDrawn) {
            gl.glNewList(displayList, GL2.GL_COMPILE_AND_EXECUTE);
            trackList[trackNr].draw(gl);
            gl.glEndList();
            displayListDrawn = true;
        } else {
            gl.glCallList(displayList);
        }
    }

    /**
     * Method that checks if the local trackNr variable is still the same as the global state value. If not, it updates
     * the local variable, and resets the display list.
     */
    private void updateTrackList(GL2 gl) {
        if (gs.trackNr != this.trackNr) {
            this.trackNr = gs.trackNr;
            displayListDrawn = false;
        }
    }

    //Method differs the getPositionOnLane request to the appropriate track object.
    public Vector getPositionOnLane(double s, int laneNr) {
        return trackList[trackNr].getPositionOnLane(s, laneNr);
    }

    //Method differs the getTangent request to the appropriate track object.
    public Vector getTangent(double s, int laneNr) {
        return trackList[trackNr].getTangent(s, laneNr);
    }

    //Method differs the getNormal request to the appropriate track.
    public Vector getNormal(double s, int laneNr) {
        return trackList[trackNr].getNormal(s, laneNr);
    }
}