import java.util.*;
import java.util.concurrent.TimeoutException;

import javax.lang.model.element.Element;
import javax.print.attribute.standard.RequestingUserName;

import org.omg.PortableInterceptor.TRANSPORT_RETRY;
import org.w3c.dom.css.ElementCSSInlineStyle;

import java.awt.Window;
import java.lang.reflect.Method;
import java.sql.Time;


/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet socket implementation</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */

public class TCPSock {
   static Set<Integer> OccupiedPorts = new HashSet<>();
	
    // TCP socket states
    enum State {
        // protocol states
    	
        CLOSED,
        LISTEN,
        SYN_SENT,
        ESTABLISHED,
        SHUTDOWN // close requested, FIN not sent (due to unsent data in queue)
    }
    private int SentAck;  // the expected sequence number of package
    private int RecAck;  // the ack number received



    private State state;
    private int localport;
    private TCPManager tcpman;
    private Queue<Packet> SYNpackets;
    private int backlog;
    private buffer ReadBuffer;
    private buffer WriteBuffer;
    private long timeout; 
    int remotePort;
    int remoteAddress;
    private boolean dataWaiting;
    private DynamicWindow slidingwindow;
    private int windowsize = 3;
    private int Movement = 0;
    private int DuplicatedAck = 0;
    
  
    
    
    
    
    //a connection queue 
    // tcp manager 
    // port number 
    // backlog : the number of pending connection 

    public TCPSock() {
          this.state = State.CLOSED;
          this.localport = -1;
          this.SYNpackets= new LinkedList<Packet>();  
          SentAck = 1;
          RecAck = 1;
          timeout = 10;
          ReadBuffer = new buffer(80000);
          WriteBuffer  = new buffer(80000);
          this.dataWaiting = true;
          this.slidingwindow = new DynamicWindow();
          this.slidingwindow.setSize(3);
          
    }
    
    
    
 

    /*
     * The following are the socket APIs of TCP transport service.
     * All APIs are NON-BLOCKING.
     */

    /**
     * Bind a socket to a local port
     *
     * @param localPort int local port number to bind the socket to
     * @return int 0 on success, -1 otherwise
     */
    // public int return
    
    public int SetManager(TCPManager tcpm) {
    	this.tcpman = tcpm;
    	return 0;
    }
    
    
    
     
    
    public int bind(int localPort) {   // then add this to the records...... ...................... 
   // 	Systm.out.println(OccupiedPorts.add(localPort));
    //	////System.out.println(state);
      if (state == State.CLOSED && OccupiedPorts.add(localPort)) {
    	   this.localport = localPort;
    //	   ////System.out.println(localPort);
    	   return 0;
    	//   return tcpman.AddRecords(this, -1, -1, localPort);
      }
       else return -1;
    }

    /**
     * Listen for connections on a socket
     * @param backlog int Maximum number of pending connections
     * @return int 0 on success, -1 otherwise
     */
    public int listen(int backlog) {
        //System.out.println("listen");
        this.backlog = 5;
     //   System.out.println(backlog);

    	if(state != State.CLOSED) return -1;
        if(localport ==-1) return -1;
        if (tcpman.AddRecords(this, -1, -1, this.localport) == -1) return -1;
        state = state.LISTEN;
        this.backlog = backlog;
        this.remoteAddress= -1;
        this.remotePort = -1;
        return 0;    
    }
    
    
    
   
    

    /**
     * Accept a connection on a socket
     *
     * @return TCPSock The first established connection on the request queue
     */
    public TCPSock accept() {   
    	if(state != State.LISTEN) return null; //state check !!!!!!!!!!!!!!!!!
    	if(SYNpackets.isEmpty()) return null;
    	Packet p;
    	synchronized (SYNpackets) {
            p =SYNpackets.remove();
		}
    	return HandleSYN(p);	  
    }
    
    
    public boolean isFull() {
    	//System.out.println(backlog);
    	return backlog == 0;
    }
    
