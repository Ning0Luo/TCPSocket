import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;







/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet TCP manager</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */
public class TCPManager  {
    private Node node;
    private int addr;
    private Manager manager;
    private ArrayList<Packet>  packets; 
    private HashMap connections; 
    private static final byte dummy[] = new byte[0];  ///??????????????????
    private ArrayList<Packet>  ToProcess;

    public TCPManager(Node node, int addr, Manager manager) {
        this.node = node;
        this.addr = addr;
        this.manager = manager;
        this.packets= new ArrayList<Packet>();
        this.connections = new HashMap< String,TCPSock>() ;
    }

    /**
	 * Start this TCP manager
	 */
	public void start() {
		     
//		TimeoutMonitor monitor = new TimeoutMonitor(this.sortedEvents);
//		new Thread(monitor).start();
	}
		
//		while (true) {
//			synchronized (packets) {
//				Iterator iter = this.packets.iterator();
//				while (iter.hasNext()) {
//					ToProcess.add((Packet) iter.next());
//				}
//			}
//			dispatch(ToProcess);
//		
	
	
	public void dispatch(ArrayList<Packet> ToProcess) {
		Iterator iter = this.ToProcess.iterator();
		while (iter.hasNext()) {
			dispatch((Packet) iter.next());
		}
	}
    
	public void dispatch(Packet p) {
		int srcA = p.getSrc();
		int desA = p.getDest();
		Transport tp = demultiplex(p);
		int srcP = tp.getSrcPort();
		int desP = tp.getDestPort();
		int Type = tp.getType();
		TCPSock socket_1 = matching(srcA, srcP, desA, desP);
		TCPSock socket_2 = matching(-1, -1, desA, desP);

		if (socket_1 != null ) {
			socket_1.receive(p);
			return;
		}

		if (socket_2 != null && socket_2.isListening() && tp.getType() == Transport.SYN &&  (!socket_2.isFull())) {
			socket_2.receive(p);
		    return;
		}
		byte[] payload = new byte[0];
		Transport FINPackage = new Transport(desP, srcP, Transport.FIN, 0, tp.getSeqNum(), payload);
		
		this.SendPacket(FINPackage.pack(), desA);
		
		
	}
	
	
	public TCPSock matching(int srcA, int srcP, int desA, int desP) {
		int[] tuple = {srcA,srcP, desA,desP};
		
		return (TCPSock) connections.get(Arrays.toString(tuple));
	}
	
	
	public Transport demultiplex(Packet p) {
		return Transport.unpack(p.getPayload());		
	} 
	

    // called by Node
    
//    public void ToDispatch(Packet packet) {
//		synchronized (packets) {
//			packets.add(packet);
//		}
//    }
    
    
    public void SendPacket(byte[] tp, int desA) {
    	node.sendSegment(this.addr,desA,Protocol.TRANSPORT_PKT,tp);
    	
    }
    

    public int AddRecords(TCPSock s, int srcA, int srcP, int desP ) {
    	int[] tuple = {srcA, srcP, addr, desP};
		synchronized (connections) {
			if (connections.containsKey(Arrays.toString(tuple)))
				return -1;
			connections.put(Arrays.toString(tuple), s);
	       // ////System.out.println("recorded");
	        ////System.out.println(connections.get(tuple));

		}
		
		return 0;
    	
    }
    
    
    
    public void removerecords ( int srcA, int srcP, int desP) {
    	int[] tuple = {srcA, srcP, addr, desP};
    	synchronized(connections) {
    		if(connections.containsKey(Arrays.toString(tuple))) return;
            connections.remove(Arrays.toString(tuple)); 
    	}
    	
    }
    
  
    public void addTimer(long deltaT, Callback cb) {
		try {
			//System.out.println("addTimer: TCPSOCKman");
			this.manager.addTimer(this.addr, deltaT, cb);
		} catch (Exception e) {
			System.err.println("gg");;
		}
	}
    
    public long now() {
    	return this.manager.now();
    }
	
	
    
    /*
     * Begin socket API
     */

    /**
     * Create a socket
     *
     * @return TCPSock the newly created socket, which is not yet bound to
     *                 a local port
     */
    public TCPSock socket() {
    	TCPSock socket = new TCPSock();
    	socket.SetManager(this); 
    	return socket;
        
        // return a socket with this tcpmanager
    }
    

    /*
     * End Socket API
     */
}

//class TimeoutMonitor implements Runnable {
//	private SortedEventQueue sortedEvents;
//	private int testInt;
//	
//	public TimeoutMonitor(SortedEventQueue sortedEvents) {
//		this.sortedEvents = sortedEvents;
//	}
//	
//	@Override
//	public void run() {
//		Event nextEvent = null;
//		while (true) {
//		    long now = Utility.fishTime();
//		    this.testInt++;
//		    
//		    boolean notClear = false;
//		    synchronized (this.sortedEvents) {
//				if (!this.sortedEvents.isEmpty()
//					&& (nextEvent = this.sortedEvents.getNextEvent()).timeToOccur() <= now)
//					notClear = true;
//			}
//		   
//			while (notClear) {
//
//				synchronized (this.sortedEvents) {
//					this.sortedEvents.removeNextEvent();
//				}
//				//System.out.println("remove timer");
//				try {
//					nextEvent.callback().invoke();
//				} catch (Exception e) {
//					System.err.println("Exception while trying to invoke method in Emulator. Error: " + e);
//					e.printStackTrace();
//				}
//				
//				notClear = false;
//				synchronized (this.sortedEvents) {
//					if (!this.sortedEvents.isEmpty()
//						&& (nextEvent = this.sortedEvents.getNextEvent()).timeToOccur() <= now)
//						notClear = true;
//				}
//			}
//			
//		}
//	}
//}

//class TimeOut extends Thread {
//	SortedEventQueue sortedEvents;
//
//	public TimeOut(SortedEventQueue sortedEvents) {
//		this.sortedEvents = sortedEvents;
//
//	}
//
//	public void run() {
//		while (true) {
//
//			long now = Utility.fishTime();
//			Event nextEvent = null;
//
//			// Run all due events
//			while (!this.sortedEvents.isEmpty()
//					&& (nextEvent = this.sortedEvents.getNextEvent()).timeToOccur() <= now) {
//
//				this.sortedEvents.removeNextEvent();
//				try {
//					nextEvent.callback().invoke();
//				} catch (Exception e) {
//					System.err.println("Exception while trying to invoke method in Emulator. Error: " + e);
//					e.printStackTrace();
//				}
//
//			}
//		}
//	}
//
//}



