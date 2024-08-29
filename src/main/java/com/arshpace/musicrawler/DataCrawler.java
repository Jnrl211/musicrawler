package com.arshpace.musicrawler;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    // I based the application on a Firefox WebDriver solely because it is my browser of choice, though it can recreated with any other RemoteWebDriver implementation
    FirefoxDriver driver;
    FirefoxOptions driverOptions;
    FirefoxProfile driverProfile;
    String baseUrlAsString = "https://music.youtube.com/";
    URI baseUrl;
    Duration explicitWaitTimeout = Duration.ofMillis(10000);

    DataCrawler() throws URISyntaxException {
        driverProfile = new FirefoxProfile();
        // The WebDriver language must be set to a EN-US locale, so Google always returns the view in English and not based on 
        // TODO: set the encoding of Selenium to UTF-8 too, it shows tilded characters incorrectly in console,
        // it seems this is handled by the output stream (so the terminal or the log file stream)
        driverProfile.setPreference("intl.accept_languages", "en-US");
        driverOptions = new FirefoxOptions();
        driverOptions.setProfile(driverProfile);
        driverOptions.addArguments("-headless");
        baseUrl = new URI(baseUrlAsString); // There is no reason why this should throw an exception: and even if it does, it will be passed to the JVM
    }

    List<Song> getSongs(Release release) {
        List<Song> songs = new ArrayList<>();
        driver = new FirefoxDriver(driverOptions);
        driver.get(release.getUrl());
        this.waitForKeyElementFoundAndDisplayed(By.tagName("ytmusic-shelf-renderer"));
        boolean isRetry = false;
        int attempts = JSoupRetryManager.getjSoupParseRetryAttempts();
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
            }
            isRetry = JSoupRetryManager.isEmptyListRetry(listItems);
            if (isRetry) {
                attempts--;
                System.out.println("JSoup failed to find list items. Retrying... ");
                continue;
            }
        } while (isRetry && attempts <= 0);
        System.out.println("Total: " + songs.size() + " songs");
        driver.quit();
        return songs;
    }

    // TODO: found another (unhandled) edge case: the UI of the extended releases list must be scrolled down until the bottom, 
    // this is because the initial request only loads up to 100 items (100 releases).
    List<Release> getReleases(Artist artist) {
        List<Release> releases = new ArrayList<>();
        boolean isRetry;
        int attempts;
        driver = new FirefoxDriver(driverOptions);
        driver.get(artist.getUrl());
        this.waitForKeyElementFoundAndDisplayed(By.tagName("ytmusic-carousel-shelf-renderer"));
        isRetry = false;
        attempts = JSoupRetryManager.getjSoupParseRetryAttempts();
        do {
            Document document;
            Elements listGroups;
            List<Release> albums = null;
            List<Release> singles = null;
            isRetry = false;
            document = Jsoup.parse(driver.getPageSource());
            listGroups = document.select("ytmusic-carousel-shelf-renderer");
            // System.out.println("Found " + listGroups.size() + " list groups, a.k.a. shelves");
            isRetry = JSoupRetryManager.isEmptyListRetry(listGroups);
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
        driver.quit();
        return releases;
    }

    Artist getArtist(String artistName) throws Exception {

        // This local class is meant to consolidate artist search result data in a single instance to iterate over them more easily
        class ArtistSearchResult {

            private String name;
            private String url;

            ArtistSearchResult() {
            }

            ArtistSearchResult(String name, String url) {
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

        // Avoid mixing implicit and explicit waiting strategies, using only implicit strategies doesn't work, so I will build this method with explicit waits only
        Artist result = null;
        String encodedArtistName;
        URI url;
        Wait<WebDriver> wait;
        WebElement ironSelector;
        List<WebElement> ironSelectorFormattedStrings;
        WebElement ironSelectorArtistsButton = null;
        boolean isRetry;
        int attempts;
        encodedArtistName = URLEncoder.encode(artistName, "UTF-8");
        url = baseUrl.resolve("search?q=" + encodedArtistName);
        driver = new FirefoxDriver(driverOptions);
        driver.get(url.toString());
        wait = new WebDriverWait(driver, explicitWaitTimeout);
        this.waitForKeyElementFoundAndDisplayed(By.cssSelector("iron-selector"));
        // findElement can be used here because "wait" confirms that the iron-selector is present by checking the match count of the CSS selector
        ironSelector = driver.findElement(By.cssSelector("iron-selector"));
        this.waitForKeyElementFoundAndDisplayed(By.cssSelector("iron-selector ytmusic-chip-cloud-chip-renderer div.gradient-box a.yt-simple-endpoint yt-formatted-string"));
        ironSelectorFormattedStrings = ironSelector.findElements(By.cssSelector("ytmusic-chip-cloud-chip-renderer div.gradient-box a.yt-simple-endpoint yt-formatted-string"));
        for (WebElement element : ironSelectorFormattedStrings) {
            if (!element.getText().equals("Artists")) {
                continue;
            }
            ironSelectorArtistsButton = element;
            break;
        }
        if (ironSelectorArtistsButton == null) {
            throw new Exception("Artists filter button not found");
        }
        ironSelectorArtistsButton.click();
        // At this point I will just parse HTML with JSoup. Selenium's selector methods seem to fail with complex selectors, but not JSoup
        // To to pass the Artists button reference from the enclosing scope to the inside of the lambda expression, it must be made "final"
        final WebElement finalArtistsButton = ironSelectorArtistsButton.findElement(By.xpath("."));
        wait.until(d -> {
            List<WebElement> shelfRenderer;
            WebElement artistsButtonChip;
            boolean isPageUpdated = false;
            // Checks whether there is only one shelf for Artists: more or less means there are none, or the page hasn't finished loading
            shelfRenderer = d.findElements(By.tagName("ytmusic-shelf-renderer"));
            // If the artistsButtonChip assignment throws a StaleElementReferenceException, it means the page has been updated,
            // and the original Artists button is not part of the active DOM: no need to check for specific attributes
            try {
                // Looks up the "ytmusic-chip-cloud-chip-renderer" ancestor tag to check whether its style has changed to "STYLE_PRIMARY"
                artistsButtonChip = finalArtistsButton.findElement(By.xpath("./../../.."));
            } catch (Exception e) {
                isPageUpdated = true;
            }
            // Originally, the condition was to check for the "chip-style" attribute to be "STYLE_PRIMARY" and for the "is-selected" attribute to exist and be empty string: ""
            return shelfRenderer.size() == 1 && isPageUpdated;
        });
        attempts = JSoupRetryManager.getjSoupParseRetryAttempts();
        do {
            String pageSource;
            Document document;
            Element searchResultsAncestor;
            Elements searchResults;
            List<ArtistSearchResult> artistSearchResults;
            isRetry = false;
            pageSource = driver.getPageSource();
            document = Jsoup.parse(pageSource);
            searchResultsAncestor = document.selectFirst("ytmusic-tabbed-search-results-renderer");
            if (searchResultsAncestor == null) {
                isRetry = true;
                attempts--;
                continue;
            }
            searchResults = searchResultsAncestor.select("ytmusic-section-list-renderer.style-scope div#contents ytmusic-shelf-renderer div#contents ytmusic-responsive-list-item-renderer");
            System.out.println(searchResults.size() + " artist name search results");
            artistSearchResults = new ArrayList<>();
            for (int i = 0; i < searchResults.size(); i++) {
                Element element;
                Element artistNameElement;
                Element artistChannelElement;
                ArtistSearchResult artistSearchResult;
                element = searchResults.get(i);
                artistNameElement = element.selectFirst("div.flex-columns div.title-column yt-formatted-string.title");
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
                artistSearchResult = new ArtistSearchResult();
                artistSearchResult.setName(artistNameElement.text());
                artistSearchResult.setUrl(baseUrl.resolve(artistChannelElement.attr("href")).toString());
                artistSearchResults.add(i, artistSearchResult);
            }
            // TODO: At this point, all possible channels have been collected: the best result typically shows up first in the list,
            // but just in case, there should be a way to let the user select which channel points to the correct artist,
            // because there may be artist channels sharing the same name (this is not implemented yet)
            // This picks the first channel that matches the artist name, regardless of whether there are more channels with that name
            for (int i = 0; i < artistSearchResults.size(); i++) {
                ArtistSearchResult artistSearchResult = artistSearchResults.get(i);
                // This short-circuit evaluation prevents exceptions by not evaluating the second argument unless necessary
                if (artistSearchResult == null || !artistSearchResult.getName().equals(artistName)) {
                    System.out.println("Not an artist name match. Item " + i + ": " + artistSearchResult.getName());
                    continue;
                }
                // This takes the first match in ascending order, so the one at the top first: this should suffice most of the times
                result = new Artist(artistSearchResult.getName(), artistSearchResult.getUrl());
                break;
            }
        } while (isRetry && attempts > 0);
        driver.quit();
        return result;
    }

    private List<Release> collectReleaseList(Element listGroupTitle, Class<? extends Release> releaseType, Artist artist) {
        // The ytmusic-shelf tag is the common ancestor to the title and item list (it is 5 levels above the yt-formatted-string tag)
        Element ancestorShelf;
        Elements releaseListItems;
        List<Release> releases = new ArrayList<>();
        // This gets the fifth ancestor, starting from the closest one (the direct parent)
        ancestorShelf = listGroupTitle.parents().get(4);
        // The nested page lookup can not be ignored, this behavior seems consistent on the YouTube Music web app:
        // This "if" runs when the shelf does not contain the full list, and its full contents should be retrieved from the extended list page,
        // short lists from shelfs in the Artist page can contain at most 10 items, otherwise, the "More" button is displayed in the shelf
        if (listGroupTitle.tagName().equals("a")) {
            URI allReleasesPage;
            boolean isRetry;
            int attempts;
            Elements nestedReleaseListItems;
            allReleasesPage = baseUrl.resolve(listGroupTitle.attr("href"));
            // No need to create a new Driver window on each nested lookup because the Document from the Artist page is cached by the caller,
            // and all information from other shelves can be accessed even if the driver URL changes (the caller is "getReleases")
            driver.get(allReleasesPage.toString());
            // There may be a very rare edge case where the full list has only one kind of releases: albums or singles (in quantities greater than 10), however,
            // I haven't found a single test case that fits these characteristics, and even if there is any, it is possible that this block will work just fine
            this.waitForKeyElementFoundAndDisplayed(By.tagName("ytmusic-grid-renderer"));
            isRetry = false;
            attempts = JSoupRetryManager.getjSoupParseRetryAttempts();
            do {
                Document document = Jsoup.parse(driver.getPageSource());
                nestedReleaseListItems = document.select("ytmusic-grid-renderer div#items ytmusic-two-row-item-renderer");
                isRetry = JSoupRetryManager.isEmptyListRetry(nestedReleaseListItems);
                if (isRetry) {
                    attempts--;
                    System.out.println("JSoup failed to find list items. Retrying... ");
                    continue;
                }
                releases.addAll(this.extractReleaseList(nestedReleaseListItems, "div.details div.title-group yt-formatted-string a.yt-simple-endpoint", true, releaseType, artist));
            } while (!(nestedReleaseListItems.size() > 0) && attempts > 0);
            return releases;
        }
        // This gets all the release list items of this group: Albums (and EPs), or Singles
        releaseListItems = ancestorShelf.select("ytmusic-carousel div ul ytmusic-two-row-item-renderer");
        releases.addAll(this.extractReleaseList(releaseListItems, "div div yt-formatted-string a", false, releaseType, artist));
        return releases;
    }

    private List<Release> extractReleaseList(Elements listItems, String itemTitleCssSelector, boolean isNestedLookup, Class<? extends Release> releaseClass, Artist artist) {
        List<Release> releases = new ArrayList<>();
        for (int i = 0; i < listItems.size(); i++) {
            Element listItem = listItems.get(i);
            Element releaseTitle = listItem.selectFirst(itemTitleCssSelector);
            Release release;
            Element releaseType;
            if (isNestedLookup) {
                // Gets the common ancestor and then looks for the details group containing the release type and release year (on the nested page)
                releaseType = releaseTitle
                    .parents()
                    .get(2)
                    .selectFirst("span.substring-group yt-formatted-string.subtitle")
                    .selectFirst("span")
                    ;
                // This method has an argument "releaseClass" to indicate the type of Releases to collect: Albums (and EPs, etc.) or Singles,
                // the Albums or Singles toggle button of the extended list page is -not- checked by default, so all Releases are displayed,
                // And since this method is called twice: first to collect Albums (and similar) and then to collect Singles,
                // this block is necessary to filter out duplicate entries that have already been collected, to avoid collecting
                // all Albums and all Singles twice (this issue popped up in a test, now it is solved)
                if (
                    releaseType.text().equals("Single") && !releaseClass.equals(Single.class)
                    || !(releaseType.text().equals("Single")) && releaseClass.equals(Single.class)
                    ) {
                    // System.out.println("Found a release, but method did not request finding instances of this type. Will ignore");
                    continue;
                }
            }
            // Otherwise, filtering is not necessary because the items list contains only the list of items under title Albums or Singles
            try {
                release = releaseClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
                System.out.println("Failed to create instance of implementation of Release class, could not collect Releases of type: " + releaseClass.toString());
                e.printStackTrace();
                return null;
            }
            release.setAuthor(artist);
            // The text of the "a" tag contains the Release name
            release.setName(releaseTitle.text());
            // The "href" attribute contains a "shortcut" relative URL pointing to the tracklist, not the final relative URL
            release.setUrl(baseUrl.resolve(releaseTitle.attr("href")).toString());
            // Tracklists have to be scraped too, but this method will only collect Releases, not their tracklists
            releases.add(release);
            System.out.println("Found a release: " + release.getName() + ", at relative URL: " + release.getUrl());
        }
        return releases;
    }

    private void waitForKeyElementFoundAndDisplayed(By keyElementSelector) {
        // This method throws an Exception if the element is not found after the timeout period
        // The selector must be very simple, ideally a unique tag that is only found once in the page,
        // its purpose is only to detect whether the key element has been loaded,
        // so its children containing the target information can be located and parsed by JSoup
        Wait<WebDriver> wait;
        wait = new WebDriverWait(driver, explicitWaitTimeout);
        // All of Selenium's By selectors seem unable to find elements when certain conditions are met,
        // so I have decided to use JSoup combined with timed retries to find nested elements and check them,
        // instead of Selenium's built-in selectors and waits (other than for simple initial checks).
        // "d" is the driver passed to Wait at the time of creation, and it is passed as an argument to the lambda expression,
        // the wait stops until the logic inside this lambda expression returns "true" (runs multiple times until "true", or until timeout occurs)
        wait.until(d -> {
            // In Selenium documentation, it is stated that the findElement method should not be used to look for non-present elements,
            // and to assert presence by reading findElements list size, instead
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

}
