package com.vise.bledemo.activity;

import com.vise.log.ViseLog;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okio.Utf8;

public class security {

    public static String Encrypt(String sSrc, byte[] sKey,byte[] Iv) throws Exception {
        int i = sSrc.length()%16;
        if(i!=0){
            for(int j=0;j<16-i;j++)
                sSrc+=" ";
        }
        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//"算法/模式/補碼方式"
        IvParameterSpec iv = new IvParameterSpec(Iv);//使用CBC模式，需要一個向量iv，可增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes());

        return DeviceControlActivity.byte2Hex(encrypted);
    }

    // 解密
    public static String Decrypt(String sSrc, byte[] sKey,byte[] IV) throws Exception {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = middlePlace.hexToBytes(sSrc);
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original);
                String result[] = originalString.split(" ");
                ViseLog.i("Decrypt 0: "+result[0]);
                String need = result[0];
                return need;
            } catch (Exception e) {
                ViseLog.i("Decrypt Failed");
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }
}