    public TCPSock HandleSYN(Packet p) {
    	backlog++;
    	if (p == null) return null;
    	int srcA = p.getSrc();
		int desA = p.getDest();
		Transport tp = Transport.unpack(p.getPayload());
		int srcP = tp.getSrcPort();
		int desP = tp.getDestPort();
		int Type = tp.getType();
		int seqNum  = tp.getSeqNum();
       
		if (Type != Transport.SYN ||desP != this.localport) return null; // double check??

		TCPSock sock = new TCPSock();
		sock.localport = this.localport;
		sock.SetManager(this.tcpman);	
		sock.SetDestination(srcP, srcA);
	    sock.setSentAck(seqNum+1);

	
	    int isexistedConnection = sock.tcpman.AddRecords(sock, srcA, srcP, desP);	

	    if (isexistedConnection == -1) return null; 
	    
	    byte[] payload = new byte[0];

	    Transport ackpacket = new Transport(this.localport, srcP, Transport.ACK, windowsize, seqNum+1, payload );// window size ...?????????
	    
	    SendPacket(ackpacket,srcA);// send back 
		sock.Establish();

	    
	    return sock; 
    }
    
    
    
   
    
    public int getsentAck() {
    	return  SentAck;
    }
    
    public int getRecAck() {
    	return RecAck;
    }
        
    private void setSentAck(int seq) {
   // need to check the state !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    	this.SentAck = seq;
    }
    
//    private void setRecAck(int seq) {
//    	this.RecAck = seq;
//    }
    
    
    private void  SetDestination(int p, int a) {
    	 this.remotePort = p;
    	 this.remoteAddress= a;
    }
    
    
    
    private void Establish() {
    	this.state = State.ESTABLISHED;
    }

    public boolean isListening() {
    	return (state == State.LISTEN);
    }
  
    public boolean isConnectionPending() {
        return (state == State.SYN_SENT);
    }

    public boolean isClosed() {
        return (state == State.CLOSED);
    }

    public boolean isConnected() {
        return (state == State.ESTABLISHED);
    }

    public boolean isClosurePending() {
        return (state == State.SHUTDOWN);
    }

    /**
     * Initiate connection to a remote socket
     *
     * @param destAddr int Destination node address
     * @param destPort int Destination port
     * @return int 0 on success, -1 otherwise
     */
    
    
    public int connect(int destAddr, int destPort) {

    	//if( tcpman.AddRecords(this, destAddr, destPort, this.port) == -1) return -1; 	
    	this.remotePort = destPort;
    	this.remoteAddress = destAddr;
    	if(tcpman.AddRecords(this, remoteAddress, remotePort, localport) == -1) return -1;
        byte[] payload = new byte[0];
        Transport synpacket = new Transport(this.localport, destPort, Transport.SYN, windowsize, 1, payload);  // session seq stars from 1

 	    SendPacket(synpacket,destAddr);
		System.out.print("S");

 	    this.state = State.SYN_SENT;
         

    	//this.SentSeq =this.SentSeq +1 ;
    	this.addTimer();

    	return 0;
    	//AddTimer(System.currentTimeMillis(), )
    	//ifSpackets.isEmpty()) return -1;
    	//long t = System.currentTimeMillis();
    	//while (p!= null && t- System.currentTimeMillis()< 2000)type name = new type();
    }
    
    
    public void receive(Packet p) { 


    	if(state == State.CLOSED) {
    		return;
    	}
    	Transport tp = Transport.unpack(p.getPayload());
		int Type = tp.getType();

		switch (Type) {
		case Transport.ACK: 
			String c = HandleAckAPacket(tp);
			System.out.print(c);
			break;
		case Transport.SYN:

			if (state == State.LISTEN) {
				SYNpackets.add(p);	
				backlog--;
		    	System.out.print("S");
			}
			if(state == State.ESTABLISHED) sendAck(remotePort, tp.getSeqNum()+1, remoteAddress);
			break;
		
		case Transport.DATA:
			if(state == State.ESTABLISHED) {
				String b = HandleDataPacket(tp);
			    System.out.print(b);
			    }
            break;
		
		case Transport.FIN:
			this.release();
			System.out.print("F");
			break;
		default:
			break;
		} 
    }
    
    
    
