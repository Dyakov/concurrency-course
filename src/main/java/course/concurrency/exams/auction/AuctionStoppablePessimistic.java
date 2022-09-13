package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Lock lock = new ReentrantLock();
    private volatile boolean isStopped;

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);

    public boolean propose(Bid bid) {
        if(isStopped) {
            return false;
        }
        if(bid.price <= latestBid.price) {
            return false;
        }
        Bid latestBidCopy = null;
        try {
            lock.lock();
            if (bid.price > latestBid.price) {
                latestBidCopy = new Bid(latestBid.id, latestBid.participantId, latestBid.price);
                latestBid = bid;
            }
        } finally {
            lock.unlock();
        }
        if(latestBidCopy != null) {
            notifier.sendOutdatedMessage(latestBidCopy);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        isStopped = true;
        return latestBid;
    }
}
