package Clusters;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.locks.Lock;

public interface Proxy_Cluster_interface extends Remote {
	public int put(int key[], String value[]) throws RemoteException;
	public String[] get(int key[], int type, long arg[]) throws RemoteException;
	public long takeLock(int key[]) throws RemoteException;
	public void releaseLock(int key[]) throws RemoteException;
	public String[] getTimeStamp(int key[], int type) throws RemoteException;
}
