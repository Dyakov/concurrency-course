package course.concurrency.exams.refactoring;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;
    private MountainTableManagerFactory mountainTableManagerFactory;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));
        mountainTableManagerFactory = new MountainTableManagerFactory();
        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<Manager> refreshThreads = routerStore.getCachedRecords().stream()
                .map(Others.RouterState::getAdminAddress)
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isEmpty))
                .map(this::createRefresher)
                .collect(Collectors.toList());
        if (!refreshThreads.isEmpty()) {
            invokeRefresh(refreshThreads);
        }
    }

    private Manager createRefresher(String adminAddress) {
        if (isLocalAdmin(adminAddress)) {
            return getLocalRefresher(adminAddress);
        } else {
            return mountainTableManagerFactory.createManager(adminAddress);
        }
    }

    protected Manager getLocalRefresher(String adminAddress) {
        return mountainTableManagerFactory.createManager("local");
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<Manager> refreshThreads) {
        List<CompletableFuture<Void>> completableFutures = refreshThreads.stream()
                .map(
                        refreshManager -> CompletableFuture.runAsync(() -> refreshManager.refresh())
                                .orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                )
                .collect(Collectors.toList());
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .exceptionally(ex -> {
                    if(ex instanceof CompletionException) {
                        if (ex.getCause() instanceof TimeoutException) {
                            log("Not all router admins updated their cache");
                        } else if (ex instanceof InterruptedException) {
                            log("Mount table cache refresher was interrupted.");
                        }
                    }
                    return null;
                })
                .join();
        logResult(refreshThreads);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<Manager> refreshThreads) {
        int successCount = 0;
        int failureCount = 0;
        for (Manager mountTableManager : refreshThreads) {
            if (mountTableManager.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableManager.getAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }

    public void setMountainTableManagerFactory(MountainTableManagerFactory mountainTableManagerFactory) {
        this.mountainTableManagerFactory = mountainTableManagerFactory;
    }
}