    public void sendAck(int destPort, int Acknum, int desA) {
    
    	byte[] payload = new byte[0];

        Transport ackpacket = new Transport(this.localport, destPort, Transport.ACK, 1, Acknum, payload );  // session seq stars from 1
        ////System.out.println("Send ACK");
        SendPacket(ackpacket, desA);
        this.SentAck = Acknum;
       
        
	}
    
    
    
    
    public String HandleAckAPacket(Transport tp) {
		if (this.RecAck == tp.getSeqNum()) {
			DuplicatedAck ++;
		}

    	this.RecAck = tp.getSeqNum();
    	String c = null;
		////System.out.println("Receive ACK" + RecAck);
		////System.out.println("movement:" + this.Movement);
		if(this.RecAck >= slidingwindow.GetFirstSeq()) {
			c = ":";
		    this.Movement = slidingwindow.Slide(RecAck);
			if (this.state == State.SYN_SENT) {
				this.state = State.ESTABLISHED;
			}
		   SendNext_W();
		   slidingwindow.Increase();

		} 
		
		if(c == null) {
			DuplicatedAck ++;
		}
		
		if(DuplicatedAck > 3) {
			slidingwindow.shrink();
			DuplicatedAck = 0;
		}
		
		return c;
    }
    
    
    public void ReSend () {
		////System.out.println("resent package---------------");
//
		for (int i = 0; i < slidingwindow.NumOfPackage(); i++) {
			Transport tp = slidingwindow.RemoveandAdd(tcpman.now());
			////System.out.println("resending " + tp.getSeqNum());
			System.out.print("!");
			tcpman.SendPacket(tp.pack(), remoteAddress);
		}
	
    }
	
    
    
    public void SendNext_W() {  // send new data as much as possible 
   
    	while(slidingwindow.available() && dataWaiting == false) {
    		SendNext();
    	}
    	
    	if (slidingwindow.empty() && this.isClosurePending() && dataWaiting == true) {
    		byte[] payload = new byte[0];
    		Transport tp = new Transport(this.localport, this.remotePort, Transport.FIN, windowsize, this.slidingwindow.GetNextSeq(), payload);
			System.out.println("F");
        	tcpman.SendPacket(tp.pack(), remoteAddress);
     		this.state = State.CLOSED;
    	}
    }
    
    public void SendNext() {	    
       byte[] payload = WriteBuffer.PackAggressive();
       if(payload.length == 0) {
    	   ////System.out.println("completng sending ----------------");
    	   this.dataWaiting = true;
    	   return;
       }
       ////System.out.println("payload length="+payload.length);
       ////System.out.println(slidingwindow.GetNextSeq()+"//////////////////");
       Transport tp = new Transport(this.localport, this.remotePort, Transport.DATA, windowsize, slidingwindow.GetNextSeq(), payload);
       SendPacket(tp, remoteAddress);
	   System.out.print(".");

    }
        
    public String HandleDataPacket(Transport tp) {
    	////System.out.println("receive package" + tp.getSeqNum());
       
    	if(this.state != State.ESTABLISHED) return "Not Connected";
		if(tp.getSeqNum() != SentAck) {
			sendAck(this.remotePort, this.SentAck, this.remoteAddress);
			return "!";
		} 
		ReadBuffer.write(tp.getPayload(), 0,tp.getPayload().length);
        sendAck(this.remotePort, tp.getSeqNum()+1, this.remoteAddress);
        return ".";
    }

    /**
     * Initiate closure of a connection (graceful shutdown)
     */
    public void close() {
      
    	state = State.SHUTDOWN;     
    	
    }

