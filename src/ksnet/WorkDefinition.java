package ksnet;

public class WorkDefinition {
	public WorkDefinition(){}
	
	public static final int RECV_TIMEOUT				= 180;
	public static final int MSG_LENGTH					= 4 ;
	public static final int MAX_REC_SIZE				= 1024;
	public static final int MAX_KBPS					= 1024;
	public static final int MAX_DATA_MSG_SIZE			= 3200;
	public static final int MAX_MSG_SIZE				= 3300;
	public static final int MAX_E_MSG_SIZE				= 10240;
	public static final int MAX_SEQ_CNT					= 100 ;
	public static final int MAX_DAY_F_CNT				= 999 ;
	public static final int MAX_SUB_MSG_SIZE			= 9999;
	
	public static final String FINE						= "00" ;
	public static final String ERROR_FILE_CREAT 		= "01" ;
	public static final String ERROR_FILE_WRITE 		= "02" ;
	public static final String ERROR_MISMATCH_RECV_CNT 	= "03" ;
	public static final String ERROR_FILE_OPEN 			= "04" ;
	public static final String ERROR_GET_FILENAME 		= "05" ; 
	public static final String ERROR_FILE_INSERT_DB 	= "06" ;
	public static final String ERROR_FILE_READ 			= "07" ;
	public static final String ERROR_REQTDATE_SMALL 	= "08" ;
	public static final String ERROR_CONNECT 			= "79" ;       
	public static final String ERROR_MSG_SEND 			= "80" ;       
	public static final String ERROR_MSG_RECV 			= "81" ;       
	public static final String ERROR_DB_EXECUTE 		= "82" ;       
	public static final String ERROR_ESSENTIAL_FIELD 	= "83" ;       
	public static final String ERROR_NO_DATA 			= "84" ;       
	public static final String ERROR_USER_CERT 			= "85" ;
	public static final String ERROR_DATA_HEADER 		= "86" ;
	public static final String ERROR_UNRESIST_SHOP 		= "87" ;       
	public static final String ERROR_ABNORMAL_MSG 		= "88" ;       
	public static final String ERROR_DB_OPEN_ERROR 		= "89" ;
	public static final String ERROR_ETC 				= "99" ;
	
	
	public static final String OPEN_REQ_MSG 			= "0800100" ;   
	public static final String OPEN_REP_MSG 			= "0810100" ;   
	public static final String TRANS_REQ_MSG 			= "0700100" ;   
	public static final String TRANS_REP_MSG 			= "0720100" ;   
	public static final String DATA_REQ_MSG 			= "0710100" ;   
	public static final String MISS_REQ_MSG 			= "0400100" ;   
	public static final String MISS_REP_MSG 			= "0420100" ;   
	public static final String MISS_DATA_MSG 			= "0410100" ;   
	public static final String PART_END_REQ_MSG 		= "0800800" ;   
	public static final String PART_END_REP_MSG 		= "0810800" ;   
	public static final String CLOSE_REQ_MSG 			= "0800900" ;   
	public static final String CLOSE_REP_MSG 			= "0810900" ;   
	
	public static final String SEND_REQ					= "S" ;         
	public static final String RECV_REQ 				= "R" ;  

	public static final String MSG_TP_S					= "S" ;		/*세션키 전송전문*/
	public static final String MSG_TP_D					= "D" ;		/*일반 암호화전문*/
	
	public static final int MAX_MISS_REQ_RETRY			= 10;
	public static final String KSNET					= "KSNET" ;
	public static final String USED_DIR					= "usedfile/";
	public static final String TRAILER_FLAG_1			= "E";
	public static final String TRAILER_FLAG_2			= "3";
	public static final String TRAILER_FLAG_3			= "T";
	public static final String SEND_FILE_FLAG			= "BRQ";
	public static final String RESULT_FILE_FLAG			= "BRP";
	public static final String RECV_FILE_FLAG			= "BRR";
	public static final String RESULT_MSG_OK			= "OK";
	public static final String RESULT_MSG_FAIL			= "FAIL";
	public static final String ZIP_EXP					= ".gz";
	public static final int	   MAX_RECVINFO_CNT			= 128;
	public static final String ICD_PRF					= "Y00";
	public static final String ICD_AFTINF				= "Y06";
	public static final String ICD_INTPRF				= "IY0";
	public static final String ICD_INTAFTINF			= "IY6";
	public static final String ICD_BATPRF				= "AY0";
	public static final String ICD_BATAFTINF			= "AY6";

	/*암호화 방식*/
	public static final String ENCTP_SEED				= "SEED";
	public static final String ENCTP_KECB				= "KECB";

	public static final byte C_STX						= 0x02;
	public static final byte C_ETX						= 0x03;
}

