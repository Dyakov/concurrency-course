package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    private long nextId() {
        return nextId.getAndIncrement();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = Order.withIdAndItemsInStatusNew(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order order = currentOrders.compute(orderId, (k, v) -> v.withPaymentInfo(paymentInfo));
        deliver(order);
    }

    public void setPacked(long orderId) {
        Order order = currentOrders.compute(orderId, (k, v) -> v.withPacked(true));
        deliver(order);
    }

    private void deliver(Order order) {
        /* ... */
        if(order.checkStatus()) {
            currentOrders.compute(order.getId(), (k, v) -> v.withStatus(Order.Status.DELIVERED));
        }
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
