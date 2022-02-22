package ksnet;

public class SendFileFormat {
	public String ebc_flag; /* A:ascii, E:ebcdic */
	public String flag; 	/* BRQ */
	public String work_dt; 	/* YYYYMMDD */
	public String sep_1; 	/* "_" */
	public String send_cd; 
	public String recv_cd; 
	public String info_cd; 
	public String sep_2; 	/* "." */
	public String f_seq; 	

	public SendFileFormat(String filename) {
		try{
			this.ebc_flag	= filename.substring(0 , 1 );
			this.flag		= filename.substring(1 , 4 );
			this.work_dt	= filename.substring(4 , 12);
			this.sep_1		= filename.substring(12, 13);
			this.send_cd	= filename.substring(13, 17);
			this.recv_cd 	= filename.substring(17, 21);
			this.info_cd 	= filename.substring(21, 24);
			this.sep_2 		= filename.substring(24, 25);
			this.f_seq 		= filename.substring(25, 28);
		}catch(Exception e){
			Util.WriteLog("SendFileFormat", "error : ["+e.getMessage()+"]");
		}
	}
	public String toString(){
		StringBuilder	sb = new StringBuilder();
		sb.append(this.ebc_flag).append(this.flag).append(this.work_dt).append(this.sep_1).append(this.send_cd);
		sb.append(this.recv_cd).append(this.info_cd).append(this.sep_2).append(this.f_seq);
		
		return sb.toString();
	}
	public String getEbc_flag() {
		return ebc_flag;
	}

	public void setEbc_flag(String ebc_flag) {
		this.ebc_flag = ebc_flag;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getWork_dt() {
		return work_dt;
	}

	public void setWork_dt(String work_dt) {
		this.work_dt = work_dt;
	}

	public String getSep_1() {
		return sep_1;
	}

	public void setSep_1(String sep_1) {
		this.sep_1 = sep_1;
	}

	public String getSend_cd() {
		return send_cd;
	}

	public void setSend_cd(String send_cd) {
		this.send_cd = send_cd;
	}

	public String getRecv_cd() {
		return recv_cd;
	}

	public void setRecv_cd(String recv_cd) {
		this.recv_cd = recv_cd;
	}

	public String getInfo_cd() {
		return info_cd;
	}

	public void setInfo_cd(String info_cd) {
		this.info_cd = info_cd;
	}

	public String getSep_2() {
		return sep_2;
	}

	public void setSep_2(String sep_2) {
		this.sep_2 = sep_2;
	}

	public String getF_seq() {
		return f_seq;
	}

	public void setF_seq(String f_seq) {
		this.f_seq = f_seq;
	}
	
	
}
