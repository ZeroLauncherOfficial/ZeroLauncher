package org.zero.launcher.ui.skin;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/// 3D model cube group representing multi-layered outer skin parts (hat, jacket, sleeves, pants).
@NotNullByDefault
public class SkinMultipleCubes extends Group {

    /// Legacy Face group kept for API compatibility.
    public static class Face extends Group {

        public Face(Image image, int startX, int startY, int width, int height, int interval, boolean reverseX, boolean reverseY,
                    Supplier<Box> supplier, BiConsumer<Box, Point2D> consumer) {
            PixelReader reader = image.getPixelReader();
            if (reader == null) return;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int argb;
                    if ((argb = reader.getArgb(startX + (reverseX ? width - x - 1 : x) * interval,
                            startY + (reverseY ? height - y - 1 : y) * interval)) != 0) {
                        Box pixel = supplier.get();
                        consumer.accept(pixel, new Point2D(x, y));
                        pixel.setMaterial(createMaterial(Color.rgb(
                                (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, (argb >> 0) & 0xFF)));
                        getChildren().add(pixel);
                    }
                }
            }
        }

        protected Material createMaterial(Color color) {
            return new PhongMaterial(color);
        }

    }

    protected int width;
    protected int height;
    protected int depth;
    protected float startX;
    protected float startY;
    protected double length;
    protected double thick;
    private final SkinCube meshView;

    public SkinMultipleCubes(int width, int height, int depth, float startX, float startY, double length, double thick) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.startX = startX;
        this.startY = startY;
        this.length = length;
        this.thick = thick;

        float scaleX = (float) ((width + depth) * 2) / 64.0F;
        float scaleY = 16.0F / 64.0F;
        this.meshView = new SkinCube(width, height, depth, scaleX, scaleY, startX, startY, (float) (thick * 2), false);
        getChildren().add(this.meshView);
    }

    private void rebuildMesh() {
        float scaleX = (float) ((width + depth) * 2) / 64.0F;
        float scaleY = 16.0F / 64.0F;
        this.meshView.setModel(new SkinCube.Model(width + (float) (thick * 2), height + (float) (thick * 2), depth + (float) (thick * 2), scaleX, scaleY, startX, startY, false));
    }

    public void setWidth(int width) {
        this.width = width;
        rebuildMesh();
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
        rebuildMesh();
    }

    public int getHeight() {
        return height;
    }

    public void setDepth(int depth) {
        this.depth = depth;
        rebuildMesh();
    }

    public int getDepth() {
        return depth;
    }

    public void setStartX(float startX) {
        this.startX = startX;
        rebuildMesh();
    }

    public float getStartX() {
        return startX;
    }

    public void setStartY(float startY) {
        this.startY = startY;
        rebuildMesh();
    }

    public float getStartY() {
        return startY;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    public void setThick(double thick) {
        this.thick = thick;
        rebuildMesh();
    }

    public double getThick() {
        return thick;
    }

    public void updateSkin(Image skin) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(skin);
        this.meshView.setMaterial(material);
    }

}
