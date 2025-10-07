package ford.james.motorola.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class LockUtils {

	public static Lock getLock(ReadWriteLock lock, boolean isWrite) {
		return isWrite ? lock.writeLock() : lock.readLock();
	}
}
