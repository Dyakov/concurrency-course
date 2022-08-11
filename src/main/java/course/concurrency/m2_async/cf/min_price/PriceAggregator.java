package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>> completableFutures = shopIds.stream()
                .map(
                        shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))
                                .completeOnTimeout(NaN, 999999, TimeUnit.MICROSECONDS)
                                .exceptionally(ex -> NaN)
                ).collect(Collectors.toList());
        return CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .thenApply(
                        price -> completableFutures.parallelStream()
                                .mapToDouble(CompletableFuture::join)
                                .filter(value -> !Double.isNaN(value))
                                .min()
                ).join().orElse(NaN);
    }
}
