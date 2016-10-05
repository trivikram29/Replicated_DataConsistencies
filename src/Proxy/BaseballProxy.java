package Proxy;

import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Clusters.Proxy_Cluster_interface;

public class BaseballProxy extends UnicastRemoteObject implements Client_Proxy_interface {
	static final int STRONGCONSISTENCY = 0;
	static final int EVENTUALCONSISTENCY = 1;
	static final int MONOTONIC = 2;
	static final int STALEBOUNDEDNESS = 3;
	static final int StaleLimit = 1;
	static int count = 0;
	
	Proxy_Cluster_interface pci[] = new Proxy_Cluster_interface[2];
	long strongReadTime = 0, eventualReadTime = 0, monotonicReadTime = 0, boundedstaleReadTime = 0;
	protected BaseballProxy() throws RemoteException {
		try {
			pci[0] = (Proxy_Cluster_interface)Naming.lookup("cluster0");
			pci[1] = (Proxy_Cluster_interface)Naming.lookup("cluster1");
		} catch (MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}
		System.out.println("Base ball Server started");
	}

	public static void main(String[] args) {
		try {
			BaseballProxy p = new BaseballProxy();
			Naming.rebind("client_proxy", p);
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public int put(int key, String val) throws RemoteException {
		int keys[] = new int[1];
		keys[0] = key;
		String value[] = new String[1];
		value[0] = val;
		long time = pci[0].takeLock(keys);
		long time2 = pci[1].takeLock(keys);
		pci[0].put(keys, value);
		pci[1].put(keys, value);
		pci[0].releaseLock(keys);
		pci[1].releaseLock(keys);
		return 0;
	}

	public String[] get(int keys[], int type) throws RemoteException {
		String str[] = null;
		switch(type){
		case STRONGCONSISTENCY:
			str = strongGet(keys);
			break;
		case EVENTUALCONSISTENCY:
			str = eventualGet(keys);
			break;
		case MONOTONIC:
			str = monotonicGet(keys);
			break;
		case STALEBOUNDEDNESS:
			str = boundedStaleGet(keys);
			break;
		}
	return str;
	}
	
	public String[] strongGet(int keys[]) throws RemoteException {
		//Strong consistent read
		String str[];
		long args[] = new long[1];
		long time = 0;
		time = time + pci[0].takeLock(keys);
		time = pci[1].takeLock(keys) + time;

		str = pci[(int) (Math.random() * pci.length)].get(keys, STRONGCONSISTENCY, args);
		time = time + Long.parseLong(str[str.length-1]);
		System.out.println(str);

		long nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		pci[0].releaseLock(keys);
		pci[1].releaseLock(keys);
		long nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
		//System.out.println("read key " + key + " value " + str);
		time = time + (nanos2 - nanos);
		System.out.println("time taken for strong read with keys "+keys+" is "+ time);
		strongReadTime = strongReadTime + time;
		System.out.println("total time taken "+strongReadTime);
		return str;
	}
	
	public String[] eventualGet(int keys[]) throws RemoteException {
		//Eventual consistent read
		String str[];
		long args[] = new long[1];
		long time = 0;
		str = pci[(int)(Math.random()*pci.length)].get(keys, EVENTUALCONSISTENCY, args);
		time = time + Long.parseLong(str[str.length-1]);
		System.out.println(str);
		System.out.println("time taken for eventual read with key "+keys+" is "+ time);
		eventualReadTime = eventualReadTime + time;
		System.out.println("total time taken "+eventualReadTime);
		return str;
	}
	
	public String[] monotonicGet(int keys[]) throws RemoteException {
	//Monotonic read
	String str[];
	//0th element contains timestamp of data, 1st element contains timestamp of monotonic read
	//2nd element contains time taken to get the timestamps
	ArrayList<String[]> times = new ArrayList<String[]>();
	int ind = (int)(Math.random()*pci.length);
	for(int i = 0; i < pci.length; i++){
		times.add(pci[ind].getTimeStamp(keys, MONOTONIC));
		ind = (ind+1)%pci.length;
	}
	
	long time = Long.parseLong(times.get(0)[times.get(0).length-1]) + Long.parseLong(times.get(1)[times.get(0).length-1]);

	long nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
	//checking which cluster has the latest timestamp than the max of monotonic read timestamps
		int index = (int)(Math.random()*pci.length);
		boolean cond = true;
		while(cond){
			for(int i = 0; i<keys.length; i++){
				long maxMonRead = Math.max(Long.parseLong(times.get(0)[i].split("/")[1]), Long.parseLong(times.get(1)[i].split("/")[1]));
				if(maxMonRead <= Long.parseLong(times.get(index)[i].split("/")[0])){
					cond = false;
					continue;
				}else{
					cond = true;
					System.out.println("not monotonic read "+ index);
					index = (index+1)%pci.length;
					count++;
					break;
				}
					
			}
		}
	long args[] = new long[keys.length];
	for(int i = 0; i < args.length; i++){
		args[i] = Long.parseLong(times.get(index)[i].split("/")[0]);
	}
	long nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
	str = pci[index].get(keys, MONOTONIC, args);
	time = time + Long.parseLong(str[str.length-1]);
	System.out.println(str);
	//System.out.println("read key " + key + " value " + str);
	time = time + (nanos2 - nanos);
	System.out.println("time taken for monotonic read with key "+keys+" is "+ time);
	monotonicReadTime = monotonicReadTime + time;
	System.out.println("total time taken "+monotonicReadTime + " re reads " + count);
	return str;
}
	
	public String[] boundedStaleGet(int keys[]) throws RemoteException {
	//Stale Boundedness
	String str[];
	//0th element contains timestamp of data, 1st element contains countTS of staleboundedness (no of writes to a key)
	//2nd element contains time taken to get the timestamps
	ArrayList<String[]> times = new ArrayList<String[]>();
	
	int ind = (int)(Math.random()*pci.length);
	for(int i = 0; i < pci.length; i++){
		times.add(pci[ind].getTimeStamp(keys, STALEBOUNDEDNESS));
		ind = (ind+1)%pci.length;
	}
	
	long time = Long.parseLong(times.get(0)[times.get(0).length-1]) + Long.parseLong(times.get(1)[times.get(0).length-1]);

	long nanos = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
	//checking which cluster has the latest number of writs to a key

	int index = (int)(Math.random()*pci.length);
	boolean cond = true;
	while(cond){
		for(int i = 0; i < keys.length; i++){
			long maxNoWrites = Math.max(Long.parseLong(times.get(0)[i].split("/")[1]), Long.parseLong(times.get(1)[i].split("/")[1]));
			if(maxNoWrites - Long.parseLong(times.get(index)[i].split("/")[1]) <= StaleLimit){
				cond = false;
				continue;
			}else{
				cond = true;
				System.out.println("not stale bounded read "+ index);
				index = (index+1)%pci.length;
				count++;
			}
		}
		
	}
	long nanos2 = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());
	long args[] = new long[keys.length];
	for(int i = 0; i < args.length; i++){
		args[i] = Long.parseLong(times.get(index)[i].split("/")[0]);
	}
	str = pci[index].get(keys, STALEBOUNDEDNESS, args);
	time = time + Long.parseLong(str[str.length-1]);
	System.out.println(str);
	//System.out.println("read key " + key + " value " + str);
	time = time + (nanos2 - nanos);
	System.out.println("time taken for stalebounded read with key "+keys+" is "+ time);
	boundedstaleReadTime = boundedstaleReadTime + time;
	System.out.println("total time taken "+boundedstaleReadTime + " re reads"+ count);
	return str;
}
	
	public void print(){
		System.out.println();
		System.out.println("total time taken for strong reads "+strongReadTime);
		System.out.println("total time taken for eventual reads "+eventualReadTime);
		System.out.println("total time taken for monotonic reads "+monotonicReadTime + " re reads " + count);
		System.out.println("total time taken for boundedstale reads "+boundedstaleReadTime + " re reads"+ count);
	}
	
}
