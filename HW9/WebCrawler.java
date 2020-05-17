package ru.ifmo.rain.fadeev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
//import java.util.function.Supplier;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> setOfDownloadedPages = ConcurrentHashMap.newKeySet();
        Set<String> visitedPages = ConcurrentHashMap.newKeySet();

        Map<String, IOException> errors = new ConcurrentHashMap<>();

        if(depth >= 1) {
            visitedPages.add(url);
            Phaser phaser = new Phaser(1);
            download(url, depth, phaser, visitedPages, setOfDownloadedPages, errors);
            phaser.arriveAndAwaitAdvance();
        }
        return new Result(new ArrayList<>(setOfDownloadedPages), errors);
    }

    @Override
    public void close() {
        downloaders.shutdownNow();
        extractors.shutdownNow();
    }

    void download(String url,
                  int depth,
                  Phaser phaser,
                  Set<String> visitedPages,
                  Set<String> setOfDownloadedPages,
                  Map<String, IOException> errors) {

        phaser.register();

        downloaders.submit(() -> {
        try {
            Document page = downloader.download(url);
            setOfDownloadedPages.add(url);
            if (depth != 1) {
                phaser.register();
                extractors.submit(() -> {
                    try {
                        List<String> urls = page.extractLinks();
                        for (String newUrl : urls) {
                            if (!visitedPages.contains(newUrl)) {
                                visitedPages.add(newUrl);
                                download(newUrl,
                                        depth - 1,
                                        phaser,
                                        visitedPages,
                                        setOfDownloadedPages,
                                        errors);
                            }
                        }
                    } catch (IOException ex) {
                        errors.put(url, ex);
                    } finally {
                        phaser.arrive();
                    }
                });
            }
        } catch (IOException e) {
            errors.put(url, e);
        } finally {
            phaser.arrive();
        } });
    }

    public static void main(String[] args) {
        if (args == null || args.length > 5 || args.length < 2) {
            System.err.println("Invalid input arguments, follow this format:");
            System.err.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
        } else {
            try {
                try (Crawler webCrawler = new WebCrawler(
                        new CachingDownloader(),
                        Integer.parseInt(args[2]),
                        Integer.parseInt(args[3]), 1)) {

                    webCrawler.download(args[0], Integer.parseInt(args[1]));
                }
            } catch (IOException e) {
                System.err.println("IOException in main");
                e.printStackTrace();
            }
        }
    }
}