import com.jogamp.opengl.util.texture.Texture;
import robotrace.Vector;
import javax.media.opengl.GL2;

import static javax.media.opengl.GL2GL3.*;

/**
 * Implementation of the terrain.
 */
class Terrain {
    static Texture landscape;
    int displayList;                    //Variable pointing to the display list.

    /**
     * Draws the terrain. It calls the displaylist to draw itself, if it's already set, otherwise it enables the texture,
     * calls drawTerrain and stores it all in the displaylist.
     */
    public void draw(GL2 gl) {
        if (displayList == 0) {
            displayList = gl.glGenLists(1);
            gl.glNewList(displayList, GL2.GL_COMPILE_AND_EXECUTE);

            drawTerrain(gl);

            gl.glEndList();
        } else {
            gl.glCallList(displayList);
        }
    }

    /**
     * Method that draws the terrain. Only gets called once, afterwards the terrain is drawn by displaylist.
     */
    private void drawTerrain(GL2 gl){
        float stepSize = 0.30f;
        Vector normal;
        gl.glColor3d(1,1,1);                            //Set color to white so it wont interfere with texture.
        landscape.enable(gl);
        landscape.bind(gl);


        //Simple algorithm to draw the surface. Normalize the height to [0,1] for texture coordinates.
        for (float x = -20; x<20; x+=stepSize) {
            gl.glBegin(gl.GL_TRIANGLE_STRIP);
            for (float y = -20; y<=20; y+=stepSize) {
                normal = getNormal(x,y);
                gl.glNormal3f((float)normal.x(),(float)normal.y(),(float)normal.z());
                gl.glTexCoord1f((heightAt(x, y) + 1f) / 2f);
                gl.glVertex3f(x, y, heightAt(x, y));

                normal = getNormal(x+stepSize, y);
                gl.glNormal3f((float) normal.x(), (float)normal.y(),(float)normal.z());
                gl.glTexCoord1f((heightAt(x+stepSize,y)+1f)/2f);
                gl.glVertex3f(x+stepSize,y,heightAt(x+stepSize,y));

            }
            gl.glEnd();
        }
        landscape.disable(gl);

        //Draw the water surface as a single quad.
        gl.glNormal3f(0,0,1f);
        gl.glColor4d(0.5,0.5,0.7,0.5);
        gl.glBegin(GL_QUADS);
        gl.glVertex3f(-20,-20,0);
        gl.glVertex3f(-20,20,0);
        gl.glVertex3f(20,20,0);
        gl.glVertex3f(20,-20,0);
        gl.glEnd();

    }

    /**
     * Computes the elevation of the terrain at ({@code x}, {@code y}).
     */
    static float heightAt(float x, float y) {
        return (float)(0.6 * Math.cos(0.3 * x + 0.2 * y) + 0.4 * Math.cos(x - 0.5*y));
    }

    /**
     * Returns the normal vector at a given x and y. Calculates tangent in X directiont and tangent in Y. Returns cross
     * product.
     */
    private Vector getNormal(float x, float y) {
        Vector tangentX = new Vector(1,0,-0.3*0.6 * Math.sin(0.3 * x + 0.2 * y) - 0.4 * Math.sin(x - 0.5 * y));
        Vector tangentY = new Vector(0,1,-0.2*0.6* Math.sin(0.3 * x + 0.2 * y) + 0.2 * Math.sin(x - 0.5 * y));

        Vector normal = tangentX.cross(tangentY);
        if (normal.length() == 0 || normal.z() == 0) {
            System.out.println("ERROR");
        }
        return normal.normalized();
    }
}
