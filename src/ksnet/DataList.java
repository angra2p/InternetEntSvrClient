package ksnet;

public class DataList{	
	public String f_type	;
	public String send_cd	;
	public String recv_cd	;
	public String filename	;
	public String ret_cd	;
	public String end_time	;
	
	public DataList(SendFileFormat fileformat){
		StringBuffer sb = new StringBuffer() ;
		sb.append(fileformat.ebc_flag).append(fileformat.flag).append(fileformat.work_dt).append(fileformat.sep_1).append(fileformat.send_cd).append(fileformat.recv_cd);
		sb.append(fileformat.info_cd).append(fileformat.sep_2).append(fileformat.f_seq);
		this.f_type 	= fileformat.info_cd.toString() ;
		this.send_cd	= fileformat.send_cd.toString() ;
		this.recv_cd	= fileformat.recv_cd.toString() ;
		this.filename	= sb.toString()		;
		this.ret_cd		= ""				;
		this.end_time	= ""				;
	}
	
	public String getF_type() {
		return f_type;
	}

	public void setF_type(String f_type) {
		this.f_type = f_type;
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

	public String getFilename() {
		return this.filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getRet_cd() {
		return ret_cd;
	}

	public void setRet_cd(String ret_cd) {
		this.ret_cd = ret_cd;
	}
	public String getEnd_time() {
		return end_time;
	}
	public void setEnd_time(String time) {
		this.end_time	= time;
	}
}

