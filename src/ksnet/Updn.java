package ksnet;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;

public class Updn {
	public static String RT_MODE					;
	public static String SERVER_IP			= "210.181.28.143";
	public static String PRF_SERVER_IP		= "121.138.30.10";
	public static String R_SERVER_PORT		= "29994";
	public static String T_SERVER_PORT		= "29998";
	public static String R_PRF_SERVER_PORT	= "29995";
	public static String T_PRF_SERVER_PORT	= "29997";
	public static String SEND_FILENAME				;
	
	public static void updnSendData(String info_cd, String send_cd, String recv_cd, String dir, String mode) throws Exception{
		
		ksnetInternetEntSvrClient.fileformat		= new SendFileFormat[1];
		ksnetInternetEntSvrClient.prf_fileformat	= new SendFileFormat[1];
		ksnetInternetEntSvrClient.data_list			= new DataList[1];
		ksnetInternetEntSvrClient.prf_data_list		= new DataList[1];
		
		makeFname(send_cd, recv_cd, info_cd, dir);
		
		if(!Util.CheckFileState(Util.get("SEND_DIR"), SEND_FILENAME)){
			Util.WriteLog("updnSendData", "CheckFileState() ERROR");
			return ;
		}
		
		//Proof Firm Data
		if(ksnetInternetEntSvrClient.prf_datalist_cnt != 0){
			
			ksnetInternetEntSvrClient.enc_tp			= WorkDefinition.ENCTP_KECB;
			ksnetInternetEntSvrClient.prf_data_list[0]	= new DataList(ksnetInternetEntSvrClient.prf_fileformat[0]);
			
			if(mode.equals("T"))		RT_MODE="T";
			else if(mode.equals("R"))	RT_MODE="R";

			if(!Util.SendData(ksnetInternetEntSvrClient.prf_data_list, 1))
				Util.WriteLog("updnSendData", "SendData() ERROR~!");
		}
		//Normal Firm Data
		else if(ksnetInternetEntSvrClient.datalist_cnt != 0){
			
			ksnetInternetEntSvrClient.enc_tp		= WorkDefinition.ENCTP_SEED;
			ksnetInternetEntSvrClient.data_list[0]	= new DataList(ksnetInternetEntSvrClient.fileformat[0]);
			
			if(mode.equals("T"))		RT_MODE="T";
			else if(mode.equals("R"))	RT_MODE="R";

			if(!Util.SendData(ksnetInternetEntSvrClient.data_list, 1))
				Util.WriteLog("updnSendData", "SendData() ERROR~!");
		}	
		
		Util.deleteFile(Util.get("SEND_DIR")+System.getProperty("file.separator")+SEND_FILENAME);
		Util.WriteLog("updnSendData", "SEND WORK END");
	}

	public static void updnRecvData(String send_dt, String info_cd, String send_cd, String recv_cd, String f_seq_no, String dir, String mode){
		Socket				serv	= null;
		DataOutputStream	dout	= null;
		DataInputStream		din		= null;

		NegoMsg	s_negomsg	= new NegoMsg();
		try{
			if(mode.equals("T")){
				serv	= Util.connectServer(SERVER_IP, T_SERVER_PORT);
				Updn.RT_MODE = "T";
			}
			else{
				serv	= Util.connectServer(SERVER_IP, R_SERVER_PORT);
				Updn.RT_MODE = "R";
			}
			
			//for TIME_WAIT protection
			serv.setSoLinger(true, 0);

			//Encryption type setting
			ksnetInternetEntSvrClient.enc_tp = WorkDefinition.ENCTP_SEED;
			
			dout	= new DataOutputStream(serv.getOutputStream());
			din		= new DataInputStream(serv.getInputStream());

			keyInfo.getInstance().setP_kbuf(eUtil.GenerateKey());
			
			//Encrypted session key
			keyInfo.getInstance().setE_kbuf(eUtil.ks_rsa_encrypt(keyInfo.getInstance().getP_kbuf()));
			
			byte[]	sbuf	= eUtil.setEncsession(keyInfo.getInstance().getP_kbuf(), keyInfo.getInstance().getE_kbuf());
			
			if(!Util.snd_rcv_sessionKey(serv, dout, din, sbuf)){
				Util.WriteLog("updnRecvData", "ERROR snd_rcv_sessionKey()");
				return ;
			}
			
			byte[]	e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.OPEN_REQ_MSG, recv_cd, WorkDefinition.KSNET, "", 0, 0, 0L, "");

			if(!sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din).equals(WorkDefinition.FINE)){
				Util.WriteLog("updnRecvData", "ERROR snd_rcv_socket()");
				return ;
			}
		
			String	send_msg	= s_negomsg.NegoMsg(info_cd, WorkDefinition.TRANS_REQ_MSG, recv_cd, send_cd, 0, 0, 0, 0, WorkDefinition.RECV_REQ, 0, send_dt.substring(2, 8), send_dt.substring(2, 8), "A", f_seq_no, 0L, 0);
			byte[]	e_dbuf		= eUtil.ks_seed_encrypt(keyInfo.getInstance().getP_kbuf(), send_msg.getBytes("EUC-KR"));
			
			e_sbuf		= new byte[4+1+e_dbuf.length];

			String	h_msg		= String.format("%04d%s", e_dbuf.length+1, WorkDefinition.MSG_TP_D);

