package com.arshpace.musicrawler;

/**
 * The Artist class describes an artist, band or any other kind of music author, 
 * and acts as an wrapper to provide the artist name and channel URL in the same place.
 */
public class Artist {
    
    private String name;
    private String url;

    Artist(String name, String url) {
        this.setName(name);
        this.setUrl(url);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
