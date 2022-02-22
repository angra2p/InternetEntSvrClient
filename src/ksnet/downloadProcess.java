package ksnet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import bbnk.ProcBbdata;

public class downloadProcess {
	RandomAccessFile	raf;
	String				fullfilename;
	String				filename;
	String				f_type;
	String				send_cd;
	String				recv_cd;
	String				rmsg;
	String				seq_no;
	String				block_no;
	String				miss_block_no;
	String				miss_seq_no;
	byte[]				data_buf;
	byte[]				conv_buf;
	byte[]				shift_buf;
	char[]				miss;
	int					data_len;
	int					rec_len;
	int					bin_len;
	int					f_seq;
	long				curr_posit;
	long				ins_posit;
	long				shift_posit;
	public downloadProcess() {
		this.data_buf	= new byte[WorkDefinition.MAX_DATA_MSG_SIZE];
		this.miss		= new char[WorkDefinition.MAX_SEQ_CNT]		;
		this.f_seq		= 1		;
	}
	
	public boolean downloadProcess(Socket serv, DataInputStream din, DataOutputStream dout, String recv_cd, int datalist){
		try{
			Arrays.fill(this.miss, '0');
			while(true){
			
				byte[] rbyte	= readMsg.readData(serv, din);
			
				this.rmsg		= new String(rbyte, "EUC-KR");
				
				/*test*/
				Util.WriteLog("downloadProcess", "Msg=["+this.rmsg.substring(0, 96) +" ... ]");
				
				if(this.rmsg.substring(3, 10).equals(WorkDefinition.DATA_REQ_MSG) || this.rmsg.substring(3, 10).equals(WorkDefinition.MISS_DATA_MSG)){
					if(this.rmsg.substring(3, 10).equals(WorkDefinition.DATA_REQ_MSG) && this.rmsg.substring(44, 48).equals("0001") && this.rmsg.substring(48, 51).equals("001")){
						makeFname();
						
						this.bin_len		= Integer.parseInt(this.rmsg.substring(86, 90));
						
						/*압축파일인 경우*/
						if(Util.get("ZIP_YN").equals("Y"))
							this.fullfilename	= this.fullfilename + WorkDefinition.ZIP_EXP;
						
						Util.WriteLog("downloadProcess","fullfilename : ["+this.fullfilename+"]");
						
						this.raf	= new RandomAccessFile(this.fullfilename, "rw");
				
						Util.WriteLog("downloadProcess", "block_no=["+this.block_no+"], seq_no=["+this.seq_no+"], bin_len=["+this.bin_len+"]");
						
						System.arraycopy(rbyte, 96, this.data_buf, 0, this.bin_len);
						writeRecordBin(this.data_buf);
						
						this.miss[Integer.parseInt(this.seq_no) -1] = '1';
					}
					else if(this.rmsg.substring(3, 10).equals(WorkDefinition.MISS_DATA_MSG)){
						this.miss_block_no	= this.rmsg.substring(44, 48);
						this.miss_seq_no	= this.rmsg.substring(48, 51);
						Util.WriteLog("downloadProcess", "recv msg ["+this.rmsg+"]");
						
						Util.WriteLog("downloadProcess","m_block_no : ["+this.miss_block_no+"], m_seq_no : ["+this.miss_seq_no+"]");
						this.bin_len	= Integer.parseInt(this.rmsg.substring(86, 90));
						Util.WriteLog("downloadProcess","bin_len : ["+this.bin_len+"]");
						System.arraycopy(this.rmsg.getBytes(), 96, this.data_buf, 0, this.bin_len);
						writeMissRecordBin(this.data_buf);	
						
						this.miss[Integer.parseInt(this.miss_seq_no)-1] = '1';
					}	
					else{
						this.block_no		= this.rmsg.substring(44, 48);
						this.seq_no			= this.rmsg.substring(48, 51);
						this.bin_len		= Integer.parseInt(this.rmsg.substring(86, 90));	
						Util.WriteLog("downloadProcess", "block_no=["+this.block_no+"], seq_no=["+this.seq_no+"], bin_len=["+this.bin_len+"]");
					
						System.arraycopy(rbyte, 96, this.data_buf, 0, this.bin_len);
						writeRecordBin(this.data_buf);

						this.miss[Integer.parseInt(this.seq_no) -1] = '1';
					}
				}
				
				//miss require step
				else if(this.rmsg.substring(3, 10).equals(WorkDefinition.MISS_REQ_MSG)){
					Thread.sleep(10);
					
					byte[]	e_sbuf	= sendMsg.sendMsg(dout, this.f_type, WorkDefinition.MISS_REP_MSG, this.recv_cd, this.send_cd, "", Integer.parseInt(this.block_no), Integer.parseInt(this.seq_no), 0L, new String(this.miss));
					
					if(!sendMsg.snd_socket(serv, e_sbuf, dout))	{	Util.WriteLog("sendMsg", "snd_socket() Error in ["+WorkDefinition.MISS_REP_MSG+"]");	return false;}
					
					Arrays.fill(this.miss, '0');
					this.block_no	= "0";	
					this.seq_no 	= "0";	
				}
				// part end step
				else if(this.rmsg.substring(3, 10).equals(WorkDefinition.PART_END_REQ_MSG) && !Util.no_data_yn)
				{
					Thread.sleep(10);
					byte[]	e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.PART_END_REP_MSG, this.recv_cd, WorkDefinition.KSNET, "", 0, 0, 0, "");
					
					if(!sendMsg.snd_socket(serv, e_sbuf, dout))	{	Util.WriteLog("", "snd_socket() Error in ["+WorkDefinition.PART_END_REP_MSG+"]");	return false;}
					
					if(this.raf != null)
						this.raf.close();
					
					//decompress step
					if(Util.get("ZIP_YN").equals("Y") || Util.get("ZIP_YN").equals("y"))	decompressData();
					
					//DB처리
					if(Util.get("COCA_DB_YN").equals("Y"))
					{
						if(f_type.equals("C01")||f_type.equals("C02")||f_type.equals("C03")||f_type.equals("C04")||f_type.equals("C05")||f_type.equals("C06"))
						{
							ProcBbdata pbd	= new ProcBbdata();
							String db_driver	= Util.get("JDBC_DRIVER");
							String db_url		= Util.get("JDBC_URL");
							String db_user		= Util.get("JDBC_USER");
							String db_pwd		= Util.get("JDBC_PWD");
							
							if(!pbd.CorpCard2DB(this.fullfilename, f_type, db_driver, db_url, db_user, db_pwd, Util.get("LOG_DIR")))
								Util.WriteLog("ProcBbdata.CorpCard2DB", "DB Proc 4 ("+this.fullfilename+")  Fail ~!!! ");
							else
								Util.WriteLog("ProcBbdata.CorpCard2DB", "DB Proc 4 ("+this.fullfilename+")  Success ... ");
						}
					}
					
					//XML파일로 변환
					if(Util.get("COCA_XML_YN").equals("Y"))
					{
						if(f_type.equals("C01")||f_type.equals("C02")||f_type.equals("C03")||f_type.equals("C04")||f_type.equals("C05")||f_type.equals("C06"))
						{
							ProcBbdata pbd	= new ProcBbdata();
							String xml_fpath = pbd.CorpCard2Xml(this.fullfilename, f_type, Util.get("LOG_DIR"));
							
							if(xml_fpath.length() > 3)	Util.WriteLog("ProcBbdata.CorpCard2Xml", "xml_fpath=["+xml_fpath+"]  Success ... ");
							else	Util.WriteLog("ProcBbdata.CorpCard2Xml", "Fail ... ");
						}
					}	
					
					//JSON파일로 변환
					if(Util.get("COCA_JSON_YN").equals("Y"))
					{
						if(f_type.equals("C01")||f_type.equals("C02")||f_type.equals("C03")||f_type.equals("C04")||f_type.equals("C05")||f_type.equals("C06"))
						{
							ProcBbdata pbd	= new ProcBbdata();
							String json_fpath = pbd.CorpCard2Json(this.fullfilename, f_type, Util.get("LOG_DIR"));

							if(json_fpath.length() > 3)	Util.WriteLog("ProcBbdata.CorpCard2Json", "json_fpath=["+json_fpath+"]  Success ... ");
							else	Util.WriteLog("ProcBbdata.CorpCard2Json", "Fail ... ");
						}
					}
						
					break;
				}	
				// part end step and no Data
				else if(this.rmsg.substring(3, 10).equals(WorkDefinition.PART_END_REQ_MSG) && Util.no_data_yn)
				{
					Thread.sleep(10);
					
					byte[]	e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.PART_END_REP_MSG, this.rmsg.substring(20, 24), WorkDefinition.KSNET, "", 0, 0, 0, "");
					
					if(!sendMsg.snd_socket(serv, e_sbuf, dout)){
						Util.WriteLog("downloadProcess", "Error step ["+WorkDefinition.PART_END_REP_MSG+"]");
						return false;
					}
					this.curr_posit = 0L;
					break;
				}
				
			}//while end
			
		}catch(Exception e){
			
			Util.WriteLog("downloadProcess", "ERROR=["+e.getMessage()+"]");
			try{	
				//if file transfer is failed, received file is delete 
				File	f	= new File(Util.get("RECV_DIR")+System.getProperty("file.separator")+this.fullfilename);
				
				Util.WriteLog("downloadProcess", "delete file=["+Util.get("RECV_DIR")+System.getProperty("file.separator")+this.fullfilename+"]");
				
				if(f.delete())		Util.WriteLog("downloadProcess", this.fullfilename+" rollback~!!");
				else				Util.WriteLog("downloadProcess", this.fullfilename+" delete error~!");
			}catch(Exception e1){
				Util.WriteLog("downloadProcess", "delete file error=["+e1.getMessage()+"]");
			}
		
		}finally{
			try{if(this.raf != null)	
				this.raf.close();
			}catch(Exception e) {}
		
		}
		return true;
	}
	
	/*
	 - UTF-8  : Han character 3byte
	 - EUC-KR : han Character 2byte
	*/
	public boolean writeRecordBin(byte[] data_msg){
		
		try{
			this.raf.write(data_msg, 0, this.bin_len);
			this.curr_posit = this.raf.getFilePointer();
			Util.WriteLog("writeRecordBin","file posit : ["+this.curr_posit+"]");
		}catch(Exception e){
			Util.WriteLog("writeRecordBin","error : ["+e.getMessage()+"]");
			return false;
		}
		return true;
	}
	public void decompressData(){
		try{
			String				dst	= this.fullfilename.replace(WorkDefinition.ZIP_EXP, "");
			GZIPInputStream		gis	= new GZIPInputStream(new FileInputStream(this.fullfilename));
			FileOutputStream	fos	= new FileOutputStream(dst);
			byte[]				buf	= new byte[WorkDefinition.MAX_REC_SIZE];
			int					len ;
			
			Util.WriteLog("decompressData","File Decompressing...");
			
			while((len = gis.read(buf)) != -1)
				fos.write(buf, 0, len);

			gis.close();
			fos.close();

			/*0.05sec wait*/
			Thread.sleep(50);
			Util.WriteLog("decompressData", "delete gzip file = ["+this.fullfilename+"]");
			
			File del	= new File(this.fullfilename);
			
			if(del.delete())	Util.WriteLog("decompressData", "DELETE OK ["+this.fullfilename+"]");
			else				Util.WriteLog("decompressData", "DELETE NOT OK ["+this.fullfilename+"]");
		}
		catch(Exception e){
			Util.WriteLog("decompressData", "ERROR : ["+e.getMessage()+"]");	
		}
	}
	
	//Data write in file Method in gzip file
	public boolean writeMissRecordBin(byte[] data_msg){
		try{
			this.ins_posit		= ((Integer.parseInt(this.miss_block_no) - 1) * WorkDefinition.MAX_SEQ_CNT * WorkDefinition.MAX_DATA_MSG_SIZE) + ((Integer.parseInt(this.miss_seq_no) - 1) * WorkDefinition.MAX_DATA_MSG_SIZE);
			this.shift_buf		= new byte[(int)(this.curr_posit - this.ins_posit)];
			this.shift_posit	= this.bin_len;

			this.raf.seek(this.ins_posit);
			this.raf.readFully(this.shift_buf);

			Util.WriteLog("writeMissRecordBin","ins_posit : ["+this.ins_posit+"]");
			this.raf.write(data_msg, 0, this.bin_len);
			this.raf.seek(this.curr_posit);
		}catch(Exception e){
			Util.WriteLog("writeMissRecordBin","error : ["+e.getMessage()+"]");
			return false;
		}
		return true;
	}
	
	public void makeFname(){
		StringBuilder	sb 	= new StringBuilder();
		
		this.f_type		= this.rmsg.substring(0, 3)		;
		this.send_cd	= this.rmsg.substring(10, 14)	;
		this.recv_cd	= this.rmsg.substring(20, 24)	;
		this.block_no	= this.rmsg.substring(44, 48)	;
		this.seq_no		= this.rmsg.substring(48, 51)	;
		
		sb.append(Util.get("RECV_DIR")).append(System.getProperty("file.separator")).append("A").append(WorkDefinition.RECV_FILE_FLAG);
		sb.append(Util.getDate().substring(0, 8)).append("_").append(this.send_cd).append(this.recv_cd).append(this.f_type).append(".").append(String.format("%03d", this.f_seq));
		
		this.fullfilename	= sb.toString();
		while(true){
			if(new File(this.fullfilename).exists())
				this.fullfilename	= sb.replace(this.fullfilename.length()-3, this.fullfilename.length(), String.format("%03d", ++this.f_seq)).toString();
			else
				break;
		}
	}
	public void decompressData(String gzipFile, String dstFile){
		try{
			GZIPInputStream		gis	= new GZIPInputStream(new FileInputStream(gzipFile));
			FileOutputStream	fos	= new FileOutputStream(dstFile);
			byte[]				buf	= new byte[WorkDefinition.MAX_REC_SIZE];
			int					len ;
			
			//decompress work
			while((len = gis.read(buf)) != -1)
				fos.write(buf, 0, len);

			gis.close();	fos.close();
		}catch(Exception e){
			Util.WriteLog("decompressData","error ["+e.getMessage()+"]");
		}
		return ;
	}
}
