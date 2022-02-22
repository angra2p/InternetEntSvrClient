package ksnet;

/*
 - 압축기능 추가(2017-06-12)
 - 전용선 용 송/수신 기능 완료
*/

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.zip.*;

public class uploadProcessBin {
	RandomAccessFile	raf;
	NegoMsg				s_negomsg;
	String 				filename;
	String				org_filename;		//temporary filename
	String				send_msg;
	String				rmsg;
	String				result_cd;
	String				miss_cnt;
	String 				miss_chk_msg;
	byte[]				data_buf;
	byte[]				send_buf;
	int 				read_len;
	int					block_no;
	int 				seq_no;
	int					retry_cnt;
	long				f_curr_posit;
	long				f_posit;
	long				f_size;
	boolean 			EOF_FLAG;

	public uploadProcessBin(){
			this.data_buf		= new byte[WorkDefinition.MAX_DATA_MSG_SIZE];
			this.send_buf		= new byte[WorkDefinition.MAX_MSG_SIZE];
			this.block_no		= 1;
	}
	public synchronized boolean upload(DataList datalist, Socket serv, DataOutputStream dout, DataInputStream din) throws IOException, InterruptedException{
			this.filename	= datalist.filename;
			if(Util.get("ZIP_YN").equals("Y") && Util.checkDataType(datalist.getF_type()).equals("TEXT"))	compressData();

			Util.WriteLog("uploadProcessBin","filename : ["+this.filename+"]");
			try{
				this.raf	= new RandomAccessFile(Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename, "rw");
				this.f_size	= this.raf.length();
			} catch(Exception e){
				Util.WriteLog("uploadProcessBin", "Access File error=["+e.getMessage()+"]");
				this.raf.close();
				return false;
			}

			this.s_negomsg	= new NegoMsg();

			Util.WriteLog("uploadProcessBin", "FILE SIZE : ["+this.f_size+"]");
			
			while(true){
				Arrays.fill(this.data_buf, (byte)0x00);	Arrays.fill(this.send_buf,  (byte)0x00);
				this.read_len		= this.raf.read(data_buf, 0, WorkDefinition.MAX_DATA_MSG_SIZE);
				this.f_curr_posit	= this.raf.getFilePointer();
				if(this.read_len == -1)
						this.EOF_FLAG = true;
				if(this.read_len > 0){
						this.seq_no++;
						if(this.seq_no > WorkDefinition.MAX_SEQ_CNT){
								this.seq_no = 1;
								this.block_no++;
						}
				
						Util.WriteLog("uploadProcessBin", "BLOCK_NO=["+String.format("%04d", this.block_no)+"], SEQ_NO=["+String.format("%03d", this.seq_no)+"]");
						
						sendMsg.sendData(serv, dout, datalist.getF_type(), WorkDefinition.DATA_REQ_MSG, datalist.getSend_cd(), datalist.getRecv_cd(), this.block_no, this.seq_no, 1, this.read_len, this.data_buf, this.send_buf);
						//0.01sec 대기
						Thread.sleep(10);
				}
				if(this.seq_no == WorkDefinition.MAX_SEQ_CNT || this.EOF_FLAG == true){
						if(this.seq_no == WorkDefinition.MAX_SEQ_CNT && this.EOF_FLAG == true)
								this.seq_no = 0;

						Util.WriteLog("uploadProcessBin", String.format("BLOCK_NO=[%04d], SEQ_NO=[%03d]", this.block_no, this.seq_no));
						this.retry_cnt = 0;
						while(true){
								if(this.retry_cnt > 0)
										Thread.sleep(10000);		//10sec sleep

								byte[]	e_sbuf	= sendMsg.sendMsg(dout, datalist.getF_type(), WorkDefinition.MISS_REQ_MSG, datalist.getSend_cd(), datalist.getRecv_cd(), "", this.block_no, this.seq_no, 0L, "");
							
								this.rmsg		= sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din, WorkDefinition.MISS_REP_MSG);
								
								//0.01sec wait
								Thread.sleep(10);
								
								this.miss_cnt	 = this.rmsg.substring(2, 5);
								
								if(!this.rmsg.substring(0, 2).equals(WorkDefinition.FINE)){
									Util.WriteLog("uploadProcessBin", "RETRUN ERROR CODE=["+this.rmsg.substring(0, 2)+"]");
									datalist.setRet_cd(rmsg.substring(0, 2));
									this.raf.close();
									return false;
								}
								Util.WriteLog("uploadProcessBin", "BLOCK_NO=["+this.rmsg.substring(5, 9)+"], SEQ_NO=["+this.rmsg.substring(9, 12)+"],MISS ARRAY=["+this.rmsg.substring(12, 112)+"]");
								Util.WriteLog("uploadProcessBin", "MISS COUNT=["+Integer.parseInt(this.miss_cnt)+"]");

								if(Integer.parseInt(this.miss_cnt) == 0)
										break;
								else{
										if(this.retry_cnt > WorkDefinition.MAX_MISS_REQ_RETRY){
											Util.WriteLog("uploadProcessBin", "Exceed Max Miss Require retry count");
											this.raf.close();
											return false;
										}
										//0410100
										readMissData(dout, serv, datalist);

								}	//else end
						}	//while end
				}
				if(this.EOF_FLAG){
					//압축파일 삭제 및 원본파일 바꿔치기
					if(Util.get("ZIP_YN").equals("Y") && Util.checkDataType(this.filename.substring(21, 24).toString()).equals("TEXT")){
							this.raf.close();

							//압축파일 삭제
							Util.deleteFile(Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename);

							this.filename	= this.org_filename;
							this.raf		= new RandomAccessFile(Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename, "r");
							this.f_size		= this.raf.length();
							Util.WriteLog("uploadProcessBin","FILE SIZE : ["+this.f_size+"]");
					}
					
					byte[]	e_sbuf	= sendMsg.sendMsg(dout, "000", WorkDefinition.PART_END_REQ_MSG, Util.get("KSNET_CD"), WorkDefinition.KSNET, "", 0, 0, this.f_size, "");
					
					Thread.sleep(10);
					this.rmsg	= sendMsg.snd_rcv_socket(serv, e_sbuf, dout, din);
					
					if(!this.rmsg.equals(WorkDefinition.FINE)){
						Util.WriteLog("uploadProcessBin", "ERROR STEP IN ["+WorkDefinition.PART_END_REP_MSG+"] ERROR CODE=["+this.rmsg+"]");
						datalist.setRet_cd(this.rmsg);
						this.raf.close();
						return false;
					}
					
					datalist.setRet_cd(WorkDefinition.FINE);
					datalist.setEnd_time(Util.getDate().substring(8, 14));
					this.raf.close();
					break;
			}	//max sequence or EOF
			else
				continue;
		}	//while end
		Util.WriteLog("uploadProcessBin", "FILE TRANSMIT COMPLETION");

