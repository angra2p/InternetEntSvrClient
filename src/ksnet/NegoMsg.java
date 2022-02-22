package ksnet;

public class NegoMsg {
	public String f_type		;	
	public String msg_type		;	
	public String send_cd		;	
	public String recv_cd		;	
	public String send_tm		;	
	public String rec_cnt		;	
	public String block_no		;	
	public String seq_no		;	
	public String return_cd		;	
	public String rec_len		;	
	public String sr_ty			;	
	public String miss_cnt		;	
	public String from_date		;	
	public String to_date		;	
	public String req_type		;	
	public String filler		;
	public String ebc_yn		;
	public String enc_yn		;
	public String zip_yn		;
	public String bin_len		;
	public String enc_rem		;
	public String filler_1		;
	public String filler_2		;
	
	public void NegoMsg(){}

	public String NegoMsg(String f_type, String msg_type, String send_cd, String recv_cd, int rec_cnt, int block_no, int seq_no, int rec_len, String sr_ty, int miss_cnt, String from_date, String to_date, String req_type, String filler, long f_size, int bin_len){
		StringBuffer sb = new StringBuffer();
		String.format("%96s", this.toString());

		this.f_type 	= f_type;			sb.append(this.f_type);
		
		this.msg_type 	= msg_type;			sb.append(this.msg_type);
		
		this.send_cd 	= String.format("%-10s", send_cd);		/* right padding " " */
		sb.append(this.send_cd);
		
		this.recv_cd 	= String.format("%-10s", recv_cd);		
		sb.append(this.recv_cd);
		
		this.send_tm 	= Util.getDate().substring(2, 14);
		sb.append(this.send_tm);
		
		if(rec_cnt == 0)	this.rec_cnt	= "00".toString();	
		else	this.rec_cnt = String.format("%02d", rec_cnt);
		sb.append(this.rec_cnt);
		
		this.block_no	= String.format("%04d", block_no);
		sb.append(this.block_no);
		
		this.seq_no		= String.format("%03d", seq_no);
		sb.append(this.seq_no);
		
		this.return_cd 	= WorkDefinition.FINE;
		sb.append(this.return_cd);
		
		if(rec_len == 0)	this.rec_len 	= "000".toString();	
		else	this.rec_len = String.format("%03d", rec_len);
		sb.append(this.rec_len);
		
		if(sr_ty.length() != 0)		this.sr_ty = sr_ty;
		else	this.sr_ty = String.format("%s", " ");
		sb.append(this.sr_ty);
		
		if(miss_cnt != 0)	this.miss_cnt = String.format("%03d", miss_cnt);
		else	this.miss_cnt = "000".toString();
		sb.append(this.miss_cnt);
		
		if(from_date.length() != 0)	this.from_date = from_date;
		else	this.from_date = String.format("%6s", " ");
		sb.append(this.from_date);
		
		if(to_date.length() != 0)	this.to_date = to_date;
		else	this.to_date = String.format("%6s", " ");
		sb.append(this.to_date);
		
		if(req_type.length() != 0)	this.req_type = req_type;
		else	this.req_type = String.format("%s", " ");
		sb.append(this.req_type);
		
		//according to Command type file sequence
		if(msg_type.equals(WorkDefinition.TRANS_REQ_MSG) && this.sr_ty.equals(WorkDefinition.RECV_REQ) && filler.length() != 0)
			this.filler	= String.format("%03d%7s", Integer.parseInt(filler), " ");
		else if(f_size > 0L)	this.filler = String.format("%010d", f_size);
		else					this.filler = String.format("%10s", " ");
		sb.append(this.filler);
		
		this.ebc_yn = String.format("%s", " ");		this.enc_yn = String.format("%s", " ");		
		
		if(Util.get("ZIP_YN").equals("Y") && Util.checkDataType(f_type).equals("TEXT"))	this.zip_yn = "Z";
		else	this.zip_yn = String.format("%s", " ");
		
		if(bin_len > 0)	this.bin_len = String.format("%04d", bin_len);
		else	this.bin_len = String.format("%4s", " ");	
		
		this.enc_rem 	= String.format("%s", " ");	
		
		/*기본 Binary 통신을 위한 Flag*/
		this.filler_1 	= String.format("%s", "B");	
		this.filler_2 	= String.format("%4s", " ");
		
		sb.append(this.ebc_yn).append(this.enc_yn).append(this.zip_yn);
		sb.append(this.bin_len).append(this.enc_rem).append(this.filler_1).append(this.filler_2);
		
		return sb.toString();
	}
	public String NegoMsg(String r_msg)
	{
		return r_msg.substring(3, 10).toString();
	}
	public String getF_type() {
		return f_type;
	}

	public void setF_type(String f_type) {
		this.f_type = f_type;
	}

	public String getMsg_type() {
		return msg_type;
	}

	public void setMsg_type(String msg_type) {
		this.msg_type = msg_type;
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

	public String getSend_tm() {
		return send_tm;
	}

	public void setSend_tm(String send_tm) {
		this.send_tm = send_tm;
	}

	public String getRec_cnt() {
		return rec_cnt;
	}

	public void setRec_cnt(String rec_cnt) {
		this.rec_cnt = rec_cnt;
	}

	public String getBlock_no() {
		return block_no;
	}

	public void setBlock_no(String block_no) {
		this.block_no = block_no;
	}

	public String getSeq_no() {
		return seq_no;
	}

	public void setSeq_no(String seq_no) {
		this.seq_no = seq_no;
	}

	public String getReturn_cd() {
		return return_cd;
	}

	public void setReturn_cd(String return_cd) {
		this.return_cd = return_cd;
	}

	public String getRec_len() {
		return rec_len;
	}

	public void setRec_len(String rec_len) {
		this.rec_len = rec_len;
	}

	public String getSr_ty() {
		return sr_ty;
	}

	public void setSr_ty(String sr_ty) {
		this.sr_ty = sr_ty;
	}

	public String getMiss_cnt() {
		return miss_cnt;
	}

	public void setMiss_cnt(String miss_cnt) {
		this.miss_cnt = miss_cnt;
	}

	public String getFrom_date() {
		return from_date;
	}

	public void setFrom_date(String from_date) {
		this.from_date = from_date;
	}

	public String getTo_date() {
		return to_date;
	}

	public void setTo_date(String to_date) {
		this.to_date = to_date;
	}

	public String getReq_type() {
		return req_type;
	}

	public void setReq_type(String req_type) {
		this.req_type = req_type;
	}

	public String getFiller() {
		return filler;
	}

	public void setFiller(String filler) {
		this.filler = filler;
	}

	public String getEbc_yn() {
		return ebc_yn;
	}

	public void setEbc_yn(String ebc_yn) {
		this.ebc_yn = ebc_yn;
	}

	public String getEnc_yn() {
		return enc_yn;
	}

	public void setEnc_yn(String enc_yn) {
		this.enc_yn = enc_yn;
	}

	public String getZip_yn() {
		return zip_yn;
	}

	public void setZip_yn(String zip_yn) {
		this.zip_yn = zip_yn;
	}

	public String getBin_len() {
		return bin_len;
	}

	public void setBin_len(String bin_len) {
		this.bin_len = bin_len;
	}

	public String getEnc_rem() {
		return enc_rem;
	}

	public void setEnc_rem(String enc_rem) {
		this.enc_rem = enc_rem;
	}

	public String getFiller_2() {
		return filler_2;
	}

	public void setFiller_2(String filler_2) {
		this.filler_2 = filler_2;
	}
	
}
