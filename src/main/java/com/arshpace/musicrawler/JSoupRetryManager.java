package com.arshpace.musicrawler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.jsoup.select.Elements;

public class JSoupRetryManager {

    static int jSoupParseRetryAttempts = 5;
    static Duration jSoupParseRetryDelay = Duration.ofMillis(1000);

    public static boolean isEmptyListRetry(Elements list) {
        boolean isRetry = false;
        if (!(list.size() > 0)) {
            isRetry = true;
            try {
                Thread.sleep(jSoupParseRetryDelay.get(ChronoUnit.MILLIS));
            } catch (InterruptedException e) {
                System.out.println("Failed to sleep on JSoup document lookup retry");
                e.printStackTrace();
            }
        }
        return isRetry;
    }

    public static int getjSoupParseRetryAttempts() {
        return jSoupParseRetryAttempts;
    }

    public static void setjSoupParseRetryAttempts(int jSoupParseRetryAttempts) {
        JSoupRetryManager.jSoupParseRetryAttempts = jSoupParseRetryAttempts;
    }

    public static Duration getjSoupParseRetryDelay() {
        return jSoupParseRetryDelay;
    }

    public static void setjSoupParseRetryDelay(Duration jSoupParseRetryDelay) {
        JSoupRetryManager.jSoupParseRetryDelay = jSoupParseRetryDelay;
    }

}
