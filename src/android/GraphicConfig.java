package de.impacgroup.mlbarcodescanner.module;

public class GraphicConfig {

    float heightScaleFactor;
    float widthScaleFactor;
    private float width;
    int previewWidth;
    int previewHeight;
    public int xOffset = 0;
    public int yOffset = 0;
    boolean shouldFlip;

    public GraphicConfig(float heightScaleFactor, float widthScaleFactor, float width, int previewWidth, int previewHeight, boolean shouldFlip) {
        this.heightScaleFactor = heightScaleFactor;
        this.width = width;
        this.widthScaleFactor = widthScaleFactor;
        this.previewHeight = previewHeight;
        this.previewWidth = previewWidth;
        this.shouldFlip = shouldFlip;
    }

    float getWidth() {
        return width;
    }
}