    /**
     * Release a connection immediately (abortive shutdown)
     */
    public void release() {
    	state = State.CLOSED;     
    }
    
    
    private void addTimer() {
		try {
			this.Movement = 0;
			Method method = Callback.getMethod("TimeOut", this, null);
			Callback cb = new Callback(method, this, null);
			this.tcpman.addTimer(this.timeout*1000, cb);
		} catch (Exception e) {
			System.err.println(e);
		}
    	
 	}
    
    
    
    
   public void TimeOut() {
	  
	   ////System.out.println("timeout invoke");
	   
	   if (this.isClosed()||(slidingwindow.empty() && this.isClosurePending() && dataWaiting == true)) 
		{
		////System.out.println("timer cancel");
		return;
	 }
	   
	   if (slidingwindow.empty()) {
		   ////System.out.println("slidingwindow.empty--------------------");
		   ////System.out.println(this.state);
		   ////System.out.println(this.isClosurePending() + " " + (dataWaiting == true));
		   addTimer();
		   return;
	   }
		if (this.Movement == 0) {
			////System.out.println("resend--------------------");
			ReSend();
			//slidingwindow.reset();

			
		}

		

		////System.out.println(slidingwindow.NumOfPackage() + " " + this.Movement);
		this.addTimer();
	}
    
   
     
     

    /**
     * Write to the socket up to len bytes from the buffer buf starting at
     * position pos.
     *
     * @param buf byte[] the buffer to write from
     * @param pos int starting position in buffer
     * @param len int number of bytes to write
     * @return int on success, the number of bytes written, which may be smaller
     *             than len; on failure, -1
     */
    public int write(byte[] buf, int pos, int len) {
    	
		int length = this.WriteBuffer.write(buf, pos, len);
		
		if(this.dataWaiting == true) {
			this.dataWaiting = false;
			SendNext_W();
		}
		
	    return length;
	     	
    }
    
    
    

    /**
     * Read from the socket up to len bytes into the buffer buf starting at
     * position pos.
     *
     * @param buf byte[] the buffer
     * @param pos int starting position in buffer
     * @param len int number of bytes to read
     * @return int on success, the number of bytes read, which may be smaller
     *             than len; on failure, -1
     */
  
    
    private void SendPacket(Transport tp, int desA) {
    	byte[] payload = tp.pack();
    	
    	if(tp.getType()!= Transport.ACK) {
    		int SentSeq = tp.getSeqNum();
        	long SendTime = this.tcpman.now();
        	boolean a = slidingwindow.add(tp, SendTime);
        	//////System.out.println(a+"bbbbbbbbbbbbb"+ slidingwindow.NumOfPackage());
        	
        	
        	if(a) {
        		//////System.out.println("send data:  "+ SentSeq );
        		tcpman.SendPacket(payload, desA);	        		
        	}
        	
        	return;
    		////System.out.print(SentSeq);
    	}
    	tcpman.SendPacket(payload, desA);

    	
    	
    	
    }
    
    public int read(byte[] buf, int pos, int len) {
    	 return this.ReadBuffer.read(buf, pos, len);
    }

    /*
     * End of socket API
     */
}


class buffer {
    private byte[] elements = null;

    private int capacity  = 0;
    private int writePos  = 0;
    private int available = 0;

    public buffer(int capacity) {
        this.capacity = capacity;
        this.elements = new byte[capacity];
    }

    public void reset() {
        this.writePos = 0;
        this.available = 0;
    }

    public int capacity() { return this.capacity; }
    public int available(){ return this.available; }

    public int remainingCapacity() {
        return this.capacity - this.available;
    }

    private boolean put(byte element){

        if(available < capacity){
            if(writePos >= capacity){
                writePos = 0;
            }
            elements[writePos] = element;
            writePos++;
            available++;
            return true;
        }

        return false;
    }

