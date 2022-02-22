package ksnet;

public class metaInfo{
	
	byte[]	route_type	;	//[	  2]
	byte[]	enc_type	;	//[	  1]
	byte[]	m_key_type	;	//[	  1]
	byte[]	trno		;	//[	 16]
	byte[]	key			;	//[	 16]
	byte[]	iv			;	//[	 16]
	byte[]	ctr_blocks	;	//[1024]
	int		rnd_idx		;
	
	private static metaInfo hmsg;
	
	private metaInfo(){}
	
	public static synchronized metaInfo getInstance(){
		if(hmsg == null){
			hmsg = new metaInfo();
		}
		return hmsg;
	}
	public void setRoute_type(byte[] route_type){
		this.route_type = route_type;
	}
	public byte[] getRoute_type(){
		return this.route_type;
	}
	
	
	public void setEnc_type(byte[] enc_type){
		this.enc_type	= enc_type;
	}
	public byte[] getEnc_type(){
		return this.enc_type;
	}
	
	
	public void setM_key_type(byte[] m_key_type){
		this.m_key_type	= m_key_type;
	}
	public byte[] getM_key_type(){
		return this.m_key_type;
	}
	
	
	public void setTrno(byte[] trno){
		this.trno		= trno;
	}
	public byte[] getTrno(){
		return this.trno;
	}
	
	
	public void setKey(byte[] key){
		this.key		= key;
	}
	public byte[] getKey(){
		return this.key;
	}
	
	
	public void setIv(byte[] iv){
		this.iv			= iv;
	}
	public byte[] getIv(){
		return this.iv;
	}
	
	
	public void setCtr_blocks(byte[] ctr_blocks){
		this.ctr_blocks	= ctr_blocks;
	}
	public byte[] getCtr_blocks(){
		return this.ctr_blocks;
	}


	public void setRnd_idx(int rnd_idx){
		this.rnd_idx	= rnd_idx;
	}
	public int getRnd_idx(){
		return this.rnd_idx;
	}
}
