package org.apache.karaf.features.core;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RepoWatcherImpl implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RepoWatcherImpl.class);

    private long interval = 1000L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final FeaturesService featuresService;
    private List<String> watchURLs = new CopyOnWriteArrayList<>();
    private AtomicInteger counter = new AtomicInteger(0);

    public RepoWatcherImpl(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    @Override
    public void run() {
        logger.debug("Bundle watcher thread started");
        int oldCounter = -1;
        Set<Repository> watchedBundles = new HashSet<>();
        while (running.get() && watchURLs.size() > 0) {
            if (oldCounter != counter.get()) {
                oldCounter = counter.get();
                watchedBundles.clear();
                for (String bundleURL : watchURLs) {
//                    // Transform into regexp
//                    bundleURL = bundleURL.replaceAll("\\*", ".*");
                    try {
                        Repository repository = featuresService.getRepository(bundleURL);
                        if (isMavenSnapshotUrl(getLocation(repository))) {
                            watchedBundles.add(repository);
                        }
//                        for (Bundle bundle : bundleService.selectBundles(Collections.singletonList(bundleURL), false)) {
//                            if (isMavenSnapshotUrl(getLocation(bundle))) {
//                                watchedBundles.add(bundle);
//                            }
//                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (watchedBundles.size() > 0) {
//                // Get the wiring before any in case of a refresh of a dependency
//                FrameworkWiring wiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
//                File localRepository = this.localRepoDetector.getLocalRepository();
//                List<Bundle> updated = new ArrayList<>();
//                for (Repository bundle : watchedBundles) {
//                    try {
//                        updateBundleIfNecessary(localRepository, updated, bundle);
//                    } catch (IOException ex) {
//                        logger.error("Error watching bundle.", ex);
//                    } catch (BundleException ex) {
//                        logger.error("Error updating bundle.", ex);
//                    }
//                }
//                if (!updated.isEmpty()) {
//                    try {
//                        final CountDownLatch latch = new CountDownLatch(1);
//                        wiring.refreshBundles(updated, (FrameworkListener) event -> latch.countDown());
//                        latch.await();
//                    } catch (InterruptedException e) {
//                        running.set(false);
//                    }
//                    for (Bundle bundle : updated) {
//                        try {
//                            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
//                                logger.info("[Watch] Bundle {} is a fragment, so it's not started", bundle.getSymbolicName());
//                            } else {
//                                bundle.start(Bundle.START_TRANSIENT);
//                            }
//                        } catch (BundleException ex) {
//                            logger.warn("[Watch] Error starting bundle", ex);
//                        }
//                    }
//                }
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ex) {
                running.set(false);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread stopped");
        }
    }

    public void add(String url) {
        boolean shouldStart = running.get() && (watchURLs.size() == 0);
        if (!watchURLs.contains(url)) {
            watchURLs.add(url);
            counter.incrementAndGet();
        }
        if (shouldStart) {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    public void remove(String url) {
        watchURLs.remove(url);
        counter.incrementAndGet();
    }

    private String getLocation(Repository repository) {
//        String location = bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
//        return location != null ? location : bundle.getLocation();
        return repository.getURI().toString();
    }

    private boolean isMavenSnapshotUrl(String url) {
        return url.startsWith("mvn:") && url.contains("SNAPSHOT");
    }
}
