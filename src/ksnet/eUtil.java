package ksnet;

import java.io.*;
import java.util.*;
import java.math.*;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.*;
import javax.crypto.spec.*;

//import ksnet.encrypt.KSBankSeed;

public class eUtil{
	

	public eUtil(){}

	public static byte[] GenerateKey(){
		
		StringBuffer	sb 	= new StringBuffer();
		byte[]			buf = null;

		try{
			buf = sb.append(System.currentTimeMillis()).append(Long.MAX_VALUE).substring(0, 16).getBytes("ksc5601");
		}catch(Exception e){
			Util.WriteLog("GenerateKey", "Error : ["+e.getMessage()+"]");
		}

		return buf;
	}
	public static byte[] setEncsession(byte[] p_kbuf, byte[] e_kbuf){
		byte[]	sbuf 	= new byte[4+1+e_kbuf.length];
		String	h_msg	= String.format("%04d%s", e_kbuf.length+1, WorkDefinition.MSG_TP_S);
		
		System.arraycopy(h_msg.getBytes(), 0, sbuf, 0, h_msg.getBytes().length);
		System.arraycopy(e_kbuf, 0, sbuf, h_msg.getBytes().length, e_kbuf.length);

		return sbuf;
	}
	public static byte[] kscms_decrypt(byte[] p_kbuf, byte[] e_rbuf)
	{
			try
			{
					return ks_seed_decrypt(p_kbuf,e_rbuf);
			}catch(Exception e)
			{
					Util.WriteLog("kscms_decrypt", "Error : ["+e.getMessage()+"]");
					e.printStackTrace();
			}
			return null;
	}
	public static byte[] ks_seed_decrypt(byte[] kbuf, byte[] mbuf) throws java.security.NoSuchProviderException,javax.crypto.NoSuchPaddingException,java.security.InvalidAlgorithmParameterException,javax.crypto.BadPaddingException,java.security.NoSuchAlgorithmException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException
	{
			byte tdata[]	= new  KSBankSeed(kbuf).cbc_decrypt(mbuf) ;
			return tdata;
	}

	public static byte[] ks_seed_encrypt(byte[] kbuf, byte[] mbuf) throws java.security.NoSuchProviderException,javax.crypto.BadPaddingException,javax.crypto.NoSuchPaddingException,java.security.InvalidAlgorithmParameterException,java.security.NoSuchAlgorithmException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException
	{
			byte tdata[]	= new  KSBankSeed(kbuf).cbc_encrypt(mbuf) ;
			return tdata;
	}

	public static byte[] ks_rsa_encrypt(byte[] sbuf) throws java.security.NoSuchProviderException,javax.crypto.NoSuchPaddingException,javax.crypto.BadPaddingException,java.security.NoSuchAlgorithmException,java.security.spec.InvalidKeySpecException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException
	{
			BigInteger modulus			= new BigInteger("d4846c2b8228dddfab9e614da2a324c1cc7b29d848cc005624d3a09667a2aab9073290bace6aa536ddceb3c47ddda78d9954da06c83aa65b939c5ec773a3787e71bec5a1c077bb446c06b393d2537967645d386b4b0b4ec21372fdc728c56693028c1c3915c1c4279793eb3dccefd6bf49b86cc7d88a47b0d44aba9e73750fcd",16);
			BigInteger publicExponent	= new BigInteger("0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001",16);

			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, publicExponent);

			KeyFactory keyfactory = KeyFactory.getInstance("RSA");
			java.security.PublicKey publickey = keyfactory.generatePublic(pubKeySpec);

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			cipher.init(Cipher.ENCRYPT_MODE, publickey);//, fixedsecurerandom);
			
			byte[] rbuf = cipher.doFinal(sbuf);