		return true;
	}
	public synchronized boolean compressData(){
			byte[]			buf	= new byte[WorkDefinition.MAX_DATA_MSG_SIZE];
			int				len ;
			try{
			GZIPOutputStream	gzout	= new GZIPOutputStream(new FileOutputStream(Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename+".gz"));
			FileInputStream		fi		= new FileInputStream(Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename);
			
			//File compression
			while((len = fi.read(buf)) > 0){
				gzout.write(buf, 0, len);
			}
			this.org_filename	= this.filename;
			this.filename		= this.filename + WorkDefinition.ZIP_EXP;
		
			fi.close()		;	
			gzout.finish()	;	
			gzout.close()	;
			
			Util.WriteLog("compressData","COMPRESS FILE ["+Util.get("SEND_DIR")+System.getProperty("file.separator")+this.org_filename+"] => ["+Util.get("SEND_DIR")+System.getProperty("file.separator")+this.filename+"]");
		
			return true;
		}catch(Exception e){
			Util.WriteLog("compressData","ERROR : ["+e.getMessage()+"]");
			return false;
		}
	}
	public void readMissData(DataOutputStream dout, Socket serv, DataList datalist){
		try{
			this.block_no		= Integer.parseInt(this.rmsg.substring(5, 9 ));
			this.seq_no			= Integer.parseInt(this.rmsg.substring(9, 12));
			this.miss_chk_msg	= this.rmsg.substring(12, 112);
		
			for(int i=0; i<this.seq_no; i++){
				if(this.miss_chk_msg.substring(i, i+1).equals("0")){
					this.f_posit	= (this.block_no - 1) * WorkDefinition.MAX_SEQ_CNT * WorkDefinition.MAX_DATA_MSG_SIZE + i * WorkDefinition.MAX_DATA_MSG_SIZE;
					this.raf.seek(this.f_posit);
					this.read_len	= this.raf.read(this.data_buf, 0, WorkDefinition.MAX_DATA_MSG_SIZE);
					
					sendMsg.sendData(serv, dout, datalist.getF_type(), WorkDefinition.MISS_DATA_MSG, datalist.getSend_cd(), datalist.getRecv_cd(), this.block_no, i+1, 1, this.read_len, this.data_buf, this.send_buf);
					Util.WriteLog("readMissData", new String(this.send_buf).substring(0, 100) + " ...");
				}
			}
			
			//file pointer reset
			this.raf.seek(this.f_curr_posit);
			this.retry_cnt++;
		}catch(Exception e){
			Util.WriteLog("readMissData", "ERROR : ["+e.getMessage()+"]");
		}
	}
}
