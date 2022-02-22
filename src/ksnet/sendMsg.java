package ksnet;

import java.io.*;
import java.net.*;

public class sendMsg {
	public sendMsg(){}
	
	/*
	 * Function  : 전문 조립 후 암호화 진행 
	 * Parameter :
	 	- dout 	 = output Stream;
		- f_type = 파일구붐명
		- type 	 = 전문내역서 종류
	 * return value : 암호화된 전문 byte[] 
	 */
	public static byte[] sendMsg(DataOutputStream dout, String f_type, String type, String send_cd, String recv_cd, String req_type, int block_no, int seq_no, long f_size, String miss){
		String	send_msg	= null;
		NegoMsg	s_negomsg	= new NegoMsg();
		int 	miss_cnt 	= 0;
		
		if(type.equals(WorkDefinition.OPEN_REQ_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, 0, 0, 0, "", 0, "", "", "", "", 0L, 0);
		}
		else if(type.equals(WorkDefinition.TRANS_REQ_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, 0, 0, 0, req_type, 0, Util.from_dt, Util.to_dt, Util.get("REQ_TYPE"), "", 0L, 0);
		}
		else if(type.equals(WorkDefinition.MISS_REQ_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, block_no, seq_no, 0, "", 0, "", "", "", "", 0L, 0);
		}
		else if(type.equals(WorkDefinition.MISS_REP_MSG)){
			for(int i=0; i<seq_no; i++){
				if(miss.substring(i, i+1).equals("0"))
					miss_cnt++;
			}
			
			Util.WriteLog("sendMsg", "miss array=["+miss+"]");
			Util.WriteLog("sendMsg", "miss count=["+miss_cnt+"]");

			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, block_no, seq_no, 0, "", miss_cnt, "", "", "", "", 0L, 0);
			send_msg	= send_msg.concat(miss);
		}
		else if(type.equals(WorkDefinition.PART_END_REQ_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, 0, 0, 0, "", 0, "", "", "", "", f_size, 0);
		}
		else if(type.equals(WorkDefinition.PART_END_REP_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, block_no, seq_no, 0, "",  0, "", "", "", "", 0L, 0);
		}
		else if(type.equals(WorkDefinition.CLOSE_REQ_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, 0, 0, 0, "", 0, "", "", "", "", 0L, 0);
		}
		else if(type.equals(WorkDefinition.CLOSE_REP_MSG)){
			send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, block_no, seq_no, 0, "", 0, "", "", "", "", 0L, 0);
		}
		try{	
			//Encryption Step
			if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
				byte[] p_dbuf	= send_msg.getBytes()						 ;
				byte[] e_dbuf	= null ;
				
				e_dbuf	= eUtil.ks_seed_encrypt(keyInfo.getInstance().getP_kbuf(), p_dbuf);
				byte[]	e_sbuf	= new byte[4+1+e_dbuf.length]			 ;

				String	h_msg	= String.format("%04d%s", e_dbuf.length+1, WorkDefinition.MSG_TP_D);

				System.arraycopy(h_msg.getBytes()	, 0, e_sbuf, 0						, h_msg.getBytes().length	);
				System.arraycopy(e_dbuf				, 0, e_sbuf, h_msg.getBytes().length, e_dbuf.length				);

				Util.WriteLog("sendMsg", "Msg=["+new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), e_dbuf), "EUC-KR")+"]");
				
				return e_sbuf;
			}
			else if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_KECB)){
				
				//Msg length(4byte)
				int	elen	= send_msg.length();
				send_msg	= String.format("%04d", elen).concat(send_msg);
				
				Util.WriteLog("sendMsg", "Msg("+elen+")=["+send_msg+"]");
				
				byte[] rnd_bytes	= eUtil.mem_rnd_to_msg();
				byte[] enc_counter	= eUtil.aes_128_ecb_encrypt(metaInfo.getInstance().getKey(), rnd_bytes, 0, 16);
				byte[] enc_data		= eUtil.speed_ctr_encrypt(send_msg.getBytes(), elen+4);
				
				int midx = 0, tlen = 0;

				byte[] sbuf	= new byte[7 + enc_counter.length + enc_data.length + 1];
				
				tlen	= sbuf.length - 4;

				sbuf[0]	= WorkDefinition.C_STX;
				sbuf[1]	= (byte)((tlen >>> 8) & 0xff);
				sbuf[2]	= (byte)(tlen & 0xff);
				sbuf[3]	= eUtil.calculate_lrc(sbuf, 3);
				sbuf[4]	= '$';

				midx	= 5;
				System.arraycopy("D1".getBytes(), 0, sbuf, midx, "D1".getBytes().length);
				midx	= midx + "D1".getBytes().length;

				System.arraycopy(enc_counter, 0, sbuf, midx, enc_counter.length);
				midx	= midx + enc_counter.length;

				System.arraycopy(enc_data, 0, sbuf, midx, enc_data.length);
				midx	= midx + enc_data.length;

				sbuf[midx]	= WorkDefinition.C_ETX;

				return sbuf;
			}
		}catch(Exception e){
			Util.WriteLog("sendMsg", "Error : ["+e.getMessage()+"]");
			return (new byte[0]);
		}

		return (new byte[0]);
	}

	/*
	 * Function  : 전문+데이터 조립
	 * Parameter : 
	 	- serv     : Server socket;
		- dout     : output stream;
		- f_type   : 파일구분코드
		- type     : 송수신전문 업무구분
	 	- rec_cnt  : 바이너리송신 모드 이므로 고정값 "1"
		- data_buf : 전송할 전문내 데이터
		- send_buf : 공통부 + 데이터
	 * return value : void
	 */
	public static void sendData(Socket serv, DataOutputStream dout, String f_type, String type, String send_cd, String recv_cd, int block_no, int seq_no, int rec_cnt, int rec_len, byte[] data_buf, byte[] send_buf){
		String	send_msg 	= null;
		NegoMsg s_negomsg	= new NegoMsg();
		
		if(type.equals(WorkDefinition.DATA_REQ_MSG) || type.equals(WorkDefinition.MISS_DATA_MSG)){
			try{
			
				if(!serv.isConnected()){	Util.WriteLog("sendData", "server socket closed~!");	return; }
				
				send_msg	= s_negomsg.NegoMsg(f_type, type, send_cd, recv_cd, 0, block_no, seq_no, 0, "", 0, "", "", "", "", 0L, rec_len);
				
				System.arraycopy(send_msg.getBytes(), 0, send_buf, 0, send_msg.length());
				System.arraycopy(data_buf, 0, send_buf, send_msg.length(), rec_cnt * rec_len);
			
				if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
					if(!snd_enc_data_socket(serv, send_buf, dout))	
						Util.WriteLog("sendData", "Error snd_enc_data_socket()");
				}
				else if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_KECB)){
					
					String	elen	= String.format("%04d", send_msg.length()+rec_len);
					if(!snd_enc_data_socket(serv, send_buf, dout, elen))
						Util.WriteLog("sendData", "Error snd_enc_data_socket()");
				}
			}catch(Exception e){
				Util.WriteLog("sendData", "Error : ["+e.getMessage()+"]");
				return;
			}
		}
		
	}
	
	/*
	 * Function		: 암호화된 전문 송/수신
	 * Parameter	:
	 	serv - 서버 Socket;
		sbuf - Send Buffer;
		dout - Output Stream;
		din  - Input Stream;
	 * return value : Error Code(String);
	 */
	public static String snd_rcv_socket(Socket serv, byte[] sbuf, DataOutputStream dout, DataInputStream din) throws UnsupportedEncodingException {
		byte[]			len_buf	= new byte[4]		;
		readMsg			rm		= new readMsg()		;
		String			rmsg	= null				;
		
		if(!serv.isConnected()){
			Util.WriteLog("snd_rcv_socket","server socket is closed~!");
			return null;
		}
		try{
			dout	= new DataOutputStream(serv.getOutputStream());
			dout.write(sbuf, 0, sbuf.length);
			dout.flush();
		}catch(Exception e){
			Util.WriteLog("snd_rcv_socket","Write() Error : ["+e.getMessage()+"]");
			return WorkDefinition.ERROR_CONNECT;
		}
		try{
			din	= new DataInputStream(serv.getInputStream());
			
			int rtn_len	= 0;
			if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
				
				din.readFully(len_buf);
				
				int		len 	= Integer.parseInt(new String(len_buf, "EUC-KR"));
				byte[]	tbuf	= new byte[len]		;
				byte[]	rbuf	= new byte[len-1]	;

				din.readFully(tbuf);

				if(new String(tbuf, 0, 1, "EUC-KR").equals(WorkDefinition.MSG_TP_D))
						System.arraycopy(tbuf, 1, rbuf, 0, len-1);
				else{
						Util.WriteLog("snd_rcv_socket","Abnormal MSG TYPE~!");
						return WorkDefinition.ERROR_ABNORMAL_MSG;
				}
				
				rmsg	= new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), rbuf), "EUC-KR");
				Util.WriteLog("snd_rcv_socket", "Msg=["+rmsg+"]");
				
				/*test*/
				Util.WriteLog("snd_rcv_socket", "RETURN CODE : ["+rmsg.substring(51, 53).toString()+"]");
				return rmsg.substring(51, 53).toString();
			}
			else if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_KECB)){
				
				byte[]	mbuf	= new byte[20480];
				
				rtn_len	= din.read(mbuf, 0, 3400);
				
				int min_len	= 7+1;		//sizeof(NEW_RSA_HEADER)
			
				if(mbuf[0] != WorkDefinition.C_STX || mbuf[rtn_len-1] != WorkDefinition.C_ETX){
					Util.WriteLog("snd_rcv_socket", "ERROR format-error2");
					return "99".toString();
				}
				if(mbuf[5] != 'D' || mbuf[6] != '1'){
					Util.WriteLog("snd_rcv_socket", "ERROR format-error3");
					return "99".toString();
				}

				byte[]	enc_counter	= new byte[16];
				byte[]	enc_data	= new byte[rtn_len - 7 - 16 - 1];
				int		midx		= 0;

				midx	= 7;
				System.arraycopy(mbuf, midx, enc_counter, 0, enc_counter.length);
				midx	= midx + enc_counter.length;
				System.arraycopy(mbuf, midx, enc_data, 0, enc_data.length);

				byte[]	rbuf	= eUtil.aes_128_ecb_decrypt(metaInfo.getInstance().getKey(), enc_counter, 0, 16);
			
				if(!eUtil.msg_to_mem_rnd(rbuf)){
					throw new IllegalStateException("ERROR = [ 난수위치정보오류!! ]");
				}
				
				byte[]	wbuf	= eUtil.speed_ctr_decrypt(enc_data, enc_data.length);
				
				rmsg	= new String(wbuf, "EUC-KR");
				
				Util.WriteLog("snd_rcv_socket", "Msg("+rmsg.length()+")=["+rmsg+"]");
				
				return rm.readMsg(rmsg.substring(4, rmsg.length()), rmsg.substring(7, 14)).toString();
			}

		}catch(Exception e){
			Util.WriteLog("snd_rcv_socket","Error : ["+e.getMessage()+"]");
			return WorkDefinition.ERROR_ETC;
		}
		return null;
	}
	/*
	 * Function  : 결번요청에 대한 응답 / 자료송수신요청에 대한 응답 의 전문 Parsing
	 * ParaMeter :
	 	- serv  : server socket;
		- sbuf  : 전문 buffer
		- dout  : Dataoutput Stream;
		- din	: DataIntput Stream;
		- type	: 전문 종류(결번요청응답)
	 * return value : String
	 */
	public static String snd_rcv_socket(Socket serv, byte[] sbuf, DataOutputStream dout, DataInputStream din, String type){
		byte[]			len_buf = new byte[4]			;
		readMsg			rm 		= new readMsg()			;
		String			result	= null					;
		
		if(!serv.isConnected()){
			Util.WriteLog("snd_rcv_socket", "server socket is closed~!");
			return WorkDefinition.ERROR_CONNECT;
		}
		try{
			dout	= new DataOutputStream(serv.getOutputStream());
			dout.write(sbuf, 0, sbuf.length);
			dout.flush();
		}catch(Exception e){
			Util.WriteLog("snd_rcv_socket", "write() Error : ["+e.getMessage()+"]");
		}
		try{
			din		= new DataInputStream(serv.getInputStream());
			if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
				int rtn_len		= 0;
				int	read_len	= 0;
	
				din.readFully(len_buf);
			
				//Util.WriteLog("snd_rcv_socket", "len buf : ["+new String(len_buf, "EUC-KR")+"]");
	
				int 	len		= Integer.parseInt(new String(len_buf, "EUC-KR"));
				byte[]	tbuf	= new byte[len]		;
				byte[]	rbuf	= new byte[len-1]	;
			
				din.readFully(tbuf);
	
				if(new String(tbuf, 0, 1, "EUC-KR").equals(WorkDefinition.MSG_TP_D))
					System.arraycopy(tbuf, 1, rbuf, 0, len-1);
				else{
					Util.WriteLog("snd_rcv_socket", "Abnormal MSG TYPE~!");
					return WorkDefinition.ERROR_ABNORMAL_MSG;
				}
			
				//암호화된 수신전문 복호화
				String	rmsg	= new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), rbuf), "EUC-KR");
				
				Util.WriteLog("snd_rcv_socket", "Msg=["+rmsg+"]");
				String	mtype	= rmsg.substring(3, 10).toString();
				String	rtn		= rm.readMsg(rmsg, mtype); 
			
				if(type.equals(WorkDefinition.TRANS_REP_MSG) || type.equals(WorkDefinition.MISS_REP_MSG)){
					result	= rtn;
				}
			}
			else if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_KECB)){
				byte[]	mbuf	= new byte[WorkDefinition.MAX_E_MSG_SIZE];

				int rtn_len	= din.read(mbuf, 0, 3400);

				int min_len	= 7+1;	//sizeof(NEW_RSA_HEADER)

				if(mbuf[0] != WorkDefinition.C_STX || mbuf[rtn_len-1] != WorkDefinition.C_ETX){
					Util.WriteLog("snd_rcv_socket", "ERROR format-error2");
					return "99".toString();
				}
				if(mbuf[5] != 'D' || mbuf[6] != '1'){
					Util.WriteLog("snd_rcv_socket", "ERROR format-error3");
					return "99".toString();
				}
				
				byte[]	enc_counter	= new byte[16];
				byte[]	enc_data	= new byte[rtn_len - 7 - 16 - 1];
				int	midx		= 0;

				midx	= 7;
				System.arraycopy(mbuf, midx, enc_counter, 0, enc_counter.length);
				midx	= midx + enc_counter.length;

				System.arraycopy(mbuf, midx, enc_data, 0, enc_data.length);

				byte[]	rbuf	= eUtil.aes_128_ecb_decrypt(metaInfo.getInstance().getKey(), enc_counter, 0, 16);

				if(!eUtil.msg_to_mem_rnd(rbuf)){
					throw new IllegalStateException("ERROR = [ 난수위치정보오류!! ]");
				}

				byte[]	wbuf	= eUtil.speed_ctr_decrypt(enc_data, enc_data.length);

				String	rmsg	= new String(wbuf, "EUC-KR");
				String	mtype	= rmsg.substring(7, 14);
				String	rtn		= rm.readMsg(rmsg.substring(4, rmsg.length()), mtype);
				 
				Util.WriteLog("snd_rcv_socket", "Recv Msg("+rmsg.length()+")=["+rmsg+"]");
				
				return rtn;
			}
		}catch(Exception e){
			Util.WriteLog("snd_rcv_socket", "Error : ["+e.getMessage()+"]");
			return WorkDefinition.ERROR_ETC;
		}
		
		return result;
	}
	
	/*
	 * Function  	: 전문 데이터 SEED암호화 송신 
	 * Parameter 	:
	 	- serv : Server Socket;
		- sbuf : data Buffer;
		- dout : output stream;
	 * return value : boolean  
	 */
	public static boolean snd_enc_data_socket(Socket serv, byte[] sbuf, DataOutputStream dout) throws UnsupportedEncodingException{

		try{
			if(!serv.isConnected()){	Util.WriteLog("snd_enc_data_socket", "socket is closed~!"); return false;}
			
			dout			= new DataOutputStream(serv.getOutputStream());
			
			byte[]	e_dbuf	= eUtil.ks_seed_encrypt(keyInfo.getInstance().getP_kbuf(), sbuf);
			
			String	h_msg	= String.format("%04d%s", e_dbuf.length+1, WorkDefinition.MSG_TP_D);
			byte[]	e_sbuf	= new byte[4+1+e_dbuf.length];

			System.arraycopy(h_msg.getBytes(), 0, e_sbuf, 0, 5);
			System.arraycopy(e_dbuf, 0, e_sbuf, 5, e_dbuf.length);
		
			Util.WriteLog("snd_enc_data_socket", "Msg=["+new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), e_dbuf), 0, 120, "EUC-KR")+" ... ]");
			
			dout.write(e_sbuf, 0, e_sbuf.length);
			dout.flush();
		}catch(Exception e){
			Util.WriteLog("snd_enc_data_socket", "Error : ["+e.getMessage()+"]");
			return false;
		}
		return true;
	}
	
	/*
	 * Function		: 전문 데이터 ECB암호화 송신
	 * Parameter	:
	 	- serv : Server Socket;
		- sbuf : data Buffer;
		- dout : output stream;
		- elen : 앞의 길이 4byte;
	 * return value : boolean
	 */
	public static boolean snd_enc_data_socket(Socket serv, byte[] sbuf, DataOutputStream dout, String elen) throws UnsupportedEncodingException{
		try{
			if(!serv.isConnected()){ 	Util.WriteLog("snd_enc_data_socket", "socket is closed~!");	return false; }

			dout	= new DataOutputStream(serv.getOutputStream());

			byte[]	p_sbuf	= new byte[Integer.parseInt(elen) + 4];

			System.arraycopy(elen.getBytes(), 0, p_sbuf, 0, 4						);
			System.arraycopy(sbuf			, 0, p_sbuf, 4, Integer.parseInt(elen)	);

			Util.WriteLog("snd_enc_data_socket", "Msg=["+new String(p_sbuf, 0, 100, "EUC-KR")+" ... ]");
			
			byte[]	rnd_bytes	= eUtil.mem_rnd_to_msg();
			byte[]	enc_counter	= eUtil.aes_128_ecb_encrypt(metaInfo.getInstance().getKey(), rnd_bytes, 0, 16);
			byte[]	enc_data	= eUtil.speed_ctr_encrypt(p_sbuf, Integer.parseInt(elen)+4);
			
			Util.WriteLog("snd_enc_data_socket", "enc_counter:["+enc_counter.length+"], enc_data:["+enc_data.length+"]");
			
			byte[]	e_sbuf	= new byte[7 + enc_counter.length + enc_data.length + 1];

			int tlen	= e_sbuf.length - 4;

			e_sbuf[0]	= WorkDefinition.C_STX;
			e_sbuf[2]	= (byte) (tlen & 0xff);
			e_sbuf[1]	= (byte) ((tlen >>> 8) & 0xff);
			e_sbuf[3]	= eUtil.calculate_lrc(e_sbuf, 3);
			e_sbuf[4]	= '$';

			int midx	= 5;
			System.arraycopy("D1".getBytes(), 0, e_sbuf, midx, "D1".getBytes().length);
			midx	= midx + "D1".getBytes().length;

			System.arraycopy(enc_counter, 0, e_sbuf, midx, enc_counter.length);
			midx	= midx + enc_counter.length;

			System.arraycopy(enc_data, 0, e_sbuf, midx, enc_data.length);
			midx	= midx + enc_data.length;

			e_sbuf[midx]	= WorkDefinition.C_ETX;
			midx++;

			dout.write(e_sbuf, 0, e_sbuf.length);
			dout.flush();
		
		}
		catch(Exception e){
			Util.WriteLog("snd_enc_data_socket", "Error : ["+e.getMessage()+"]");
			return false;
		}
		return true;
	}
	
	/*
	 * Function  : 암호화된 전문 송신
	 * Parameter : 
	 	- serv : server socket
		- sbuf : data buffer
		- dout : Output stream
	 * return value : boolean
	 */
	public static boolean snd_socket(Socket serv, byte[] sbuf, DataOutputStream dout){
		
		try{
			if(!serv.isConnected()){	Util.WriteLog("snd_socket", "socket is closed~!");	return false; }

			dout	= new DataOutputStream(serv.getOutputStream());

			dout.write(sbuf, 0, sbuf.length);
			dout.flush();
		
		}catch(Exception e){
			Util.WriteLog("snd_socket", "ERROR : ["+e.getMessage()+"]");
			return false;
		}
		return true;
	}
}
