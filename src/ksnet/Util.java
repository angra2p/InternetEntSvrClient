package ksnet;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;


public class Util {
	public static boolean	no_data_yn	= false;
	public static String config_path 	;
	public static long stime 		 	;
	public static long olen 		 	;
	public static int max_cps 		 	;
	public static String data_type		;
	public static String send_dir	 	;
	public static String from_dt		;
	public static String to_dt			;
	
	public Util(){}
	public static boolean SendData(DataList[] data_list, int datalist_cnt) throws Exception {
		
		Socket				serv		= null;
		DataOutputStream	dout		= null;
		DataInputStream		din			= null;
		uploadProcessBin	upb			= null;

		data_list[0].ret_cd = WorkDefinition.ERROR_ETC.toString();
		
		WriteLog("SendData", "RT_MODE : ["+Updn.RT_MODE+"]");
		try {
			
			/*daemon or window service mode*/
			if(Updn.RT_MODE == null){
				if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED))
					serv	= connectServer(get("SERVER_IP"), get("SERVER_PORT"));
				else
					serv	= connectServer(get("PRF_SERVER_IP"), get("PRF_SERVER_PORT"));
			}
			/*command mode*/
			else{
				if(Updn.RT_MODE.equals("R")){
					if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED))
						serv	= connectServer(Updn.SERVER_IP, Updn.R_SERVER_PORT);
					else
						serv	= connectServer(Updn.PRF_SERVER_IP, Updn.R_PRF_SERVER_PORT);
				}
				else{
					if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED))
						serv	= connectServer(Updn.SERVER_IP, Updn.T_SERVER_PORT);
					else
						serv	= connectServer(Updn.PRF_SERVER_IP, Updn.T_PRF_SERVER_PORT);
				}
			}
			
			Util.getFromToDt();
			
			// Control Speed Logic
			control_speed(get("NET_BPS"), 0, true);

			// normal firm data
			if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
					
					//Key 생성 및 Instance 저장
					keyInfo.getInstance().setP_kbuf(eUtil.GenerateKey());
					
					//암호화된 Session Key(SeedKey를 이용한 암호화)
					keyInfo.getInstance().setE_kbuf(eUtil.ks_rsa_encrypt(keyInfo.getInstance().getP_kbuf()));
					
					byte[]	sbuf	= eUtil.setEncsession(keyInfo.getInstance().getP_kbuf(), keyInfo.getInstance().getE_kbuf());
					
					if(!snd_rcv_sessionKey(serv, dout, din, sbuf))	return false;
			}
			
			// proof firm data
			else{
				byte[]	sbuf	= new byte[267];
				int		tlen	= sbuf.length - 4;
				
				Well512.getInstance().initWELL();
				//Key 생성
				metaInfo.getInstance().setKey(eUtil.make_session_key())	;
				//Key 암호화
				byte[]	kbuf	= eUtil.encrypt_rsa_2048(metaInfo.getInstance().getKey());
				
				System.arraycopy("0000$C1000".getBytes(), 0, sbuf, 0, 10);
				System.arraycopy(kbuf, 0, sbuf, 10, kbuf.length);
	
				sbuf[0]	=	WorkDefinition.C_STX		;
				sbuf[1]	=	(byte) ((tlen >>> 8) & 0xff);
				sbuf[2]	=	(byte) (tlen & 0xff)		;			//(byte)7
				sbuf[3]	=	eUtil.calculate_lrc(sbuf, 3);

				sbuf[sbuf.length - 1] = WorkDefinition.C_ETX;
				
				//세션키 전송
				if(!snd_rcv_sessionKey(serv, dout, din, sbuf))	return false;
				
				metaInfo.getInstance().setRoute_type("00".getBytes())	;
				metaInfo.getInstance().setEnc_type("$".getBytes())		;
				metaInfo.getInstance().setM_key_type("0".getBytes())	;
					
			}
				
			byte[]		e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.OPEN_REQ_MSG, get("KSNET_CD"), WorkDefinition.KSNET, "", 0, 0, 0L, "");
		
			if(!sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din).equals(WorkDefinition.FINE)){	
				WriteLog("snd_rcv_socket", "Error snd_rcv_socket()");	
				return false;
			}
		
			//send data count
			Util.WriteLog("SendData", "SEND DATA COUNT : ["+datalist_cnt+"]");
		
			for (int i = 0; i < datalist_cnt; i++) {
					e_sbuf	= sendMsg.sendMsg(dout, data_list[i].getF_type(), WorkDefinition.TRANS_REQ_MSG, data_list[i].getSend_cd(), data_list[i].getRecv_cd(), WorkDefinition.SEND_REQ, 0, 0, 0L, "");
				
					if(!sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din).equals(WorkDefinition.FINE)){	
						WriteLog("snd_rcv_socket", "Error snd_rcv_socket()");	
						return false; 
					}
					
					// Binary send mode
					upb	= new uploadProcessBin();
					if(!upb.upload(data_list[i], serv, dout, din)){
						WriteLog("uploadProcessBin", "ERROR=["+data_list[i].getRet_cd()+"]");
						
						if(i == datalist_cnt - 1)	return false;
						else	continue;
					}
					WriteLog("SendData", "ERROR CODE : ["+data_list[i].getRet_cd()+"]");
			} //i for end

			e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.CLOSE_REQ_MSG, get("KSNET_CD"), WorkDefinition.KSNET, "", 0, 0, 0L, "");
		
			if(!sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din).equals(WorkDefinition.FINE)){
				WriteLog("SendData", WorkDefinition.CLOSE_REQ_MSG+" ERROR");
				return false;
			}
			
			// 0.05sec wait
			Thread.sleep(50);
			
			return true;
		} catch (SocketTimeoutException e) {
			WriteLog("SendData", "SocketTimeoutException : "+e.getMessage());
			return false;
		} catch (Exception e) {
			WriteLog("SendData", "Exception : "+e.getMessage());
			return false;
		} finally {
			try{	if (serv != null)	serv.close(); 	}
			catch (Exception e) {}
			try {	if (dout != null)	dout.close(); 	}
			catch (Exception e) {}
			try {	if (din != null)	din.close(); 	}
			catch (Exception e) {}
			try {	if (upb	!= null)	upb	= null;		}
			catch (Exception e) {}
		} // finally end
	}
	
	public static boolean RecvData() throws IOException{
		Socket				serv			= null;
		DataOutputStream	dout			= null;
		DataInputStream		din				= null;
		downloadProcess		dp				= null;
		
		try{
			serv	= connectServer(get("SERVER_IP"), get("SERVER_PORT"));
			
			//for TIME_WAIT protection( send buffer's data and close )
			serv.setSoLinger(true, 0);
		
			//from date & to date setting
			Util.getFromToDt();
	
			//세션키 전송
			keyInfo.getInstance().setP_kbuf(eUtil.GenerateKey());
			
			//암호화된 Session Key(SeedKey를 이용한 암호화)
			keyInfo.getInstance().setE_kbuf(eUtil.ks_rsa_encrypt(keyInfo.getInstance().getP_kbuf()));
			
			byte[]	sbuf	= eUtil.setEncsession(keyInfo.getInstance().getP_kbuf(), keyInfo.getInstance().getE_kbuf());
			
			if(!snd_rcv_sessionKey(serv, dout, din, sbuf))	return false;
			
			byte[]	e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.OPEN_REQ_MSG, get("KSNET_CD"), WorkDefinition.KSNET, "", 0, 0, 0L, "");

			if(!sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din).equals(WorkDefinition.FINE)){
				WriteLog("snd_rcv_socket", "Error snd_rcv_socket()");
				return false;
			}
			
			e_sbuf		= sendMsg.sendMsg(dout, get("RECV_DATA_TYPE"), WorkDefinition.TRANS_REQ_MSG, get("KSNET_CD"), get("RECV_BANK_CD"), WorkDefinition.RECV_REQ, 0, 0, 0L, "");
			
			String rmsg	= sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din, WorkDefinition.TRANS_REP_MSG);

			
			if(!rmsg.substring(0, 2).equals(WorkDefinition.FINE) && !rmsg.substring(0, 2).equals(WorkDefinition.ERROR_NO_DATA))		return false;
			
			//main class's static variable setting
			ksnetInternetEntSvrClient.datalist_cnt	= Integer.parseInt(rmsg.substring(2, 6));
			ksnetInternetEntSvrClient.data_list		= new DataList[ksnetInternetEntSvrClient.datalist_cnt];
			ksnetInternetEntSvrClient.fileformat	= new SendFileFormat[ksnetInternetEntSvrClient.datalist_cnt];
			
			WriteLog("RecvData", "datalist_cnt : ["+ksnetInternetEntSvrClient.datalist_cnt+"]");
		
			//no receive data
			if(ksnetInternetEntSvrClient.datalist_cnt == 0 && no_data_yn){
				dp	= new downloadProcess();
					dp.downloadProcess(serv,din, dout, get("KSNET_CD"), 0);
			}	
			else{
				for(int i=0; i<ksnetInternetEntSvrClient.datalist_cnt; i++){
					dp	= new downloadProcess();
					if(!dp.downloadProcess(serv, din, dout, get("KSNET_CD"), i))
						continue;
				}
			}
			
			rmsg	= readMsg.readMsg(serv, din, WorkDefinition.CLOSE_REQ_MSG);
		
			e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.CLOSE_REP_MSG, get("KSNET_CD"), WorkDefinition.KSNET, "", 0, 0, 0, "");
			
			if(!sendMsg.snd_socket(serv, e_sbuf, dout)){	
				WriteLog("RecvData", "Error snd_socket()");	
				return false; 
			}
		
			//0.01sec wait
			Thread.sleep(10);
			
			return true;
		}catch(Exception e){
			WriteLog("RecvData", "ERROR=["+e.getMessage()+"]");
			return false;
		}
		finally{
			try{	if(serv != null)	serv.close();	}
			catch(Exception e){}
			try{	if(dout != null)	dout.close();	}
			catch(Exception e){}
			try{	if(din != null)		din.close();	}
			catch(Exception e){}
			try{	if(dp	!= null)	dp	= null;		}
			catch(Exception e){}
		}
	}
	
	public static void initStaticVariable(){
		ksnetInternetEntSvrClient.datalist_cnt 		= 0;
		ksnetInternetEntSvrClient.prf_datalist_cnt 	= 0;
		Util.stime							= 0L;
		Util.olen							= 0L;
		Util.max_cps						= 0;
	}
	
	public static byte binaryStringToByte(String src) {
		byte ret = 0, total = 0;
		for (int i = 0; i < 8; i++) {
			ret		= (src.charAt(7 - i) == '1') ? (byte) (1 << i) : 0;
			total	= (byte) (ret | total);
		}
		return total;
	}
	/*
	public static String byteArrayToHex(byte[] src) {
		StringBuilder	sb = null;
		String			dst		 ;
		
		if(src == null || src.length == 0)
			return null;
		sb	= new StringBuilder(src.length+2);

		for(int i=0; i < src.length; i++){
			dst	= "0"+Integer.toHexString(0xff & src[i]);
			sb.append(dst.substring(dst.length() - 2));
		}
		return sb.toString();
	}
	*/
	public static void closeStream(Socket serv){
		try{
			serv.close();
		}catch(IOException e){
			Util.WriteLog("closeStream", "socket close error=["+e.getMessage()+"]");
		}
		return;
	}
	public static void closeStream(Socket serv, DataOutputStream dout, DataInputStream din){
		try{
			serv.close();
		}catch(IOException e){
			Util.WriteLog("closeStream", "socket close error=["+e.getMessage()+"]");
		}
		try{
			dout.close();
		}catch(IOException e1){
			Util.WriteLog("closeStream", "output stream close error=["+e1.getMessage()+"]");
		}
		try{
			din.close();
		}catch(IOException e2){
			Util.WriteLog("closeStream", "input stream close error=["+e2.getMessage()+"]");
		}
		return;
	}
	public static void closeStream(Socket serv, DataOutputStream dout, DataInputStream din, BufferedInputStream bin){
		try{
			serv.close();
		}catch(IOException e){
			Util.WriteLog("closeStream", "socket close error=["+e.getMessage()+"]");
		}
		try{
			dout.close();
		}catch(IOException e1){
			Util.WriteLog("closeStream", "output stream close error=["+e1.getMessage()+"]");
		}
		try{
			din.close();
		}catch(IOException e2){
			Util.WriteLog("closeStream", "input stream close error=["+e2.getMessage()+"]");
		}
		try{
			bin.close();
		}catch(IOException e3){
			Util.WriteLog("closeStream", "input stream close error=["+e3.getMessage()+"]");
		}
		return;
	}
	
	public static Socket connectServer(String ip, String port){
		try{
			Socket server	= new Socket();
			server.connect(new InetSocketAddress(ip, Integer.parseInt(port)));
			server.setSoTimeout(60*1000);
			
			WriteLog("connectServer", "IP : ["+ip+"], PORT : ["+port+"] CONNECT COMPLETION");
			return server;
		}catch(Exception e){
			WriteLog("connectServer", "("+ip+":"+port+") ERROR=["+e.getMessage()+"]");
		}
		
		return null;
	}
	public static boolean CompressGzipFile(String SrcFile, String DstFile) throws IOException {
		FileInputStream		fis		= null;
		FileOutputStream 	fos		= null;
		GZIPOutputStream	gzipos	= null;
		try {
			fis 	= new FileInputStream(SrcFile);
			fos 	= new FileOutputStream(DstFile);
			gzipos	= new GZIPOutputStream(fos);
			byte[] buf = new byte[1024];
			int len = 0;

			while ((len = fis.read(buf)) != -1) {
				gzipos.write(buf, 0, len);
			}
		} catch (IOException e) {
			Util.WriteLog("CompreGzipFile", "Gzip compress Error=["+e.getMessage()+"]");
			return false;
		} finally{
			gzipos.close();
			fos.close();
			fis.close();
		}

		return true;
	}

	public static boolean DecompressGzipFile(String SrcFile, String DstFile) throws IOException {
		FileInputStream		fis = null;
		GZIPInputStream		gis	= null;
		FileOutputStream	fos	= null;
		try {
			fis = new FileInputStream(SrcFile);
			gis = new GZIPInputStream(fis);
			fos = new FileOutputStream(DstFile);
			byte[] buf	= new byte[1024];
			int len 	= 0;

			while ((len = gis.read(buf)) != -1) {
				fos.write(buf, 0, len);
			}
		} catch (IOException e) {
			Util.WriteLog("DecompressGzipFile", "Gzip Decompress Error=["+e.getMessage()+"]");
			return false;
		} finally{
			fis.close();
			gis.close();
			fos.close();
		}
		return true;
	}

	public static boolean isStringNumber(String src){
		try{
			Integer.parseInt(src);
			return true;
		}catch(NumberFormatException e){
			WriteLog("isStringNumber", "error=["+e.getMessage()+"]");
			return false;
		}
	}
	public static synchronized void WriteLog(String funcname, String msg) {
		File 			log_dir 	= null;
		StringBuffer 	sb 			= new StringBuffer();
		PrintStream 	ps 			= null;
		String 			cur_date 	= null;
		String 			yyyymmdd 	= null;
		String 			hhmmss 		= null;

		log_dir = new File(get("LOG_DIR"));

		if (!log_dir.exists())
			log_dir.mkdir();

		cur_date 	= getDate();
		yyyymmdd 	= cur_date.substring(0, 8).toString();
		hhmmss 		= cur_date.substring(8, 14).toString();
		
		sb.append(yyyymmdd);

		File log_file = new File(log_dir, sb.toString());

		try {
			if (log_file.exists())
				ps = new PrintStream(new FileOutputStream(log_file, true), true);	//if log file exists, overwrite
			else
				ps = new PrintStream(new FileOutputStream(log_file), true);			//if log file not exists, log file create and write
		} catch (Exception e) {
			WriteLog("WriteLog",e.getMessage());
			return;
		}
		sb.setLength(0);
		sb.append("[").append(hhmmss.substring(0, 2)).append(":").append(hhmmss.substring(2, 4)).append(":").append(hhmmss.substring(4, 6)).append("] ");
		sb.append("[").append(funcname).append("]  ").append(msg);
		ps.println(sb.toString());

		try {
			if (ps != null) {
				ps.close();
			}
		} catch (Exception e) {
		} finally {
			ps = null;
		}
	}

	public static void setEnv(){
		String			tmp	= null	;
		BufferedReader	br 	= null	;
		int				idx	= 0		;

		try{
			br	= new BufferedReader(new FileReader(config_path));
			
			while((tmp = br.readLine()) != null){
				// # or null pass
				if(tmp.length() == 0 || tmp.substring(0, 1).equals("#"))
					continue;
				else{
					// ex) SERVER_IP=127.0.0.1 -> KEY : SERVER_IP, VALUE : 127.0.0.1 
					idx	= tmp.indexOf("=");
					ksnetInternetEntSvrClient.conf.put(tmp.substring(0, idx), tmp.substring(idx+1, tmp.length()));
				}
			}
		}catch(Exception e){
			WriteLog("setEnv", "Errorr : ["+e.getMessage()+"]");
		}finally{
			try{
				if(br != null)	br.close();
			}catch(Exception e){}
		}
	}
	
	public static String get(String src){
		return ksnetInternetEntSvrClient.conf.get(src);
	}
	public static boolean setConf(String dir) {
		
		if(!new File(dir).exists()){
			WriteLog("setConf", "Configure File Location Check~!");
			return false;
		}
		config_path = dir;
		return true;
	}
	public static String getDate() {
		int yyyy, mm, dd, hour, min, sec;
		Calendar cal = Calendar.getInstance();

		yyyy 	= cal.get(Calendar.YEAR);
		mm 		= cal.get(Calendar.MONTH);
		dd 		= cal.get(Calendar.DATE);
		hour 	= cal.get(Calendar.HOUR_OF_DAY);
		min 	= cal.get(Calendar.MINUTE);
		sec 	= cal.get(Calendar.SECOND);

		StringBuffer sb = new StringBuffer();
		sb.append(yyyy).append(mm < 9 ? "0" : "").append(mm + 1).append(dd < 10 ? "0" : "").append(dd).append(hour < 10 ? "0" : "").append(hour);
		sb.append(min < 10 ? "0" : "").append(min).append(sec < 10 ? "0" : "").append(sec);

		return sb.toString();
	}
	
	public static String befWorkDt(int span){
		
		Calendar	cal		= Calendar.getInstance();
		DateFormat	format	= new SimpleDateFormat("yyyyMMdd");
		cal.add(Calendar.DATE, span);
		String date	= format.format(cal.getTime());
		
		return date;
	}
	public static void getFromToDt(){
		
		from_dt	= befWorkDt(Integer.parseInt(get("FROM_DATE"))).substring(2, 8);
		if(get("TO_DATE").equals("today") || get("TO_DATE").equals("today"))
			to_dt	= getDate().substring(2, 8);
		else
			to_dt	= getDate().substring(2, 8);
		
		return;
	}
	public static void deleteFile(String path){
		File del	= new File(path);
		if(del.delete())
			WriteLog("deleteFile", "delete file OK ["+path+"]");
		else
			WriteLog("deleteFile", "delete file NOT OK ["+path+"]");
	
		return ;
	}
	public static void control_speed(String net_bps, int slen, boolean is_init) throws InterruptedException {
		
		long ctime 		= 0;
		long wait_secs 	= 0;

		/* millisec -> sec */
		ctime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		/*Util.WriteLog("control_speed", "########## static variable stime : [" + stime + "] ##########");*/
		if (is_init) {
			stime = ctime;
			if (net_bps.length() < 2 || net_bps.length() > 10) {
				Util.WriteLog("control_speed", "NET_BPS error(Ex:1M, 56K, 2400)");
				net_bps = "1M";
				Util.WriteLog("control_speed", "NET_BPS  => (" + net_bps + ")");
			} else
				Util.WriteLog("control_speed", "MAX_BPS=[" + net_bps + "]");

			//Util.WriteLog("control_speed", String.format("bps[%s], strlen bps[%d], number[%s], endflag[%s]", net_bps, net_bps.length(), net_bps.substring(0, net_bps.length()-1), net_bps.substring(net_bps.length()-1, net_bps.length())));
			if (net_bps.substring(net_bps.length() - 1, net_bps.length()).equals("M") || net_bps.substring(net_bps.length() - 1, net_bps.length()).equals("m"))
				max_cps = Integer.parseInt(net_bps.substring(0, net_bps.length() - 1)) * 128 * 1024; // M = 1024*1024

			else if (net_bps.substring(net_bps.length() - 1, net_bps.length()).equals("K") || net_bps.substring(net_bps.length() - 1, net_bps.length()).equals("k"))
				max_cps = Integer.parseInt(net_bps.substring(0, net_bps.length() - 1)) * 128;

			else {
				if (Integer.parseInt(net_bps.substring(0, net_bps.length() - 1)) > (1024 * 512))
					max_cps = (1024 * 512) / 8;
				else
					max_cps = Integer.parseInt(net_bps.substring(0, net_bps.length() - 1)) / 8;
			}
			Util.WriteLog("control_speed", "max cps : ["+max_cps+"]");
			if (max_cps <= 0 || max_cps > (WorkDefinition.MAX_KBPS * 128)) {
				Util.WriteLog("control_speed", "BPS OUT-OF-RANGE(0 <= bps[" + net_bps.substring(0, net_bps.length() - 1) + "] > [" + WorkDefinition.MAX_KBPS + "] Kbps");
				max_cps = WorkDefinition.MAX_KBPS * 128;
				Util.WriteLog("control_speed", "BPS => (" + WorkDefinition.MAX_KBPS + "Kbps)");
			}
			max_cps = (max_cps * 9) / 10;
			return;
		}
		/*Util.WriteLog("control_speed", "ctime:["+ctime+"], max cps:["+max_cps+"], olen:["+olen+"], slen:["+slen+"]");*/
		wait_secs = ((olen + slen) / max_cps) + stime - ctime;
		if (wait_secs > 0L) {
			Util.WriteLog("control_speed", "MAX BPS(" + ((max_cps * 10) / 9)/128 + "Kbps) above -> wait(" + wait_secs + ")=((" + olen + "+" + slen + ")/" + max_cps + ")-(" + (ctime - stime) + ")");
			Thread.sleep(wait_secs * 1000);
		}
		olen = olen + slen;
	}
	
	public static boolean snd_rcv_sessionKey(Socket serv, DataOutputStream dout, DataInputStream din, byte[] e_kbuf) throws IOException {
		boolean result	= false;
		try{
			dout	= new DataOutputStream(serv.getOutputStream());
			din		= new DataInputStream(serv.getInputStream());
			
			Util.WriteLog("snd_rcv_sessionKey", "ENC TYPE : ["+ksnetInternetEntSvrClient.enc_tp+"]");
			
			dout.write(e_kbuf, 0, e_kbuf.length);
			dout.flush();

			if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_SEED)){
				
				Util.WriteLog("snd_rcv_sessionKey", "KEY : [0x"+new java.math.BigInteger(keyInfo.getInstance().getP_kbuf()).toString(16)+"]");
				byte[]	rbuf	= new byte[7];
				din.readFully(rbuf);
				String	rmsg	= new String(rbuf, 0, rbuf.length, "EUC-KR");
			
				WriteLog("snd_rcv_sessionKey", "return msg : ["+rmsg+"]");

				if(rmsg.substring(4, 5).equals(WorkDefinition.MSG_TP_S) && rmsg.substring(5, 7).equals(WorkDefinition.RESULT_MSG_OK))	result = true;
				else																													result = false;
			}
			else if(ksnetInternetEntSvrClient.enc_tp.equals(WorkDefinition.ENCTP_KECB)){
				
				byte[]	rbuf	= new byte[7+34+1];
				din.read(rbuf);

				WriteLog("snd_rcv_sessionKey", "read_key_msg(S:"+rbuf.length+":"+hex_encode(rbuf, 0, rbuf.length) + ")");
				
				int midx = 7;
				
				int tlen = 2;
				String	resp_code	= new String(rbuf, midx, tlen, "8859_1");
				
				midx	+= tlen;
				tlen	= 16;
				
				String	trno		= new String(rbuf, midx, tlen, "8859_1");
				
				midx	+= tlen;
				tlen	= 16;
				
				String 	enc_iv		= new String(rbuf, midx, tlen, "8859_1");	//암호화된 Initial Vector
				midx	+= tlen;
				
				if(!resp_code.equals(WorkDefinition.FINE)){
					WriteLog("snd_rcv_sessionKey", "NEW KEY NOT-OK-MSG ~!!");
					result	= false;
				}
				
				eUtil.storeSessionKey(enc_iv.getBytes("8859_1"), trno.getBytes("8859_1"));
				
				result	= true;
				return result;
			}
		}catch(Exception e){
			WriteLog("snd_rcv_sessionKey", "Error : ["+e.getMessage()+"]");
		}
		
		return result;
	}
	public static String hex_encode(byte[] sbuf){
		if(sbuf == null)	return null;

		return hex_encode(sbuf, 0, sbuf.length);
	}
	public static String hex_encode(byte[] sbuf, int sidx, int len){
		if(sbuf	== null)	return null;

		int tidx	= sidx = len;
		if(tidx > sbuf.length)	tidx = sbuf.length;

		StringBuffer	sb = new StringBuffer();
		
		for(int i=sidx; i<tidx; i++)
			sb.append(Integer.toHexString((0xFF & sbuf[i]) | 0x0100).substring(1));
		
		return sb.toString();
	}
	public static String checkDataType(String type){
        if(type.equals("IY0") || type.equals("Y00") || type.equals("IY6") || type.equals("Y06") || type.equals("AY0") || type.equals("AY6"))
            return "BINARY".toString();
        
        else
            return "TEXT".toString();
    }
	public static boolean CheckSendData(){
		ksnetInternetEntSvrClient.fileformat		= new SendFileFormat[WorkDefinition.MAX_DAY_F_CNT];
		ksnetInternetEntSvrClient.prf_fileformat	= new SendFileFormat[WorkDefinition.MAX_DAY_F_CNT];
		
		try{
			File path	= new File(get("SEND_DIR"));
			synchronized (path) {
				File[] files = path.listFiles();
				for (File file : files) {
					if (file.isFile()) {
						if (!CheckFileState(get("SEND_DIR"), file.getName().toString()))
							continue;
					}
				}
			}
			if(ksnetInternetEntSvrClient.datalist_cnt != 0){
				ksnetInternetEntSvrClient.data_list	= new DataList[ksnetInternetEntSvrClient.datalist_cnt];
				for (int i = 0; i < ksnetInternetEntSvrClient.datalist_cnt; i++){
					Util.WriteLog("CheckSendData", "SEND FILENAME [" + ksnetInternetEntSvrClient.fileformat[i].toString() + "]");
					ksnetInternetEntSvrClient.data_list[i]	= new DataList(ksnetInternetEntSvrClient.fileformat[i]);
				}
			}
			if(ksnetInternetEntSvrClient.prf_datalist_cnt != 0){
				ksnetInternetEntSvrClient.prf_data_list	= new DataList[ksnetInternetEntSvrClient.prf_datalist_cnt];
				for (int i = 0; i < ksnetInternetEntSvrClient.prf_datalist_cnt; i++){
					Util.WriteLog("CheckSendData", "SEND FILENAME [" + ksnetInternetEntSvrClient.prf_fileformat[i].toString() + "]");	
					ksnetInternetEntSvrClient.prf_data_list[i]	= new DataList(ksnetInternetEntSvrClient.prf_fileformat[i]);
				}
			}
			return true;
		}catch(Exception e){
			WriteLog("CheckSendData", "ERROR=["+e.getMessage()+"]");
			return false;
		}
		
	}
	public static boolean CheckFileState(String path, String filename) throws Exception {

		byte[] in = new byte[1024];
		boolean endflag = false;

		WriteLog("CheckFileState", "FILE CHECKING.....");

		if (filename.substring(1, 4).equals(WorkDefinition.RESULT_FILE_FLAG)) {
			File f = new File(get("SEND_DIR") + System.getProperty("file.separator") + filename);

			if (f.delete())
				Util.WriteLog("CheckFileState", "delete file OK [ " + filename + " ]");

			else
				Util.WriteLog("CheckFileState", "delete file fail [ " + filename + " ]");

			return false;
		}
		
		if(filename.length() != 28){
			Util.WriteLog("CheckFileState", "INCORRECT FILENAME [ "+filename+" ] (ex ABRQYYYYMMDD_12340081R00.001)");
			return false;
		}
		
		if (!filename.substring(1, 4).equals(WorkDefinition.SEND_FILE_FLAG)) {
			Util.WriteLog("CheckFileState", "NOT SUBJECT FILE [ " + filename + " ]");
			return false;
		}

		if (!filename.substring(4, 12).equals(Util.getDate().substring(0, 8))) {
			Util.WriteLog("CheckFileState", "NOT WORK DATE FILE [ " + filename + " ]");
			return false;
		}

		if (filename.substring(17, 21).length() != 4) {
			Util.WriteLog("CheckFileState", "ABNORMAL RECV_CD [ " + filename.substring(13, 17).toString() + " ]");
			return false;
		}

		if (filename.substring(21, 24).length() != 3) {
			Util.WriteLog("CheckFileState", "ABNORMAL INFO_CD [ " + filename.substring(21, 24).toString() + " ]");
			return false;
		}
		if (filename.substring(25, 28).length() != 3){
			Util.WriteLog("CheckFileState", "ABNORMAL FILENAME [ "+ filename +"]");
		}
		
		int endflag_cnt = 0;
		//Proof type
		if(checkDataType(filename.substring(21, 24)).equals("BINARY")){
			RandomAccessFile	raf	= new RandomAccessFile(new File(get("SEND_DIR") + System.getProperty("file.separator") + filename), "r");
			
			int len = 0;
			if((len = raf.read(in)) < 0){
				Util.WriteLog("CheckFileState", "FILE OPEN ERROR");
				raf.close();
				return false;
			}
			raf.seek(0L);
			while (true) {
				Arrays.fill(in, (byte) 0x00);
				len = raf.read(in, 0, WorkDefinition.MAX_REC_SIZE);
				
				if (len <= 0)
					break;

				String trailerCheck = new String(in).substring(0, 1);
				if (trailerCheck.equals(WorkDefinition.TRAILER_FLAG_1) || trailerCheck.equals(WorkDefinition.TRAILER_FLAG_2) || trailerCheck.equals(WorkDefinition.TRAILER_FLAG_3))
					endflag = true;
				else
					endflag = false;
			}
			raf.close();
			if(!endflag){
				Util.WriteLog("CheckFileState", "ABNORMAL FILE(NO TRAILER FLAG) ["+filename+"]");
				return false;
			}
			
			/*file condition OK*/
			ksnetInternetEntSvrClient.prf_fileformat[ksnetInternetEntSvrClient.prf_datalist_cnt++] = new SendFileFormat(filename);	
		}
		
		//Normal type
		else {
			BufferedReader	br	= new BufferedReader(new FileReader(Util.get("SEND_DIR") + System.getProperty("file.separator") + filename));
			
			if(br.read() < 0){
				Util.WriteLog("CheckFileState", "FILE OPEN ERROR");
				br.close();
				return false;
			}
			while (true) {
				String line = null;
				if ((line = br.readLine()) == null)		break;

				else {
					if (line.substring(0, 1).equals(WorkDefinition.TRAILER_FLAG_1) || line.substring(0, 1).equals(WorkDefinition.TRAILER_FLAG_2) || line.substring(0, 1).equals(WorkDefinition.TRAILER_FLAG_3)){
						endflag_cnt++;
						endflag = true;
					}
					else
						endflag = false;
				}
			}
			br.close();
			if(!endflag && endflag_cnt != 1){
				Util.WriteLog("CheckFileState", "ABNORMAL FILE [ "+filename+"]");
				return false;
			}
			/*file condition OK*/
			ksnetInternetEntSvrClient.fileformat[ksnetInternetEntSvrClient.datalist_cnt++] = new SendFileFormat(filename);
		}
		
		WriteLog("CheckFileState", "FILE CONDITION FINE");
		
		return true;
	}
	public static boolean ResultTreat(DataList[] data_list, int cnt) throws IOException {
		BufferedWriter bw = null;
		Util.WriteLog("ResultTreat", "filename of data list : ["+data_list[0].getFilename()+"]");
		try {
			for (int i = 0; i < cnt; i++) {
				if (data_list[i].ret_cd.equals(WorkDefinition.FINE)) {
					bw = new BufferedWriter(new FileWriter(get("SEND_DIR")+ System.getProperty("file.separator") + data_list[i].filename.replace(WorkDefinition.SEND_FILE_FLAG, WorkDefinition.RESULT_FILE_FLAG) + "-" + WorkDefinition.RESULT_MSG_OK));

					Util.WriteLog("ResultTreat", "result filename : [" + get("SEND_DIR") + System.getProperty("file.separator") + data_list[i].filename.replace(WorkDefinition.SEND_FILE_FLAG, WorkDefinition.RESULT_FILE_FLAG) + "-" + WorkDefinition.RESULT_MSG_OK + "]");
					bw.close();	Thread.sleep(10);

					if (!UsedFile(data_list[i]))
						Util.WriteLog("ResultTreat", "usedfile move FAIL [" + data_list[i].filename + "]");

					else
						Util.WriteLog("ResultTreat", "usedfile move OK   [" + data_list[i].filename + "]");
				} else {
					bw = new BufferedWriter(new FileWriter(get("SEND_DIR") + System.getProperty("file.separator") +data_list[i].filename.replace(WorkDefinition.SEND_FILE_FLAG, WorkDefinition.RESULT_FILE_FLAG) + "-" + WorkDefinition.RESULT_MSG_FAIL + "_"  + data_list[i].ret_cd));
					bw.close();	Thread.sleep(10);
					Util.WriteLog("ResultTreat", "result filename : [" + get("SEND_DIR") + System.getProperty("file.separator") + data_list[i].filename.replace(WorkDefinition.SEND_FILE_FLAG, WorkDefinition.RESULT_FILE_FLAG) + "-" + WorkDefinition.RESULT_MSG_FAIL + "_" + data_list[i].ret_cd + "]");
				}
			}

		} catch (Exception e) {
			Util.WriteLog("ResultTreat", e.getMessage());
			bw.close();
			return false;
		}
		return true;
	}

	public static boolean UsedFile(DataList datalist) throws InterruptedException {

		int retry_cnt 	= 0;
		boolean result	= false;
		File src = new File(get("SEND_DIR")+System.getProperty("file.separator")+datalist.getFilename());

		Util.WriteLog("UsedFile", "src fullfilename : ["+src.getPath()+"]");
		
		if(!new File(get("USED_DIR")).exists())
			new File(get("USED_DIR")).mkdirs();
		
		File dst = new File(get("USED_DIR")+System.getProperty("file.separator")+datalist.getFilename()+"-OK_"+datalist.getEnd_time());
		Util.WriteLog("UsedFile", "dst fullfilename : ["+dst.getPath()+ "]");

		while (true) {
			retry_cnt++;
			if ((result = src.renameTo(dst)) == false && retry_cnt < 20){
				Thread.sleep(10);
				continue;
			}
			else
				break;
		}
		if (!result)
			return false;
		else
			return true;
	}
	public static boolean proc_check(String svc){
		try{
			if(svc.equals("Y")){
				WriteLog("serviceCheck", get("MONIT_TERM")+" min wait......");
				Thread.sleep(Long.parseLong(get("MONIT_TERM"))*60*1000);
				initStaticVariable();
				ksnetInternetEntSvrClient.conf.clear();
				System.gc();
				return true;
			}
			else{
				WriteLog("proc_check", "WORK END");
				return false;
			}
		}catch(Exception e){	return false;	}
	}
	public static String byteArrayToHex(byte[] src){
		StringBuilder	sb	= new StringBuilder();
		for(final byte b : src)
			sb.append(String.format("%02x", b&0xff));

		return sb.toString();
	}
}
