package com.arshpace.musicrawler;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    // @Test
    // public void shouldAnswerWithTrue()
    // {
    //     assertTrue( true );
    // }

    /**
     * A test I did to check the response of the JSoup.parse method when given a gibberish argument instead of valid HTML
     */
    @Test
    public void jsoupParseFails() {
        Document document;
        // document = Jsoup.parse("Not a valid HTML tree!");
        // document = Jsoup.parse("");
        document = Jsoup.parse("><<<><<>><>>>.{.bjasjda}><");
        System.out.println("This test should reveal the value and class of a Document that failed to be parsed:");
        System.out.println("Value of document: " + String.valueOf(document));
        System.out.println("Class of document: " + document.className());
        // Result: JSoup seemingly handles anything, adding boilerplate tags if necessary
    }

    /**
     * <p>
     * You can customize this test to do web scapping on any other author, 
     * as long as the artist name matches at least artist name in the remote website's music database.
     * </p>
     * 
     * <p>
     * Shows the artist name and URL, followed by the list of releases (albums and singles),
     * and in the end shows the tracklist from the first album (some debugging information is also displayed).
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void showAllSongsFromFirstAlbumFromArtist() throws Exception {
        String artistName = "Claude Debussy";
        DataCrawler dataCrawler = new DataCrawler();
        Artist artist;
        List<Release> releases;
        List<Song> firstAlbumSongs;
        artist = dataCrawler.getArtist(artistName);
        System.out.println("Artist name: " + artist.getName() + ", channel URL: " + artist.getUrl());
        System.out.println("Artist releases: ");
        releases = dataCrawler.getReleases(artist);
        System.out.println("First album tracklist: ");
        firstAlbumSongs = dataCrawler.getSongs(releases.get(0));
    }

}
