package com.arshpace.musicrawler;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.remote.AbstractDriverOptions;

/**
 * DataCrawler is the main source of web scraping to collect information about authors, their releases and songs.
 * 
 * <p>
 * The simplest way to get started using DataCrawler is to create an instance of it, then collect the following items in this order:
 * </p>
 * 
 * <ul>
*  <li>Artist</li>
*  <li>Releases</li>
*  <li>Songs</li>
 * </ul>
 * 
 * <p>
 * Reminder: Google's unobtrusive CAPTCHA means it won't be necessary to handle CAPTCHAs, though maybe other pages will require manual CAPTCHA solving by the user,
 * in that case, disable headless mode and solve them manually from the web driver window, until a better solution is found.
 * </p>
 */
public class DataCrawler {

    // TODO: finish cleaning up private methods used by the public methods
    // I based the application on a Firefox WebDriver solely because it is my browser of choice, though it can recreated with any other RemoteWebDriver implementation
    FirefoxDriver driver;
    FirefoxOptions driverOptions;
    FirefoxProfile driverProfile;

    String baseUrlAsString = "https://music.youtube.com/";
    URI baseUrl;

    DataCrawler() throws URISyntaxException {
        driverProfile = new FirefoxProfile();
        // The WebDriver language must be set to a EN-US locale, so Google always returns the view in English and not based on 
        // TODO: set the encoding of Selenium to UTF-8 too, it shows tilded characters incorrectly in console
        driverProfile.setPreference("intl.accept_languages", "en-US");
        driverOptions = new FirefoxOptions();
        driverOptions.setProfile(driverProfile);
        driverOptions.addArguments("-headless");
        baseUrl = new URI(baseUrlAsString); // There is no reason why this should throw an exception: and even if it does, it will be passed to the JVM
    }

    List<Song> getSongs(Release release) {
        List<Song> songs = new ArrayList<>();
        int jSoupParseAttempts = 5;
        int jSoupRetryMillis = 1000;
        driver = new FirefoxDriver(driverOptions);
        driver.get(release.getUrl());
        this.waitForKeyElementFoundAndDisplayed(driver, 5, By.tagName("ytmusic-shelf-renderer"));
        System.out.println("Found " + driver.findElements(By.tagName("ytmusic-shelf-renderer")).size() + " key elements after explicit Wait");
        // JSoup
        boolean isRetry = false;
        int attempts = jSoupParseAttempts;
        do {
            Document document;
            Elements listItems;
            isRetry = false;
            document = Jsoup.parse(driver.getPageSource());
            listItems = document.select("ytmusic-responsive-list-item-renderer");
            System.out.println("List items found by JSoup: " + listItems.size());
            for (int i = 0; i < listItems.size(); i++) {
                Element listItemWrapper = listItems.get(i);
                Element listItem = listItemWrapper.selectFirst("div.flex-columns div.title-column yt-formatted-string.title a.yt-simple-endpoint");
                Song song;
                if (listItem == null) {
                    System.out.println("Failed to find item from list item wrapper");
                    continue;
                }
                System.out.println("Found an item: " + listItem.text() + ", at relative URL: " + listItem.attr("href"));
                // Song data can be collected easily here, no more processing required
                song = new Song();
                song.setAuthor(release.getAuthor());
                song.setName(listItem.text());
                song.setUrl(baseUrl.resolve(listItem.attr("href")).toString());
                songs.add(song);
                System.out.println("Song added successfully");
            }
            isRetry = isRetryJSoupDocumentLookup(listItems, jSoupRetryMillis);
            if (isRetry) {
                attempts--;
                System.out.println("JSoup failed to find list items. Retrying... ");
                continue;
            }
        } while (isRetry && attempts <= 0);
        System.out.println("Total: " + songs.size() + " songs");
        // End of JSoup
        driver.quit();
        return songs;
    }

