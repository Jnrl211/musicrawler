package com.arshpace.musicrawler;

import static org.junit.Assert.assertTrue;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * Unit tests for DataCrawler and App.
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
    // @Test
    public void jsoupParseFails() {
        System.out.print(System.lineSeparator());
        System.out.println("RUNNING SUB-TEST: jsoupParseFails");
        System.out.print(System.lineSeparator());
        Document document;
        // document = Jsoup.parse("Not a valid HTML tree!");
        // document = Jsoup.parse("");
        document = Jsoup.parse("><<<><<>><>>>.{.bjasjda}><");
        System.out.println("This test should reveal the value and class of a Document that failed to be parsed:");
        System.out.println("Value of document: " + String.valueOf(document));
        System.out.println("Class of document: " + document.className());
        // Result: JSoup seemingly handles anything, adding boilerplate tags if necessary
    }

    @Test
    public void encodingTest() {
        System.out.print(System.lineSeparator());
        System.out.println("RUNNING SUB-TEST: encodingTest");
        System.out.print(System.lineSeparator());
        // Prints a string in UTF-8, and the file encoding system property,
        // check whether changing the encoding of the terminal solves the wrong encoding issue
        System.out.println("Hello, World 1! Привет, мир! こんにちは世界");
        System.out.println("File encoding: " + System.getProperty("file.encoding"));
    }

    /**
     * <p>
     * You can customize this test to do web scapping on any other author, 
     * as long as the artist name matches at least one artist name in the remote website's music database.
     * </p>
     * 
     * <p>
     * Shows the artist name and URL, followed by the list of releases (albums and singles),
     * and in the end shows the tracklist from the first album (some debugging information is also displayed).
     * </p>
     * 
     * <p>
     * This test is disabled because it calls a Deprecated method
     * </p>
     * 
     * @throws Exception
     */
    // @Test
    public void showAllSongsFromFirstAlbumFromArtist() throws Exception {
        String artistName = "Claude Debussy";
        DataCrawler dataCrawler = new DataCrawler();
        Artist artist;
        List<Release> releases;
        List<Song> firstAlbumSongs;
        System.out.print(System.lineSeparator());
        System.out.println("RUNNING SUB-TEST: showAllSongsFromFirstAlbumFromArtist");
        System.out.print(System.lineSeparator());
        // Added more artists here to simulate multiple test cases
        artistName = "Nirvana";
        // artistName = "Luis Miguel";
        artist = dataCrawler.getArtist(artistName);
        System.out.println("Artist name: " + artist.getName() + ", channel URL: " + artist.getUrl());
        System.out.println("Artist releases: ");
        releases = dataCrawler.getReleases(artist);
        System.out.println("First album tracklist: ");
        firstAlbumSongs = dataCrawler.getSongs(releases.get(0));
    }

    /**
     * <p>
     * This is the updated version of the main test that:
     * </p>
     * 
     * <ul>
     *  <li>Looks up an artist channel</li>
     *  <li>Picks the first search result</li>
     *  <li>Looks up the artist discography (the list of releases)</li>
     *  <li>Picks the first release of the artist</li>
     *  <li>Looks up the tracklist of the first release</li>
     * </ul>
     * 
     * @throws Exception
     */
    @Test
    public void showAllSongsFromFirstAlbumFromArtistV2() throws Exception {
        String artistName = "Claude Debussy";
        DataCrawler dataCrawler = new DataCrawler();
        Artist artist;
        List<Artist> artistSearchResults;
        List<Release> releases;
        List<Song> firstReleaseSongs;
        System.out.print(System.lineSeparator());
        System.out.println("RUNNING SUB-TEST: showAllSongsFromFirstAlbumFromArtistV2");
        System.out.print(System.lineSeparator());
        // Added more artists here to simulate multiple test cases
        // artistName = "Nirvana";
        // artistName = "Luis Miguel";
        artistName = "Guns N' Roses";
        artistSearchResults = dataCrawler.getArtistSearchResults(artistName);
        System.out.println("Artist search results: " + artistSearchResults.size());
        for (Artist searchResult : artistSearchResults) {
            System.out.println("- " + searchResult.getName() + ", channel URL: " + searchResult.getUrl());
        }
        artist = artistSearchResults.get(0); // Picks the first search result
        if (artist == null) {
            System.out.println("Artist not found, no search results found for the provided artist name");
            return;
        }
        System.out.println("(First search result) Artist name: " + artist.getName() + ", channel URL: " + artist.getUrl());
        releases = dataCrawler.getReleases(artist);
        System.out.println("Artist releases: " + releases.size());
        for (Release release : releases) {
            System.out.println("- " + release.getName() + ", release URL: " + release.getUrl());
        }
        firstReleaseSongs = dataCrawler.getSongs(releases.get(0));
        System.out.println("First release tracks: " + firstReleaseSongs.size());
        for (Song song : firstReleaseSongs) {
            System.out.println("- " + song.getName() + ", song URL: " + song.getUrl());
        }
    }

}
