package com.example.checkevemaster.gallery;

public class GalleryModel {
    public GalleryModel() {
    }
    private String ImageURL;
    private String ImageID;
    private long UploadTime;

    public String getImageURL() {
        return ImageURL;
    }

    public void setImageURL(String imageURL) {
        ImageURL = imageURL;
    }

    public String getImageID() {
        return ImageID;
    }

    public void setImageID(String imageID) {
        ImageID = imageID;
    }

    public long getUploadTime() {
        return UploadTime;
    }

    public void setUploadTime(long uploadTime) {
        UploadTime = uploadTime;
    }
}
