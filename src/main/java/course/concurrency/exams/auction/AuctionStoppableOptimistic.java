package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(new Bid(-2L, -2L, -2L), false);

    public boolean propose(Bid bid) {
        Bid expected;
        Bid newValue;
        do {
            expected = latestBid.getReference();
            newValue = (expected == null) ? bid : (bid.price > expected.price ? bid : expected);
        } while (!latestBid.compareAndSet(expected, newValue, false, false));
        if(expected == null) {
            return true;
        } else if(newValue != expected) {
            notifier.sendOutdatedMessage(expected);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid expected;
        Bid newValue;
        do {
            expected = latestBid.getReference();
            newValue = expected;
        } while (!latestBid.compareAndSet(expected, newValue, false, true));
        return latestBid.getReference();
    }
}
