package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionPessimistic implements Auction {

    private final Lock lock = new ReentrantLock();

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;

    public boolean propose(Bid bid) {
        try {
            lock.lock();
            if(latestBid == null) {
                latestBid = bid;
                return true;
            } else if (bid.price > latestBid.price) {
                Bid latestBidCopy = new Bid(latestBid.id, latestBid.participantId, latestBid.price);
                notifier.sendOutdatedMessage(latestBidCopy);
                latestBid = bid;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Bid getLatestBid() {
        try {
            lock.lock();
            return latestBid;
        } finally {
            lock.unlock();
        }

    }
}
