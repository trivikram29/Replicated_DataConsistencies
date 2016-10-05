package Clients;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import Clients.Client.MyRunnable;
import Proxy.Client_Proxy_interface;

public class BaseballClient implements Runnable{
	static BaseballClient cl;
	static final int STRONGCONSISTENCY = 0;
	static final int EVENTUALCONSISTENCY = 1;
	static final int MONOTONIC = 2;
	static final int STALEBOUNDEDNESS = 3;
	static int keys[] = {0,1};
	public static void main(String args[])
	{
		cl = new BaseballClient();
		Client_Proxy_interface cpinterface = null;
		try	{
			cpinterface = (Client_Proxy_interface)(Naming.lookup("client_proxy"));
			int i = 0;
		//	long currentTimeinMillis = System.currentTimeMillis();
			while(i<2)
			{
				cpinterface.put(keys[i], Thread.currentThread().getName() + " index "+i);	
				i++;
			}
		//	System.out.println(Thread.currentThread().getName() + " time taken is " + (System.currentTimeMillis() - currentTimeinMillis));
		} catch (IOException | NotBoundException e) {
			e.printStackTrace();
		}
		cl.startWriteClients();
		cl.startReadClients();
		try {
			cpinterface.print();
		} catch ( RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	void startWriteClients(){
		Thread clientThread1 = new Thread(new BaseballClient());
		clientThread1.setName("Thread1");
		clientThread1.start();
		/*Thread clientThread2 = new Thread(new BaseballClient());
		clientThread2.setName("Thread2");
		clientThread2.start();*/
	}
	
	void startReadClients(){
		Thread clientThread1 = new Thread(new MyRunnable(STRONGCONSISTENCY));
		clientThread1.setName("strongReadThread");
		clientThread1.start();
		Thread clientThread2 = new Thread(new MyRunnable(EVENTUALCONSISTENCY));
		clientThread2.setName("EventualReadThread");
		clientThread2.start();
		Thread clientThread3 = new Thread(new MyRunnable(MONOTONIC));
		clientThread3.setName("MonotonicReadThread");
		clientThread3.start();
		Thread clientThread4 = new Thread(new MyRunnable(STALEBOUNDEDNESS));
		clientThread4.setName("boundedstaleReadThread");
		clientThread4.start();
		try {
			clientThread1.join();
			clientThread2.join();
			clientThread3.join();
			clientThread4.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class MyRunnable implements Runnable{
		int type = 0;
		public MyRunnable(int types) {
			type = types;
		}
		@Override
		public void run() {

			try	{
				Client_Proxy_interface cpinterface = (Client_Proxy_interface)(Naming.lookup("client_proxy"));
				int i = 300;
				long currentTimeinMillis = System.currentTimeMillis();
				while(i>0)
				{
					i--;
					String str[] = cpinterface.get(keys, type);
					System.out.print("read " );
					for(int j = 0; j < str.length; j++){
						System.out.print(" " + str[j]);
					}
					System.out.println();
				}
				//System.out.println(Thread.currentThread().getName() + " time taken is " + (System.currentTimeMillis() - currentTimeinMillis));
			} catch (IOException | NotBoundException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public void run() {
		makeConnection();	
	}
	
	void makeConnection(){
	try	{
			Client_Proxy_interface cpinterface = (Client_Proxy_interface)(Naming.lookup("client_proxy"));
			int i = 0;
			long currentTimeinMillis = System.currentTimeMillis();
			while(i<=200)
			{
				cpinterface.put((int)(Math.random()*2), " value "+i);
				i++;
			}
			System.out.println(Thread.currentThread().getName() + " time taken is " + (System.currentTimeMillis() - currentTimeinMillis));
		} catch (IOException | NotBoundException e) {
			e.printStackTrace();
		}
		
	}

}
