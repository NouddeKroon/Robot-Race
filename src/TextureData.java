/**
 * Created by Noud on 1/1/2015.
 */
class TextureData {
    double leftWallTexCoorLast;
    double leftWallTexCoorNext;
    double rightWallTexCoorLast;
    double rightWallTexCoorNext;
    double[][] roadTexCoors;
    double[] distanceData;

    TextureData(){
        roadTexCoors = new double[4][2];
    }
}
