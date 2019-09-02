package com.vise.bledemo.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.vise.log.ViseLog;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okio.Utf8;

public class security {
    public static String result;
    public static int i;
    public static String Decrypt;
    public static String Encrypt;
    public static Button BTN;
    byte [] KEY = new byte[16];
    byte [] IV = new byte[16];
    public void Do(){
        final String Address = "0xdaF0eE10cD837166331dAfaE6d24f1E859Bd7ED7";
        final String TEST="0a4ee7313109b655532c4ae0c8179214eafea0e8d9335a95d58eac6b2d343bebd74201f7a0db578ba8620bd21de4ead3";
        String Key = "7081cfec455576b1befbe58da496fafb";
        String iv = "4372546a795ebf2274ed23e99853b521";
        KEY = hexToBytes(Key);
        IV = hexToBytes(iv);
        final byte[] finalKEY = KEY;
        final byte[] finalIV = IV;
                try {
                    Encrypt = Encrypt(Address, finalKEY, finalIV);
                    System.out.println("Encrypt : "+Encrypt);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Decrypt = Decrypt(TEST, finalKEY, finalIV);
                    System.out.println("Decrypt : "+Decrypt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
    }
    //加密
    public static String Encrypt(String sSrc, byte[] sKey,byte[] Iv) throws Exception {
        System.out.println("String sSrc.length = " + sSrc.length());
        int i = 16-sSrc.length()%16;
        System.out.println("int i = :"+ i);
        byte[] text = sSrc.getBytes("UTF-8");
        System.out.println("byte[] text2.length : "+ text.length);
        String t="";
        for (int j =0;j<i;j++){
            t+="\b";
        }
        System.out.println("T : "+t);
        byte[] b = t.getBytes("UTF-8");
        byte[] data3 = new byte[text.length+b.length];
        System.arraycopy(text, 0, data3, 0, text.length);
        System.arraycopy(b, 0, data3, text.length, b.length);
        System.out.println("DATA3 : "+data3+" length:"+data3.length);
        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");//"算法/模式/補碼方式"
        IvParameterSpec iv = new IvParameterSpec(Iv);//使用CBC模式，需要IV，增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

        byte[] encrypted = cipher.doFinal(data3);

        return byte2Hex(encrypted);
    }
    // 解密
    public static String Decrypt(String sSrc, byte[] sKey,byte[] IV) throws Exception {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            IvParameterSpec iv = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = hexToBytes(sSrc);
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original);
                String result[] = originalString.split("\b");
                String need = result[0];
                return need;
            } catch (Exception e) {
                System.out.println("Decrypted Failed");
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println("Decrypted Failed");
            System.out.println(ex.toString());
            return null;
        }
    }
    //HexString 轉 Byte
    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData [i] = (byte) value;
        }
        return rawData ;
    }
    //Byte 轉 HexString
    public static String byte2Hex(byte[] b) {
        result = "";
        for (i=0 ; i<b.length ; i++){
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
}