    private Byte take() {
        if(available == 0){
            return null;
        }
        int nextSlot = writePos - available;
        if(nextSlot < 0){
            nextSlot += capacity;
        }
        byte nextbyte = elements[nextSlot];
        available--;
        return nextbyte;
    }
    
    public int write(byte[] buf, int pos, int len) {	
    	if (buf.length -1 < pos + len) return -1;
    	////////System.out.println("writing into buffer");

    	for (int i  = 0; i < len; i++) {
    		if(!this.put(buf[pos+i]))
    			return i;
    	} 
    	return len;   	
    } 
    
    
    public int read(byte[] buf, int pos, int len) {
        //	//////System.out.println(buf.length  < pos + len);
        	
        	if (buf.length  < pos + len) return -1;	
        	for (int i  = 0; i < len; i++) {
        		Byte b = this.take();
        		if (b == null) return i;
        		else buf[pos + i] = (byte)b;
        	}
        	
        	return len;
        }
    
  public byte[] PackAggressive() {
	   int len = this.available;
	   if (len == 0) return new byte[0];
	   if(len > Transport.MAX_PAYLOAD_SIZE) len = Transport.MAX_PAYLOAD_SIZE;
       byte[] payload  = new byte[len];
       int len_read = read(payload, 0, len);
       if (len !=  len_read) {
    	   //////System.out.println("pack fail");
    	   return  new byte[0];
       }
       return payload;
	   
  }
      
}

class window {

	private Transport[] BufferedPackage = null;
	private long[] SendingTime = null;
	private int capacity  = 0;
	private int writePos  = 0;
	private int Num = 0;
	private int lastSeq = -1;
	
	
	
	 public window(int capacity) {
	        this.capacity = capacity;
	        this.BufferedPackage = new Transport[capacity];
	        this.SendingTime = new long[capacity];
	    }
	
	 public boolean available() {
	        return (this.capacity - this.Num) > 0;
	    }
	 
	 public boolean empty() {
		 return this.NumOfPackage() == 0;
	 }
	 
	 public boolean add(Transport element, long Time){

	          if(Num < capacity){
	          if(writePos >= capacity){
		                writePos = 0;
		           }
		        BufferedPackage[writePos] = element;
	            SendingTime[writePos] = Time;
	            //////System.out.println("time"+ Time);
	            lastSeq = element.getSeqNum();
	            writePos++;
	            Num++;
	            if(writePos >= capacity){
	                writePos = 0;
	           }
	            return true;
	        }

	        return false;
	    }
	
	 public Transport remove() {
	        if(Num == 0){
	            return null;
	        }
	        int nextSlot = writePos - Num;
	        if(nextSlot < 0){
	            nextSlot += capacity;
	        }
	       
	        Transport nextpackage = BufferedPackage[nextSlot];
	        Num--;
	        return nextpackage;
	    }
	 
	 public int NumOfPackage() {
		 return Num;
	 }
	 
	 public long GetTime() {
		 if(Num == 0) {
			 return -1;
		 }
		 int nextSlot = writePos - Num;
	        if(nextSlot < 0){
	            nextSlot += capacity;
	        }
		  
		 return SendingTime[nextSlot];
	 }
	 
	 
	 public int GetNextSeq() {
		 return this.lastSeq+1;
	 }
	 
	 
	 public int GetFirstSeq() {
	    int nextSlot = writePos - Num;
		if (nextSlot < 0) {
			nextSlot += capacity;
		}
		return BufferedPackage[nextSlot].getSeqNum();
	 } 
   
	public int Slide(int ack) {
		//////System.out.println("unack " + Num);
		//////System.out.println("Receive ACK "+ ack);

//		//System.err.println("this first seq"+ this.GetFirstSeq());
		int i = 0;
		while(this.NumOfPackage() > 0 && this.GetFirstSeq() < ack) {
			Transport tp = this.remove();
			i++;
//			System.err.println(this.GetFirstSeq());
//			////System.out.println(tp);
//			////System.out.println(tp.getSeqNum());
		}
		return i;
	}
	
}



	
	






