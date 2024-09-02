# musicrawler
MusiCrawler is a Java library that acts as a web scraper to collect music and discography information from a popular music streaming platform.

Note that this project is a work in process, and may be subject to change.

## Building
This is a Java project that uses Maven for dependency management, so you need to install it in order to build this project. 

Once you have set up your environment, run the Maven "compile" goal to produce a JAR file that you can use as a dependency in your own projects.

For example, on Windows, travel to the root directory of the project and call Maven like the next line of code, or use IDE extensions to run Maven goals. Make sure Maven is accessible from the project directory.
```
mvn compile -f ./pom.xml
```

## Running
At the moment, this project is built as a library so the JAR is not meant for running as a stand-alone Java application, but you should be able to import its classes to perform web scraping for educational or otherwise acceptable uses.

Typically, to collect information about an artist, a release, or a song, you follow these steps in order, and stop where required:
* Create an instance of DataCrawler
* Request artist name search results
* Select an artist from search results
* Request artist releases (albums, EPs, singles: basically their discography)
* Select a release from artist
* Request tracklist from release
* Select song from release tracklist

The tests made for this project contain code examples on how to do this.