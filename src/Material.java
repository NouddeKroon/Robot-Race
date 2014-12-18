/**
 * Materials that can be used for the robots.
 */
public enum Material {

    /**
     * Gold material properties.
     * Modify the default values to make it look like gold.
     */
    GOLD (
            new float[] {0.76f, 0.61f, 0.23f, 1.0f},
            new float[] {0.63f, 0.56f, 0.37f, 1.0f},
            new float[] {0.25f, 0.20f, 0.07f, 1.0f},
            51.2f),

    /**
     * Silver material properties.
     * Modify the default values to make it look like silver.
     */
    SILVER (
            new float[] {0.51f, 0.51f, 0.51f, 1.0f},
            new float[] {0.51f, 0.51f, 0.51f, 1.0f},
            new float[] {0.19f, 0.19f, 0.19f, 1.0f},
            51.2f),

    /**
     * Wood material properties.
     * Modify the default values to make it look like wood.
     */
    WOOD (
            new float[] {0.25f, 0.25f, 0.25f, 1f},
            new float[] {0.01f, 0.01f, 0.01f, 1.0f},
            new float[] {0.19f, 0.19f, 0.19f, 1.0f},
            0f),

    /**
     * Orange material properties.
     * Modify the default values to make it look like orange.
     */
    ORANGE (
            new float[] {0.01f, 0.01f, 0.01f, 1f},
            new float[] {0.5f, 0.5f, 0.5f, 1.0f},
            new float[] {0.1f, 0.1f, 0.1f, 1.0f},
            32f);

    /** The diffuse RGBA reflectance of the material. */
    float[] diffuse;

    /** The specular RGBA reflectance of the material. */
    float[] specular;

    /** The ambient RGBA reflectance of the material. */
    float[] ambient;

    /** The shininess RGBA reflectance of the material. */
    float shininess;
    /**
     * Constructs a new material with diffuse and specular properties.
     */
    private Material(float[] diffuse, float[] specular, float[] ambient, float shininess) {
        this.diffuse = diffuse;
        this.specular = specular;
        this.ambient = ambient;
        this.shininess = shininess;
    }
}