    private boolean isRetryJSoupDocumentLookup(Elements list, int retryDelayMillis) {
        boolean isRetry = false;
        if (!(list.size() > 0)) {
            isRetry = true;
            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                System.out.println("Failed to sleep on JSoup document lookup retry");
                e.printStackTrace();
            }
        }
        return isRetry;
    }

    private void waitForKeyElementFoundAndDisplayed(WebDriver driver, int timeOutSeconds, By keyElementSelector) {
        // This method throws an Exception if the element is not found after the timeout period
        // The selector must be very simple, ideally a unique tag that is only found once in the page,
        // its purpose is only to detect whether the key element has been loaded,
        // so its children containing the target information can be located and parsed by JSoup
        Wait<WebDriver> wait;
        Duration waitTimeout;
        waitTimeout = Duration.ofSeconds(timeOutSeconds);
        wait = new WebDriverWait(driver, waitTimeout);
        // All of Selenium's By selectors seem unable to find elements more than a level deep in the DOM,
        // so I have decided to use JSoup combined with timed retries to find nested elements and check them,
        // instead of Selenium's built-in selectors and waits (other than for simple initial checks).
        // "d" is the driver passed to Wait at the time of creation, and it is passed as an argument to the lambda expression
        wait.until(d -> {
            List<WebElement> keyElementList = d.findElements(keyElementSelector);
            boolean isNonZeroCount = keyElementList.size() > 0;
            // This loop is skipped when Selenium doesn't find any elements matching the selector, so they are not present nor displayed, 
            // otherwise, it checks whether all elements are displayed if found, and returns false if at least a match is not displayed,
            // meaning the DOM hasn't finished loading (a very rare edge case, although maybe possible)
            for (WebElement keyElement : keyElementList) {
                if (!keyElement.isDisplayed()) {
                    return false;
                }
            }
            return isNonZeroCount;
        });
    }
    
    List<Release> getReleases(Artist artist) {
        List<Release> releases = new ArrayList<>();
        int jSoupParseAttempts = 5;
        int jSoupRetryMillis = 1000;
        driver = new FirefoxDriver(driverOptions);
        driver.get(artist.getUrl());
        this.waitForKeyElementFoundAndDisplayed(driver, 5, By.tagName("ytmusic-carousel-shelf-renderer"));
        System.out.println("Found " + driver.findElements(By.tagName("ytmusic-carousel-shelf-renderer")).size() + " key elements after explicit Wait");
        // JSoup
        boolean isRetry = false;
        int attempts = jSoupParseAttempts;
        do {
            Document document;
            Elements listGroups;
            List<Release> albums = null;
            List<Release> singles = null;
            isRetry = false;
            document = Jsoup.parse(driver.getPageSource());
            listGroups = document.select("ytmusic-carousel-shelf-renderer");
            System.out.println("Found " + listGroups.size() + " list groups, a.k.a. shelves");
            isRetry = isRetryJSoupDocumentLookup(listGroups, jSoupRetryMillis);
            if (isRetry) {
                attempts--;
                System.out.println("JSoup failed to find list groups. Retrying... ");
                continue;
            }
            // Look for list groups "Albums" and "Singles", which are instances of Release
            for (Element listGroup : listGroups) {
                // The header contains the group title and "More" button, if present (also, the title contains a child "a" tag if there is a "More" page)
                Element listGroupHeader = listGroup.selectFirst("ytmusic-carousel-shelf-basic-header-renderer div#details");
                Element listGroupTitle = listGroupHeader.selectFirst("yt-formatted-string.title");
                boolean isFullList = true;
                String listGroupName;
                if (listGroupTitle.selectFirst("a.yt-simple-endpoint") != null) {
                    isFullList = false;
                }
                if (!isFullList) {
                    listGroupTitle = listGroupTitle.selectFirst("a.yt-simple-endpoint");
                }
                listGroupName = listGroupTitle.text();
                // These are going to be evaluated in every iteration as ternaries now, so when the condition is false I set them to remain unchanged by assigning themselves
                albums = (listGroupName.equals("Albums")) ? collectReleaseList(listGroupTitle, Album.class, artist) : albums;
                singles = (listGroupName.equals("Singles")) ? collectReleaseList(listGroupTitle, Single.class, artist) : singles;
            }
            releases.addAll(albums);
            releases.addAll(singles);
            System.out.println("Total: " + releases.size() + " releases");
        } while (isRetry && attempts <= 0);
        // End of JSoup
        driver.quit();
        return releases;
    }

    // TODO: refactor and clean up this method
    Artist getArtist(String artistName) throws Exception {
        // Avoid mixing implicit and explicit waiting strategies, using only implicit strategies doesn't work, so I will build this method with explicit waits only
        Artist result = null;

        String encodedArtistName;
        URI url;

        Duration waitTimeout = Duration.ofSeconds(5);
        Wait<WebDriver> wait;

        WebElement ironSelector;
        List<WebElement> ironSelectorFormattedStrings;
        WebElement ironSelectorArtistsButton = null;

        driver = new FirefoxDriver(driverOptions);

        encodedArtistName = URLEncoder.encode(artistName, "UTF-8");
        url = baseUrl.resolve("search?q=" + encodedArtistName);

        driver.get(url.toString());

        wait = new WebDriverWait(driver, waitTimeout);
        wait.until(d -> {
            // "d" is the web driver of the wait object passed as argument to the lambda expression
            // The wait stops until the logic inside this lambda expression returns "true" (runs multiple times until "true", or until timeout occurs)
            // Note: in the documentation, it is said that the findElement method should not be used to look for non-present elements,
            // and to assert presence by reading findElements list size, instead
            List<WebElement> ironSelectorTest = driver.findElements(By.cssSelector("iron-selector"));
            return ironSelectorTest.size() > 0;
        });
        System.out.println("iron-selector found");
        
        ironSelector = driver.findElement(By.cssSelector("iron-selector"));

        wait.until(d -> {
            List<WebElement> ironSelectorFormattedStringsTest = ironSelector.findElements(By.cssSelector("ytmusic-chip-cloud-chip-renderer div.gradient-box a.yt-simple-endpoint yt-formatted-string"));
            return ironSelectorFormattedStringsTest.size() > 0;
        });
        System.out.println("iron-selector yt-formatted-string(s) found");

        ironSelectorFormattedStrings = ironSelector.findElements(By.cssSelector("ytmusic-chip-cloud-chip-renderer div.gradient-box a.yt-simple-endpoint yt-formatted-string"));
        System.out.println(String.valueOf(ironSelectorFormattedStrings.size()) + " items found");

        for (WebElement element : ironSelectorFormattedStrings) {
            if (!element.getText().equals("Artists")) {
                System.out.println("- Button text is: " + element.getText());
                continue;
            }
            ironSelectorArtistsButton = element;
            break;
        }
        if (ironSelectorArtistsButton == null) {
            throw new Exception("Artists filter button not found");
        }
        System.out.println("iron-selector yt-formatted-string(s) Artists filter button found");
        ironSelectorArtistsButton.click();
        System.out.println("Redirecting... ");

        // At this point I will just parse HTML with JSoup. Selenium's selector methods seem to fail with complex selectors, but JSoup gets the job done
        Thread.sleep(5000);
        
        String pageSource = driver.getPageSource();

        // JSoup
        Document document = Jsoup.parse(pageSource);

        Elements uniqueAncestor = document.select("ytmusic-tabbed-search-results-renderer.style-scope");

        System.out.println(uniqueAncestor.size() + " unique ancestors");
        if (uniqueAncestor.size() != 1) {
            throw new Exception("Unexpected unique ancestor size after 'Show All' button click");
        }

        Elements artistEntries = uniqueAncestor.first().select("ytmusic-section-list-renderer.style-scope div#contents ytmusic-shelf-renderer div#contents ytmusic-responsive-list-item-renderer");
        System.out.println(artistEntries.size() + " artist entries");

        List<String> artistNames = new ArrayList<String>(); // This was being initialized to 0, regardless of the initial capacity argument, it seems. Ignore it
        List<String> artistChannelURLs = new ArrayList<String>();

        for (int i = 0; i < artistEntries.size(); i++) {
            Element element = artistEntries.get(i);
            Element artistNameElement = element.selectFirst("div.flex-columns div.title-column yt-formatted-string.title");
            Element artistChannelElement;
            String artistChannelRelativeURL;
            URI artistChannelURI;
            if (artistNameElement == null) {
                // List item will be left as "null" (because list has been initialized to the artist count size)
                System.out.println("Item " + i + " is null");
                continue;
            }
            System.out.println("Item " + i + " contains artist name: " + artistNameElement.text());
            
            artistChannelElement = element.selectFirst("a.yt-simple-endpoint");
            if (artistChannelElement == null) {
                System.out.println("Item " + i + " channel URL not found (this entry will be ommited)");
                continue;
            }
            artistChannelRelativeURL = artistChannelElement.attr("href");
            artistChannelURI = baseUrl.resolve(artistChannelRelativeURL);
            System.out.println("Item " + i + " full channel URI is: " + artistChannelURI.toString());
            artistChannelURLs.add(i, artistChannelURI.toString());
            artistNames.add(i, artistNameElement.text());
        }
        // End of JSoup
        
        // TODO: At this point, all possible channel URLs have been collected: the top result typically shows up first in the collected list,
        // just in case, there should be a way to let the user select which channel points to the correct artist,
        // because there may be artist channels sharing the same name

        for (int i = 0; i < artistNames.size(); i++) {
            if (!artistNames.get(i).equals(artistName)) {
                System.out.println("Not an artist name match. Item " + i + ": " + artistNames.get(i));
                continue;
            }
            // Here, this takes the first match in ascending order (so the one at the top, should suffice most of the times)
            result = new Artist(artistNames.get(i), artistChannelURLs.get(i));
            break;
        }

        // TODO: maybe I should handle the driver externally, or enclose all of this in a try-catch-finally block (and close the driver in finally)
        driver.quit();
        return result;
    }

    private List<Release> collectReleaseList(Element listGroupTitle, Class<? extends Release> releaseType, Artist artist) {
        // TODO: I can't get rid of the nested lookup, this behavior seems consistent, and there may also be other considerations
        // For example: I will have to modify this scraper to also look up behind the "More" button on each shelf to see the full list of Releases

        // The ytmusic-shelf tag is the common ancestor to the title and item list (it is 5 levels above the yt-formatted-string tag)
        Element ancestorShelf;
        Elements releaseListItems;
        List<Release> releases = new ArrayList<>();

        // This gets the fifth ancestor, starting from the closest one (the direct parent)
        ancestorShelf = listGroupTitle.parents().get(4);

        if (listGroupTitle.tagName().equals("a")) {
            // Runs when this is not the full list, and should retrieve it from the extended list page
            // TODO: At the moment, this is just a patch to collect the items from the currently displayed list (the proper way in this edge case is to look up the "More" button and scrape the link page)
            ancestorShelf = listGroupTitle.parents().get(5);
        }

        // Otherwise, extract the list as normal
        // This gets all the release list items of this group ("Albums" or "Singles")
        releaseListItems = ancestorShelf.select("ytmusic-carousel div ul ytmusic-two-row-item-renderer");
        for (int i = 0; i < releaseListItems.size(); i++) {
            Element releaseListItem = releaseListItems.get(i);
            Element releaseTitle = releaseListItem.selectFirst("div div yt-formatted-string a");
            Release release;
            try {
                release = releaseType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                System.out.println("Failed to create instance of implementation of Release class, could not collect Releases of type: " + releaseType.toString());
                e.printStackTrace();
                return null;
            }
            release.setAuthor(artist);
            // This contains the Release name
            release.setName(releaseTitle.text());
            // This is a "shortcut" relative URL, not the final relative URL
            release.setUrl(baseUrl.resolve(releaseTitle.attr("href")).toString());
            // Tracklists have to be scraped too, so this will only collects Releases, not their tracklists
            releases.add(release);
            System.out.println("Found a release: " + release.getName() + ", at relative URL: " + release.getUrl());
        }
        return releases;
    }

}
