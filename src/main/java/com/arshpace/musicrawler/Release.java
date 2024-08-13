package com.arshpace.musicrawler;

import java.util.List;

/**
 * Release is any kind of music product containing a tracklist: an Album, a Single, or an EP (Extended Piece).
 * YouTube Music seems to classify artist releases in either albums or singles (EPs are listed under Singles, apparently).
 * Since both albums and singles in YouTube Music follow the same structure, this class is used as a parent class to both of them.
 */
public abstract class Release {
    
    private String name;
    private Artist author;
    private String url;
    private List<Song> trackList;

    Release() {
    }

    Release(String name, Artist artist, String url, List<Song> trackList) {
        this.setName(name);
        this.setAuthor(artist);
        this.setUrl(url);
        this.setTrackList(trackList);
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

    public List<Song> getTrackList() {
        return trackList;
    }

    public void setTrackList(List<Song> trackList) {
        this.trackList = trackList;
    }

}

