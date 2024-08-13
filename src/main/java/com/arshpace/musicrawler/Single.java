package com.arshpace.musicrawler;

import java.util.List;

public class Single extends Release {
    
    Single() {
        this.setName(null);
        this.setAuthor(null);
        this.setUrl(null);
        this.setTrackList(null);
    }

    Single(String name, Artist artist, String url, List<Song> trackList) {
        this.setName(name);
        this.setAuthor(artist);
        this.setUrl(url);
        this.setTrackList(trackList);
    }

}
