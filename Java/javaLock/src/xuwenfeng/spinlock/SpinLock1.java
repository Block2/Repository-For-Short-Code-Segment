package xuwenfeng.spinlock;

import java.util.concurrent.atomic.AtomicReference;

//可重入锁，也叫做递归锁，指的是同一线程 外层函数获得锁之后 ，内层递归函数仍然有获取该锁的代码，但不受影响。
//在JAVA环境下 ReentrantLock 和synchronized 都是 可重入锁
//synchronized是独占锁，不是以CAS实现的
//该类为可重入的自旋鎖
public class SpinLock1 {
    private AtomicReference<Thread> owner =new AtomicReference<>();
    private int count = 0;
    public void lock(){
        Thread current = Thread.currentThread();
        if(current==owner.get()) {
            count++;
            return ;
        }
 
        while(!owner.compareAndSet(null, current)){
 
        }
    }
    public void unlock (){
        Thread current = Thread.currentThread();
        if(current==owner.get()){
            if(count != 0){
                count--;
            }else{
                owner.compareAndSet(current, null);
            }
 
        }
 
    }
}

