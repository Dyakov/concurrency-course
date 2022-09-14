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
        currentOrders.compute(orderId, (k, v) -> v.withPaymentInfo(paymentInfo));
        if (currentOrders.get(orderId).checkStatus()) {
            deliver(currentOrders.get(orderId));
        }
    }

    public void setPacked(long orderId) {
        currentOrders.compute(orderId, (k, v) -> v.withPacked(true));
        if (currentOrders.get(orderId).checkStatus()) {
            deliver(currentOrders.get(orderId));
        }
    }

    private synchronized void deliver(Order order) {
        /* ... */
        if(currentOrders.get(order.getId()).checkStatus()) {
            currentOrders.compute(order.getId(), (k, v) -> v.withStatus(Order.Status.DELIVERED));
        }
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
