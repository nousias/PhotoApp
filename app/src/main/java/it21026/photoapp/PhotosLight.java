package it21026.photoapp;

public class PhotosLight {
    String name,description,url,date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public PhotosLight(String name, String description, String url, String date) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.date = date;
    }
}
