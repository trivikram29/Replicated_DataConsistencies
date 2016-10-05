package Clusters;

import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BaseballCluster extends UnicastRemoteObject implements Proxy_Cluster_interface{
	static final int MONOTONIC = 2;
	static final int STALEBOUNDEDNESS = 3;
	static final int STRONGCONSISTENCY = 0;
	static final int EVENTUALCONSISTENCY = 1;
	int clusterNumber = 0, delay = 23, stamp = 0;
	HashMap<Integer, String> hashMap = new HashMap<>();
	HashMap<Integer, Lock> lockHashMap = new HashMap<>();
	HashMap<Integer, Boolean> boolHashMap = new HashMap<>();
	HashMap<Integer, Long> timeStamp = new HashMap<>();
	HashMap<Integer, Long> monotonicTimestamp = new HashMap<>();
	HashMap<Integer, Long> staleBoundedTS = new HashMap<>();
	protected BaseballCluster() throws RemoteException {
		System.out.println("cluster initiated");
	}

	public static void main(String[] args) {
		BaseballCluster cluster1, cluster2;
		try {
			cluster1 = new BaseballCluster();
			cluster1.clusterNumber = 0;
			cluster2 = new BaseballCluster();
			cluster2.clusterNumber = 1;
			Naming.rebind("cluster0", cluster1);
			Naming.rebind("cluster1", cluster2);
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public long takeLock(int keys[]) throws RemoteException {
		Lock locks[] = new Lock[keys.length];
		long nanos = 0, nanos2 = 0;
		nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		for(int i = 0; i < keys.length; i++){
			if((locks[i] = lockHashMap.get(keys[i])) == null){
				locks[i] = getLock(keys[i]);
			}
		}
		for(int i = 0; i < keys.length; i++){
			synchronized (locks[i]) {
				
				while (true) {
					System.out.println(" trying for " +keys[i]+" " + clusterNumber);
					if (!boolHashMap.get(keys[i])) {
						boolHashMap.put(keys[i], true);
						//System.out.println(" key " +keys[i]+" locked for " + clusterNumber);
						break;
					}
				}
				
			}
		}
		nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ((nanos2-nanos) + delay * 1000000);
	}
	

	@Override
	public void releaseLock(int keys[]) throws RemoteException {
		for(int i = 0; i < keys.length; i++){
			System.out.println(" key " +keys[i]+" released for " + clusterNumber);
			boolHashMap.put(keys[i], false);
		}
	}
	
	
	
	public synchronized Lock getLock(int key){
		Lock lock = null;
		if((lock = lockHashMap.get(key)) != null){
			return lock;	
		}
		lock = new ReentrantLock();
		lockHashMap.put(key, lock);
		boolHashMap.put(key, false);
		monotonicTimestamp.put(key, (long)-1);
		timeStamp.put(key, (long) 0);
		staleBoundedTS.put(key, (long) 0);
		return lock;
	}
	
	@Override
	public int put(int keys[], String value[]) throws RemoteException {
		System.out.println("cluster "+clusterNumber + "  " +value);
		for(int i = 0; i < keys.length; i++){
			hashMap.put(keys[i], value[i]);
			timeStamp.put(keys[i], (long)(++stamp));
			staleBoundedTS.put(keys[i], (staleBoundedTS.get(keys[i])+1));
		}
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public String[] get(int keys[], int type, long arg[]) throws RemoteException {
		long nanos = 0, nanos2 = 0;
		String val[] = new String[keys.length + 1];
		nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		for(int i = 0; i < keys.length; i++){
			val[i] = hashMap.get(keys[i]);
			if(type == MONOTONIC){
				monotonicTimestamp.put(keys[i], arg[i]);	
			}
		}
		nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		val[val.length-1] = ((nanos2-nanos) + delay * 1000000) + "";
		return val;
	}
	
	
	@Override
	public String[] getTimeStamp(int keys[], int type) throws RemoteException {
		long nanos = 0, nanos2 = 0, val2 = 0;
		String str[] = new String[keys.length+1];
		nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		if(type == MONOTONIC){
			for(int i = 0; i < keys.length; i++){
				long val = timeStamp.get(keys[i]);
				val2 = monotonicTimestamp.get(keys[i]);
				str[i] = val + "/" + val2;
			}
		}
		if(type == STALEBOUNDEDNESS){
			for(int i = 0; i < keys.length; i++){
				long val = timeStamp.get(keys[i]);
				val2 = staleBoundedTS.get(keys[i]);
				str[i] = val + "/" + val2;
			}
		}

		nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		str[str.length-1] = (nanos2-nanos) + (delay * 1000000) +"";
		return str;
	}

}
