package ksnet;

import java.io.*;
import java.util.*;

public class ksnetInternetEntSvrClient {
	//환경설정 파일 내역 
	static HashMap<String, String>conf	= new HashMap<String, String>();
	static int 				datalist_cnt	; 
	static int				prf_datalist_cnt;
	static String			enc_tp		;
	static SendFileFormat[] fileformat		;	
	static SendFileFormat[]	prf_fileformat	;
	static DataList[] 		data_list		;
	static DataList[]		prf_data_list	;

	public static void main(String[] args) throws Exception {
		while (true) {
			if(!Util.setConf(args[0]))
				return ;
			
			// Configure setting
			Util.setEnv();

			String	svc_check		= args[1].toString();
			
			Util.WriteLog("ksnetInternetEntSvrClient", "SERVER_IP=["+conf.get("SERVER_IP")+"], SERVER_PORT=["+conf.get("SERVER_PORT")+"]");
			Util.WriteLog("ksnetInternetEntSvrClient", "PRF_SERVER_IP=["+conf.get("PRF_SERVER_IP")+"], PRF_SERVER_PORT=["+conf.get("PRF_SERVER_PORT")+"]");	
			Util.WriteLog("ksnetInternetEntSvrClient", "SEND_DIR=["+ conf.get("SEND_DIR") + "], RECV_DIR=["+conf.get("RECV_DIR")+"], KSNET_CD=["+conf.get("KSNET_CD")+"], svc_check=["+svc_check+"]" );
			
			//command type
			if(svc_check.equals("N")){
				if(args[2].equals("S")){
					Updn.updnSendData(args[3], args[4], args[5], args[6], args[7]);
					break;
				}
				else if(args[2].equals("R")){
					Updn.updnRecvData(args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
					break;
				}
			}
			else{
				if(!Util.CheckSendData()){
					Util.WriteLog("ksnetInternetEntSvrClient", "CheckSendData() LOGIC ERROR");
					Util.initStaticVariable();
					Util.WriteLog("ksnetInternetEntSvrClient", Util.get("MONIT_TERM")+" min wait.......");
					Thread.sleep(Long.parseLong(Util.get("MONIT_TERM")) * 60 * 1000);
					continue;
				}
		
				Util.WriteLog("ksnetInternetEntSvrClient", "NORMAL FIRM DATA COUNT : ["+datalist_cnt+"] PROOF FIRM DATA COUNT : ["+prf_datalist_cnt+"]");
		
				if(datalist_cnt != 0){
					// Encryption Type Setting
					enc_tp		= WorkDefinition.ENCTP_SEED;
					
					if(!Util.SendData(data_list, datalist_cnt))
						Util.WriteLog("SendData", "Send Data Ligic Error");
					
					if(!Util.ResultTreat(data_list, datalist_cnt))
						Util.WriteLog("ResultTreat", "Result Treat Error");
				}
				
				if(prf_datalist_cnt != 0){
					//control_speed variable initialization
					Util.stime = 0L;	Util.olen = 0L;		Util.max_cps = 0;
					
					// Encryption Type Setting
					enc_tp			= WorkDefinition.ENCTP_KECB;
					
					if(!Util.SendData(prf_data_list, prf_datalist_cnt))
						Util.WriteLog("SendData", "SendData() Logic Error");
					
					if(!Util.ResultTreat(prf_data_list, prf_datalist_cnt))
						Util.WriteLog("ResultTreat", "Proof Result Treat Error");
				}
				
				// static variable initialization
				Util.initStaticVariable();
				
				enc_tp	= WorkDefinition.ENCTP_SEED;
				
				if(!Util.RecvData()){
					Util.WriteLog("RecvData", "RECV DATA LOGIC ERROR");
					Util.initStaticVariable();
				}
				
				if(!Util.proc_check(svc_check))	break;
			}	
		}//while end
		return;
	}
}

