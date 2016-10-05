package Proxy;
import java.rmi.*;

public interface Client_Proxy_interface extends Remote {

	public int put(int key, String value) throws RemoteException;
	public String[] get(int key[], int type) throws RemoteException;
	public void print() throws RemoteException;
}
