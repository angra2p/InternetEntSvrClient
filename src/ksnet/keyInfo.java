package ksnet;

public class keyInfo{
	
	byte[]	p_kbuf	;	//[2048]
	byte[]	e_kbuf	;	//[2048]

	private static keyInfo kinfo;

	private keyInfo(){}

	public static synchronized keyInfo getInstance(){
		if(kinfo == null){
			kinfo	= new keyInfo();
		}
		return kinfo;
	}
	public void setP_kbuf(byte[] p_kbuf){
		this.p_kbuf	= p_kbuf;
	}
	public byte[] getP_kbuf(){
		return this.p_kbuf;
	}
	public void setE_kbuf(byte[] e_kbuf){
		this.e_kbuf	= e_kbuf;
	}
	public byte[] getE_kbuf(){
		return this.e_kbuf;
	}
}
