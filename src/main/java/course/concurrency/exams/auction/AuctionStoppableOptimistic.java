package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(new Bid(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE), false);

    public boolean propose(Bid bid) {
        Bid expected;
        do {
            expected = latestBid.getReference();
            if(bid.price <= expected.price) {
                return false;
            }
        } while (!latestBid.compareAndSet(expected, bid, false, false) || !latestBid.isMarked());
        if(latestBid.isMarked()) {
            return false;
        }
        notifier.sendOutdatedMessage(expected);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid expected;
        do {
            expected = latestBid.getReference();
        } while (!latestBid.compareAndSet(expected, expected, false, true));
        return latestBid.getReference();
    }
}
