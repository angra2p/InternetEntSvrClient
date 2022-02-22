package ksnet;

import java.io.*;
import java.net.*;

public class readMsg {
	
	public void readMsg(){}
	
	/*
	 * Function		: ��ȣȭ��(��) �������� ����.
	 * Parameter	:
	 	- rmsg		: ���ŵ� �� ����
		- msg_type	: ���������ڵ�
	 * return value	: String
	 */
	public String readMsg(String rmsg, String msg_type) {
		StringBuilder	sb	= new StringBuilder();
		
		try {
			if (!msg_type.equals(rmsg.substring(3, 10))) {
				Util.WriteLog("readMsg", "ERROR ABNORMAL MSG");
				return WorkDefinition.ERROR_ABNORMAL_MSG;
			}
			if (msg_type.equals(WorkDefinition.MISS_REP_MSG)) {
				String	block_no	= rmsg.substring(44, 48 );
				String	seq_no		= rmsg.substring(48, 51 );
				String	err_cd 		= rmsg.substring(51, 53 );
				String	miss_cnt 	= rmsg.substring(57, 60 );
				String	miss_data 	= rmsg.substring(96, 196);
				
				//�����ڵ� + ������� + ����ȣ + ��������ȣ + ����迭
				sb.append(err_cd).append(miss_cnt).append(block_no).append(seq_no).append(miss_data);
				return sb.toString();
			}
			else if(msg_type.equals(WorkDefinition.TRANS_REP_MSG) && rmsg.substring(56, 57).equals(WorkDefinition.RECV_REQ)){
				
				//���ŵ����� ������ no_data_yn true�� ����
				if(rmsg.substring(51, 53).equals(WorkDefinition.ERROR_NO_DATA))	
					Util.no_data_yn = true;
				else 
					Util.no_data_yn = false;
				
				String	rtn = sb.append(rmsg.substring(51, 53)).append(rmsg.substring(96, 104)).toString();
			
				//return code + send data count + total data count
				return rtn;
				
			}
			
			Util.WriteLog("readMsg", "Msg=["+rmsg+"]");
			
			return rmsg.substring(51, 53);
		} catch (Exception e) {
			Util.WriteLog("readMsg", msg_type +" step's error : ["+ e.getMessage()+"]");
			return null;
		}
	}
	
	/*
	 * Function  : ��ȣȭ�� ���� ����
	 * Parameter : 
	 	- serv : socket
		- din  : input stream
		- msg_type : ���������ڵ�
	 * return type : String(�����ڵ�)
	 */
	public static String readMsg(Socket serv, DataInputStream din, String msg_type){
		byte[]		len_buf	= new byte[4]	;
		StringBuilder	sb	= new StringBuilder();

		if(!serv.isConnected())	Util.WriteLog("readMsg", "server socket is closed~!");
		try{
			String	rmsg = null;
			din	= new DataInputStream(serv.getInputStream());
			
			din.readFully(len_buf);

			int		len		= Integer.parseInt(new String(len_buf, "EUC-KR"));
			byte[]	tbuf	= new byte[len]		;
			byte[]	rbuf	= new byte[len-1]	;

			din.readFully(tbuf);

			if(new String(tbuf, 0, 1, "EUC-KR").equals(WorkDefinition.MSG_TP_D))
				System.arraycopy(tbuf, 1, rbuf, 0, len-1);
			else{
				Util.WriteLog("readMsg", "Abnormal MSG TYPE~!");
				return WorkDefinition.ERROR_ABNORMAL_MSG;
			}
		
			rmsg	= new String(eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), rbuf), "EUC-KR");
			Util.WriteLog("readMsg", "Msg=["+rmsg+"]");
			
			if(msg_type.equals(WorkDefinition.TRANS_REP_MSG) && rmsg.substring(56, 57).equals(WorkDefinition.RECV_REQ))
			{
				if(rmsg.substring(51, 53).equals(WorkDefinition.ERROR_NO_DATA))
					Util.no_data_yn = true;
				else
					Util.no_data_yn = false;
				
				return sb.append(rmsg.substring(51, 53)).append(rmsg.substring(96, 104)).toString();
			}
			else
				return rmsg.substring(51, 53).toString();
		}catch(Exception e){
			Util.WriteLog("readMsg", "Error=["+e.getMessage()+"]");
			return WorkDefinition.ERROR_ETC;
		}
	}
	/*
	 * Function  : �����ͼۼ��� ���� ��ȣȭ�� ���� ��ȣȭ
	 * Parameter : 
	 	- serv : socket
		- din  : Input Stream
	 * return type :  ��ȣȭ �� byte[] 
	 */
	 public static byte[] readData(Socket serv, DataInputStream din){
		
		byte[]	len_buf	= new byte[4];
		try{
			din	= new DataInputStream(serv.getInputStream());
			
			din.readFully(len_buf);
			
			int		len		= Integer.parseInt(new String(len_buf, "EUC-KR"));
			byte[]	tbuf	= new byte[len]		;
			byte[]	rbuf	= new byte[len-1]	;
		
			din.readFully(tbuf);

			if(new String(tbuf, 0, 1, "EUC-KR").equals(WorkDefinition.MSG_TP_D))
				System.arraycopy(tbuf, 1, rbuf, 0, len-1);
			else{
				Util.WriteLog("readData", "ABNORMAL MSG TYPE~!");
				return new byte[0];
			}
			
			//��ȣȭ�� �������� ��ȣȭ
			return eUtil.ks_seed_decrypt(keyInfo.getInstance().getP_kbuf(), rbuf);
			
		}catch(Exception e){
			Util.WriteLog("readData", "Error=["+e.getMessage()+"]");
		}
	 	return new byte[0];
	 }
}

