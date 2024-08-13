package com.arshpace.musicrawler;

/**
 * Song is a class intended to provide the identification and access details about a song, and that's it.
 */
public class Song {
    
    private String name = null;
    private Artist author = null;
    private String url = null;

    Song() {
    }

    Song(String name, Artist author, String url) {
        this.setName(name);
        this.setAuthor(author);
        this.setUrl(url);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Artist getAuthor() {
        return author;
    }

    public void setAuthor(Artist author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
