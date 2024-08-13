package com.arshpace.musicrawler;

import java.util.List;

public class Album extends Release {
    
    Album() {
        this.setName(null);
        this.setAuthor(null);
        this.setUrl(null);
        this.setTrackList(null);
    }

    Album(String name, Artist artist, String url, List<Song> trackList) {
        this.setName(name);
        this.setAuthor(artist);
        this.setUrl(url);
        this.setTrackList(trackList);
    }

}
