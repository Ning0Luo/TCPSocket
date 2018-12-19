import java.util.*;

public class DynamicWindow {

	private Queue<Transport> BufferedPackage;
	private Queue<Long> SendingTime;
	private double capacity  = 0;
	private int writePos  = 0;
	private int Num = 0;
	private int lastSeq = -1;
	private double threshold;
	boolean ifCongestionControl = false;
	
	
	 public DynamicWindow( ) {
		   
	        this.capacity = 1;
	        this.BufferedPackage = new LinkedList<Transport>();
	        this.SendingTime = new LinkedList<Long>();
	        this. threshold = 64000;
	    }
	 
	 public void setSize(int s) {
		 this.capacity = s;
	 }
	 
	 public void GetCongestionControl() {
		 this.ifCongestionControl = true;
	 }
	
	 public boolean available() {
	        return ((int)(this.capacity) - this.Num) > 0;
	    }
	 
	 public boolean empty() {
		 return this.NumOfPackage() == 0;
	 }
	 
	 public boolean add(Transport element, long Time){

	          if(Num < (int)capacity){
//	          if(writePos >= capacity){
//		                writePos = 0;
//		           }
//		        BufferedPackage[writePos] = element;
//	            SendingTime[writePos] = Time;
//	            //////System.out.println("time"+ Time);
//	            lastSeq = element.getSeqNum();
//	            writePos++;
//	            Num++;
//	            if(writePos >= capacity){
//	                writePos = 0;
//	           }
	        	lastSeq = element.getSeqNum();
	        	SendingTime.add(Time);  
	        	BufferedPackage.add(element);  
	        	Num ++;
	        	  
	            return true;
	        }

	        return false;
	    }
	
	 
	 public Transport RemoveandAdd(long time) {
        if(Num == 0){
            return null;
        }
//        int nextSlot = writePos - Num;
//        if(nextSlot < 0){
//            nextSlot += capacity;
//        }
       
        Transport nextpackage = BufferedPackage.remove();
        BufferedPackage.add(nextpackage);
        long t = SendingTime.remove();
        SendingTime.add(time);
        
        
        return nextpackage;
    }
	 
	 
	 public Transport remove() {
	        if(Num == 0){
	            return null;
	        }
//	        int nextSlot = writePos - Num;
//	        if(nextSlot < 0){
//	            nextSlot += capacity;
//	        }
	       
	        Transport nextpackage = BufferedPackage.remove();
	        long time = SendingTime.remove();
	        Num--;
	        return nextpackage;
	    }
	 
	 public int NumOfPackage() {
		 return Num;
	 }
	 
	 public double getsize() {
		 return capacity;
	 }
	 
	 
	 public long GetTime() {
//		 if(Num == 0) {
//			 return -1;
//		 }
//		 int nextSlot = writePos - Num;
//	        if(nextSlot < 0){
//	            nextSlot += capacity;
//	        }
//		  
//		 return SendingTime[nextSlot];
		 return SendingTime.peek();
	 }
	 
	 
	 public int GetNextSeq() {
		 return this.lastSeq+1;
	 }
	 
	 
	 public int GetFirstSeq() {
//	    int nextSlot = writePos - Num;
//		if (nextSlot < 0) {
//			nextSlot += capacity;
//		}
		Transport tp = BufferedPackage.peek();
	    return tp.getSeqNum();
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
	
	
	
	
	public void Increase() {
		if(!ifCongestionControl) return;
		
		if(capacity < this.threshold) {
			capacity = capacity +1;
			
		}
		else capacity = capacity +1/capacity;

		
	}
	
	
	public void shrink() {
		if(!ifCongestionControl) return;
		//System.out.println("--------------shrink");
		//System.out.println(capacity);
		if(capacity/2 > 0) {
		 this.threshold = this.capacity/2;
	     capacity = this.capacity/2;
	     } else capacity =1;
	}
	
	public void reset() {
		if(!ifCongestionControl) return;
	//	System.out.println("reset..............window");
		this.threshold = this.capacity/2;
		this.capacity = 1;
	}
	
}