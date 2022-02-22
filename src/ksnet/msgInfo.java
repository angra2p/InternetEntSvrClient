package ksnet;

public class msgInfo{
	
	char 	ctype;
	int		clen;
	int		elen;
	byte[]	mbuf = new byte[20480];

	private static msgInfo msg;
	
	private msgInfo(){}
	
	public static synchronized msgInfo getInstance(){
		if(msg == null){
			msg = new msgInfo();
		}
		return msg;
	}
	public void setCtype(char ctype){
		this.ctype	= ctype;
	}
	public char getCtype(){
		return this.ctype;
	}
	public void setClen(int clen){
		this.clen	= clen;
	}
	public int getClen(){
		return this.clen;
	}
	public void setElen(int elen){
		this.elen	= elen;
	}
	public int getElen(){
		return this.elen;
	}
}
