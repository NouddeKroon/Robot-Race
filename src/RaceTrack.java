import robotrace.GlobalState;
import robotrace.Vector;

import javax.media.opengl.GL2;

/**
 * Implementation of a race track that is made from Bezier segments.
 */
class RaceTrack {
    GlobalState gs;
    boolean displayListDrawn;

    int displayList;
    RoadSegment[] oTrack = new RoadSegment[2];

    //Array containing the 5 different track objects
    Track[] trackList = {
            new TestTrack(), new GenericTrack(TrackCoordinates.OTRACK), new GenericTrack(TrackCoordinates.LTRACK),
            new GenericTrack(TrackCoordinates.CTRACK), new GenericTrack(TrackCoordinates.CUSTOMTRACK)
    };

    /** Keep track of which track this instance is to draw. */
    int trackNr;




    /**
     * Constructs the race track, sets up display lists.
     */
    public RaceTrack(int trackNr, GlobalState gs) {
        this.trackNr = trackNr;
        this.gs = gs;
    }

    // Specify (after instantiation) which of the tracks to use. TRACKNR specifies tracks from 0 to TODO: max track nr.
    public void setTrackNr(int trackNr) {
        this.trackNr = trackNr;
    }


    /**
     * Draws this track, based on the selected track number.
     */



    public void draw(GL2 gl) {
        // The test track is selected
        updateTrackList(gl);
        if (!displayListDrawn) {
            displayListDrawn = true;
            displayList = gl.glGenLists(1);
            gl.glNewList(displayList, GL2.GL_COMPILE);
            trackList[trackNr].draw(gl);
            gl.glEndList();
        } else {
            gl.glCallList(displayList);
        }
    }

    private void updateTrackList (GL2 gl) {
        if (gs.trackNr != this.trackNr) {
            this.trackNr = gs.trackNr;
            gl.glDeleteLists(displayList,1);
            displayListDrawn = false;
        }
    }

    public  Vector getPositionOnLane(double s, int laneNr) {
        return trackList[trackNr].getPositionOnLane(s, laneNr);
    }

    public Vector getTangent(double s, int laneNr) {
        return trackList[trackNr].getTangent(s, laneNr);
    }

    public Vector getNormal(double s, int laneNr) {
        return trackList[trackNr].getNormal(s, laneNr);
    }
}