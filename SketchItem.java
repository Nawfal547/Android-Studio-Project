package com.example.photoapp_final;

public class SketchItem {
    private final byte[] image;
    private final String metadata;

    public SketchItem(byte[] image, String metadata) {
        this.image = image;
        this.metadata = metadata;
    }

    public byte[] getImage() {
        return image;
    }

    public String getMetadata() {
        return metadata;
    }
}