package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE));

    public boolean propose(Bid bid) {
        Bid expected;
        Bid newValue;
        do {
            expected = latestBid.get();
            newValue = bid.price > expected.price ? bid : expected;
        } while (!latestBid.compareAndSet(expected, newValue));
        if(newValue != expected) {
            notifier.sendOutdatedMessage(expected);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