			return rbuf;
	}
	public static byte[] make_session_key() throws UnsupportedEncodingException{
		byte[] 	k16		= new byte[16];
		Random	random	= new Random();

		random.nextBytes(k16);
		//SO_RND.nextBytes(k16);
		
		k16[15]	= 0;

		for(int i=0; i < 15; i++)
			k16[15]	= (byte) (k16[15] ^ k16[i]);
	
		//Util.WriteLog("make_session_key", "session key=["+new String(k16, "EUC-KR")+"]");
		Util.WriteLog("make_session_key", "session key creation");
		return k16;
	}
	public static byte[] encrypt_rsa_2048(byte[] aes_key){
		try{
			BigInteger modulus 			= new BigInteger("00ef5cd77cc2e16c7c86b216143ce973c05a1ab5717851250ac56cb1ca6bc450118b0e37939049c459bdb8a109b13101952025efb32646271b2616b7fe956ccd8792e60f57155d1ac9d36fa961f7b36776881334506039cca83e34a0e7a684639c6236d09c810cbedb950cdc9295ead4203381861c0eff68d12d193444991df1644f5f7ac4c5a20d3ef418448f238f255627633b4d3dfe0287ada528cf00c46ba452f93cbec551d8c388b32a222b36700c030aefedbb64e49073abe6bf23df66ddbb7a0aab63bcabf5c80b234113016098b5a008a141efa90fdebbddf5032019af3b943e436fa1e0a033d5bd5618c5d08e1f5b7968d55182d21cea8441ac3a75f1",16);
			BigInteger publicExponent 	= new BigInteger("010001", 16);
		
			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, publicExponent);

			KeyFactory keyfactory 				= KeyFactory.getInstance("RSA");
			java.security.PublicKey publickey 	= keyfactory.generatePublic(pubKeySpec);

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publickey);// , fixedsecurerandom);
			
			//int len	= cipher.doFinal(aes_key).length;
			
			//System.arraycopy(cipher.doFinal(aes_key), 0, edbuf, 0, 16);
			return cipher.doFinal(aes_key);
		}catch(Exception e){
			Util.WriteLog("encrypt_rsa_2048", "ERROR=["+e.getMessage()+"]");
		}
		return (new byte[0]);
	}
	public static void storeSessionKey(byte[] enc_iv, byte[] trno){
		
		byte[]	iv		= aes_128_ecb_decrypt(metaInfo.getInstance().getKey(), enc_iv, 0, 16);
		byte[]	kbuf	= metaInfo.getInstance().getKey();	
		
		Util.WriteLog("storeSessionKey", "session key store~!");
		
		int t1 = 0, t2 = 0;
		byte[]	nonce = new byte[16];
		for(int i=0; i<7; i++){
			t1 = i*2;
			t2 = t1+1;
			nonce[i] = (byte)(iv[t1] ^ iv[t2]);
		}
		byte uc1 = 0;
		byte uc2 = 0;

		if (iv[14] != (uc1 = calculate_lrc(iv, 14)) || iv[15] != (uc2 = (byte) (kbuf[13] ^ iv[13]))) {
				Util.WriteLog("storeSessionKey", "ERROR invalid IV");
				throw new IllegalStateException("ERROR invalid IV");
		}

		nonce[7] = (byte) (iv[14] ^ iv[15]);

		//hmsg.iv = iv;
		metaInfo.getInstance().setIv(iv);
		//hmsg.trno = trno;
		metaInfo.getInstance().setTrno(trno);

		byte[] p_ctr_blocks = new byte[1024];
		for (int i = 0; i < p_ctr_blocks.length; i += 16) {
				System.arraycopy(nonce, 0, p_ctr_blocks, i, 8);

				p_ctr_blocks[i + 15] = (byte) (i & 0xff);
		}

		//hmsg.ctr_blocks = aes_128_cbc_encrypt(hmsg.getKey, hmsg.getIv, p_ctr_blocks, 0, p_ctr_blocks.length);
		byte[]	ebuf	= aes_128_cbc_encrypt(kbuf, metaInfo.getInstance().getIv(), p_ctr_blocks, 0, p_ctr_blocks.length);
		metaInfo.getInstance().setCtr_blocks(ebuf);
	}
	public static byte[] mem_rnd_to_msg(){
	
		byte[]	tmp_bytes	= new byte[16];
		Random	random		= new Random();

		//Util.WriteLog("mem_rnd_to_msg", "rnd_idx : ["+metaInfo.getInstance().getRnd_idx()+"]");
		
		String rnd_msg	= String.valueOf(1000000000+metaInfo.getInstance().getRnd_idx());
		System.arraycopy(rnd_msg.getBytes(), 0, tmp_bytes, 5, 10);
		
		tmp_bytes[5]	= '0';

		tmp_bytes[0]	= (byte) 0xff	;						// 0 - FF
		tmp_bytes[1]	= (byte) (random.nextInt(255) & 0xff);	// 1 - RN1
		tmp_bytes[2]	= (byte) (random.nextInt(255) & 0xff);	// 2 - RN2
		tmp_bytes[3]	= (byte) (random.nextInt(255) & 0xff);	// 3 - RN3
		tmp_bytes[4]	= (byte) (random.nextInt(255) & 0xff);	// 4 - RN4
	
		byte 	crc				= calculate_lrc(tmp_bytes, 15);
		byte[]	rnd_bytes		= new byte[16];

		System.arraycopy(tmp_bytes, 0, rnd_bytes, 0, 5	);
		
		rnd_bytes[5]	= crc							 ;
		
		System.arraycopy(tmp_bytes, 5, rnd_bytes, 6, 10	);
	
		return rnd_bytes;
	}
	
	public static boolean msg_to_mem_rnd(byte[] rnd_info){
		int 	xcnt		= 0;
		byte[]	tmp_bytes	= new byte[16];
		byte crc1 = 0, crc2 = 0;

		if(((byte) 0xff ^ rnd_info[0]) != 0 || rnd_info[6] != '0'){
			Util.WriteLog("msg_to_mem_rnd", "Invalid rcv enc_counter(POS)");
			return false;
		}
		
		crc1	= rnd_info[5];
		System.arraycopy(rnd_info, 0, tmp_bytes, 0, 5 );
		System.arraycopy(rnd_info, 6, tmp_bytes, 5, 10);
		
		crc2	= calculate_lrc(tmp_bytes, 15);
		if((crc1 ^ crc2) != 0){
			Util.WriteLog("msg_to_mem_rnd", "Invalid rcv enc_counter(CRC)");
			return false;
		}
		int rnd_sidx	= Integer.parseInt(new String(tmp_bytes, 5, 10));

		int cidx	= Well512.getInstance().countWELL512();
		if(cidx != metaInfo.getInstance().getRnd_idx() || (xcnt = rnd_sidx-metaInfo.getInstance().getRnd_idx()) < 0){
			Util.WriteLog("msg_to_mem_rnd", "ERROR rcv end_counter");
			Util.WriteLog("msg_to_mem_rnd", "ERROR cidx = ["+cidx+"]");
			Util.WriteLog("msg_to_mem_rnd", "ERROR metaInfo.rnd_idx = ["+metaInfo.getInstance().getRnd_idx()+"]");
			Util.WriteLog("msg_to_mem_rnd", "ERROR rnd_sidx = ["+rnd_sidx+"]");
			
			return false;
		}
		int[]	wnos	= null;
		for(int i =0; i<xcnt ;i++)
			wnos	= Well512.getInstance().getWELL512();
	
		return true;
	}
	public static byte calculate_lrc(byte[]src, int slen){
		byte c = 0x00;
		
		for(int i=0; i<slen; i++)
			c ^= src[i];
			
		return c;
	}
	public static byte[] speed_ctr_decrypt(byte[] sbuf, int slen){
		int bidx = 0, eidx = 0;
		int rno  = 0, ridx = 0;

		
		byte[] tbuf = new byte[slen];

		byte[] ctr_blocks	= metaInfo.getInstance().getCtr_blocks();
		int tlen = 0;
		for(int i=0; i<slen && tlen < slen; i+= 80){
			int[] rinfos = Well512.getInstance().getWELL512();

			rno 	= rinfos[0];
			ridx	= rinfos[1];

			for(int j=0; j<5; j++){
				eidx	= rno & 0x3f;
				rno		= rno >>> 6;

				bidx	= eidx * 16;
				for(int k=0; (k < 16 && tlen < slen); k++, tlen++){
					tbuf[tlen] = (byte) (ctr_blocks[bidx+k] ^ sbuf[tlen]);
				}
			}
		}
		//hmsg.rnd_idx	= ridx;
		
		metaInfo.getInstance().setRnd_idx(ridx);
		
		return tbuf;
	}
	public static byte[] speed_ctr_encrypt(byte[] sbuf, int slen){
		return speed_ctr_decrypt(sbuf, slen);
	}
	
	public static byte[] aes_128_ecb_decrypt(byte[] k16, byte[] pbuf, int idx, int len){
		try{
			SecretKeySpec skeySpec	= new SecretKeySpec(k16, "AES");		//AES/ECB/NoPadding

			Cipher cipher	= Cipher.getInstance("AES/ECB/NoPadding");		//AES

			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

			return cipher.doFinal(pbuf, idx, len);
		}catch(Exception e){
			Util.WriteLog("aes_128_ecb_decrypt", "ERROR:["+e.getMessage()+"]");
		}
		return (new byte[0]);
	}
	public static byte[] aes_128_ecb_encrypt(byte[] k16, byte[] pbuf, int idx, int len){
		try{
			SecretKeySpec	skeySpec	= new SecretKeySpec(k16, "AES");	// AES/ECB/NoPadding

			Cipher			cipher		= Cipher.getInstance("AES/ECB/NoPadding");	//AES
			
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		
			return cipher.doFinal(pbuf, idx, len);
		}catch(Exception e){
			Util.WriteLog("aes_128_ecb_encrypt", "ERROR=["+e.getMessage()+"]");
		}
		return (new byte[0]);
	}
	public static byte[] aes_128_ecb_encrypt(byte[] k16, byte[] iv, byte[] pbuf, int idx, int len){
		try{
			SecretKeySpec	skeySpec	= new SecretKeySpec(k16, "AES");
		
			Cipher			cipher		= Cipher.getInstance("AES/CBC/PKCS5Padding");	//AES

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv));

			return cipher.doFinal(pbuf, idx, len);
		}catch(Exception e){
			Util.WriteLog("aes_128_ecb_encrypt", "ERROR=["+e.getMessage()+"]");
		}
		return (new byte[0]);
	}
	public static byte[] aes_128_cbc_encrypt(byte[] k16, byte[] iv, byte[] pbuf, int idx, int len){
		try{
			SecretKeySpec	skeySpec	= new SecretKeySpec(k16, "AES");

			Cipher cipher	= Cipher.getInstance("AES/CBC/PKCS5Padding");	//"AES"

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv));

			return cipher.doFinal(pbuf, idx, len);
		}catch(Exception e){
			Util.WriteLog("aes_128_cbc_encrypt", "ERROR=["+e.getMessage()+"]");
		}
		return (new byte[0]);
	}
}