			System.arraycopy(h_msg.getBytes()	, 0, e_sbuf, 0						, h_msg.getBytes().length	);
			System.arraycopy(e_dbuf				, 0, e_sbuf, h_msg.getBytes().length, e_dbuf.length				);
			
			Util.WriteLog("updnRecvData", "Msg=["+new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), e_dbuf), "EUC-KR")+"]");
			
			if(!sendMsg.snd_socket(serv, e_sbuf, dout)){
				Util.WriteLog("updnRecvData", "ERROR ("+WorkDefinition.TRANS_REQ_MSG+") STEP");
				return ;
			}
			
			String rtn	= readMsg.readMsg(serv, din, WorkDefinition.TRANS_REP_MSG);
			if(!rtn.substring(0, 2).equals(WorkDefinition.FINE) && !rtn.substring(0, 2).equals(WorkDefinition.ERROR_NO_DATA)){
				Util.WriteLog("updnRecvData", "ERROR ("+WorkDefinition.TRANS_REP_MSG+") STEP / ERROR CODE=["+rtn.substring(0, 2)+"]");
				return ;
			}
			else if(rtn.substring(0, 2).equals(WorkDefinition.ERROR_NO_DATA))		Util.no_data_yn = true;
			
			if(Util.no_data_yn)			Util.WriteLog("updnRecvData", "NO RECEIVE DATA");
			
			ksnetInternetEntSvrClient.datalist_cnt	= Integer.parseInt(rtn.substring(2, 6));
			ksnetInternetEntSvrClient.data_list		= new DataList[ksnetInternetEntSvrClient.datalist_cnt];
			ksnetInternetEntSvrClient.fileformat	= new SendFileFormat[ksnetInternetEntSvrClient.datalist_cnt];
			
			Util.WriteLog("updnRecvData", "RECV DATA COUNT : ["+ksnetInternetEntSvrClient.datalist_cnt+"]");
			
			downloadProcess dp	= new downloadProcess();
			if(ksnetInternetEntSvrClient.datalist_cnt == 0 && Util.no_data_yn)
				dp.downloadProcess(serv, din, dout, recv_cd, 0);
			else{
				if(!dp.downloadProcess(serv, din, dout, recv_cd, ksnetInternetEntSvrClient.datalist_cnt)){
					Util.WriteLog("updnRecvData", "ERROR ("+WorkDefinition.DATA_REQ_MSG+") STEP");
					return ;
				}
			}
			
			rtn	= readMsg.readMsg(serv, din, WorkDefinition.CLOSE_REQ_MSG);
			e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.CLOSE_REP_MSG, recv_cd, WorkDefinition.KSNET, "", 0, 0, 0, "");

			if(!sendMsg.snd_socket(serv, e_sbuf, dout))
				Util.WriteLog("updnRecvData", "ERROR ("+WorkDefinition.CLOSE_REQ_MSG+") STEP");
			
			Util.WriteLog("updnRecvData", "RECEIVE WORK END");
			
			return ;
		}catch(Exception e){
			Util.WriteLog("updnRecvData", "ERROR : ["+e.getMessage()+"]");
			return ;
		}finally{
			try{	if(serv != null)	serv.close();}
			catch(Exception e){}
			try{	if(dout != null){	dout.flush();	dout.close();}}
			catch(Exception e){}
			try{	if(din != null)		din.close();}
			catch(Exception e){}
		}
	}
	protected static void makeFname(String send_cd, String recv_cd, String info_cd, String dir){
		
		FileInputStream		src 	= null;
		FileOutputStream 	dst 	= null;
		FileChannel			fcin	= null;
		FileChannel			fcout	= null;
		StringBuilder		sb	= new StringBuilder();
		
		int f_seq	= 1;
		
		sb.append("A").append(WorkDefinition.SEND_FILE_FLAG).append(Util.getDate().substring(0, 8)).append("_");
		sb.append(send_cd).append(recv_cd).append(info_cd).append(".").append(String.format("%03d", f_seq));
		
		while(true){
			if(new File(Util.get("SEND_DIR") + System.getProperty("file.separator") + sb.toString()).exists())
				sb.replace(sb.toString().length()-3, sb.toString().length(), String.format("%03d", ++f_seq));
			else break;
		}
		
		SEND_FILENAME = sb.toString();
		Util.WriteLog("makeFname", "SRC FILE : ["+dir+"], TMP FILE : ["+sb.toString()+"]");
		try{
			src = new FileInputStream(dir);
			dst	= new FileOutputStream(Util.get("SEND_DIR")+System.getProperty("file.separator")+SEND_FILENAME);
			
			fcin	= src.getChannel();
			fcout	= dst.getChannel();

			long size = fcin.size();
			
			//temporary file copy
			fcin.transferTo(0, size, fcout);
		
		}catch(Exception e){
			Util.WriteLog("makeFname", "ERROR : ["+e.getMessage()+"]");
		}finally{
			try{	if(src != null)		src.close();}
			catch(Exception e){}
			try{	if(dst != null)		dst.close();}
			catch(Exception e){}
			try{	if(fcin != null)	fcin.close();}
			catch(Exception e){}
			try{	if(fcout != null)	fcout.close();}
			catch(Exception e){}
		}
	}
}
