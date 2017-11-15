package xuwenfeng.blocklock;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

//阻塞锁的优势在于，阻塞的线程不会占用cpu时间， 不会导致 CPu占用率过高，但进入时间以及恢复时间都要比自旋锁略慢。
//理想的情况则是; 在线程竞争不激烈的情况下，使用自旋锁，竞争激烈的情况下使用，阻塞锁。
public class CLHLock1 {
    public static class CLHNode {
        private volatile Thread isLocked;
    }

    @SuppressWarnings("unused")
    private volatile CLHNode                                            tail;
    private static final ThreadLocal<CLHNode>                           LOCAL   = new ThreadLocal<CLHNode>();
    private static final AtomicReferenceFieldUpdater<CLHLock1, CLHNode> UPDATER = AtomicReferenceFieldUpdater.newUpdater(CLHLock1.class,CLHNode.class, "tail");

    public void lock() {
        CLHNode node = new CLHNode();
        LOCAL.set(node);
        CLHNode preNode = UPDATER.getAndSet(this, node);
        if (preNode != null) {
            preNode.isLocked = Thread.currentThread();
            LockSupport.park(this);
            preNode = null;
            LOCAL.set(node);
        }
    }

    public void unlock() {
        CLHNode node = LOCAL.get();
        if (!UPDATER.compareAndSet(this, node, null)) {
            System.out.println("unlock\t" + node.isLocked.getName());
            LockSupport.unpark(node.isLocked);
        }
        node = null;
    }
}
