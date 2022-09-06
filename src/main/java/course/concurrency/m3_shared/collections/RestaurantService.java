package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RestaurantService {

    private Map<String, Long> stat = new ConcurrentHashMap<>();
    private Restaurant mockRestaurant = new Restaurant("A");

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return mockRestaurant;
    }

    public void addToStat(String restaurantName) {
        stat.putIfAbsent(restaurantName, 0L);
        stat.computeIfPresent(restaurantName, (k, v) -> ++v);
    }

    public Set<String> printStat() {
        return stat.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.toSet());
    }
}
