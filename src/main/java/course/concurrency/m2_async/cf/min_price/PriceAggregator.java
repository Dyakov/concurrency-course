package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
        long timeout = 1_000_000L - 1L;
        List<CompletableFuture<Double>> completableFutures = new ArrayList<>(shopIds.size());
        shopIds.forEach(
                shopId -> {
                    CompletableFuture<Double> completableFuture = CompletableFuture.supplyAsync(
                            () -> priceRetriever.getPrice(itemId, shopId)
                    ).completeOnTimeout(NaN, timeout, TimeUnit.MICROSECONDS).exceptionally(ex -> NaN);
                    completableFutures.add(completableFuture);
                }
        );
        OptionalDouble minPriceOpt = OptionalDouble.empty();
        try {
            minPriceOpt = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
                    .thenApply(
                            price -> completableFutures.parallelStream()
                                    .filter(CompletableFuture::isDone)
                                    .map(completableFuture -> completableFuture.getNow(NaN))
                                    .filter(v -> !v.isNaN())
                                    .mapToDouble(v -> v)
                                    .min()
                    ).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return minPriceOpt.orElse(NaN);
    }
}
