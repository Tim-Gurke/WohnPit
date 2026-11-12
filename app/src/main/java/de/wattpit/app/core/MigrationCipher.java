package de.wattpit.app.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Bounded authenticated records: large backups never require buffering an entire GCM message. */
public final class MigrationCipher {
    private static final byte[] MAGIC="WOHNPIT-MIGRATION-1\n".getBytes(StandardCharsets.US_ASCII);
    private static final int CHUNK=65536;
    private static byte[] key(char[] password,byte[] salt)throws GeneralSecurityException{
        if(password==null||password.length<10)throw new IllegalArgumentException("Das Kennwort benötigt mindestens zehn Zeichen.");
        PBEKeySpec spec=new PBEKeySpec(password,salt,210000,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}
    }
    private static byte[] record(int mode,byte[] key,byte[] prefix,int sequence,int size,byte[] data)throws GeneralSecurityException{
        if(sequence<0)throw new GeneralSecurityException("Zu viele Sicherungsabschnitte.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");byte[] iv=ByteBuffer.allocate(12).put(prefix).putInt(sequence).array();
        cipher.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));cipher.updateAAD(MAGIC);cipher.updateAAD(ByteBuffer.allocate(8).putInt(sequence).putInt(size).array());return cipher.doFinal(data);
    }
    public static OutputStream encrypt(OutputStream output,char[] password)throws IOException,GeneralSecurityException{
        byte[] salt=new byte[16],prefix=new byte[8];SecureRandom random=new SecureRandom();random.nextBytes(salt);random.nextBytes(prefix);byte[] key=key(password,salt);
        DataOutputStream out=new DataOutputStream(output);out.write(MAGIC);out.write(salt);out.write(prefix);
        return new OutputStream(){byte[] buffer=new byte[CHUNK];int count,sequence;boolean closed;
            private void emit()throws IOException{try{out.writeInt(count);out.write(record(Cipher.ENCRYPT_MODE,key,prefix,sequence++,count,Arrays.copyOf(buffer,count)));count=0;}catch(GeneralSecurityException e){throw new IOException("Sicherung konnte nicht verschlüsselt werden.",e);}}
            @Override public void write(int value)throws IOException{if(closed)throw new IOException("Sicherung ist geschlossen.");buffer[count++]=(byte)value;if(count==CHUNK)emit();}
            @Override public void write(byte[] data,int offset,int length)throws IOException{if(closed)throw new IOException("Sicherung ist geschlossen.");while(length>0){int size=Math.min(length,CHUNK-count);System.arraycopy(data,offset,buffer,count,size);count+=size;offset+=size;length-=size;if(count==CHUNK)emit();}}
            @Override public void flush()throws IOException{out.flush();}
            @Override public void close()throws IOException{if(closed)return;try{if(count>0)emit();emit();out.flush();}finally{closed=true;Arrays.fill(key,(byte)0);Arrays.fill(buffer,(byte)0);out.close();}}
        };
    }
    public static InputStream decrypt(InputStream input,char[] password)throws IOException,GeneralSecurityException{
        DataInputStream in=new DataInputStream(input);byte[] magic=new byte[MAGIC.length],salt=new byte[16],prefix=new byte[8];in.readFully(magic);
        if(!Arrays.equals(magic,MAGIC))throw new IOException("Keine unterstützte AutarPie-Übernahmesicherung.");in.readFully(salt);in.readFully(prefix);byte[] key=key(password,salt);
        return new InputStream(){byte[] buffer=new byte[0];int offset,sequence;boolean ended,closed;
            private boolean next()throws IOException{
                if(ended)return false;int size;
                try{size=in.readInt();}catch(EOFException e){throw new IOException("Die Übernahmesicherung ist unvollständig.",e);}
                if(size<0||size>CHUNK)throw new IOException("Ungültiger Sicherungsabschnitt.");byte[] encrypted=new byte[size+16];in.readFully(encrypted);
                try{buffer=record(Cipher.DECRYPT_MODE,key,prefix,sequence++,size,encrypted);offset=0;}catch(GeneralSecurityException e){throw new IOException("Kennwort falsch oder Übernahmesicherung beschädigt.",e);}
                if(size==0){ended=true;if(in.read()!=-1)throw new IOException("Unerwarteter Inhalt nach dem Sicherungsende.");return false;}return true;
            }
            @Override public int read()throws IOException{if(closed)throw new IOException("Sicherung ist geschlossen.");if(offset==buffer.length&&!next())return -1;return buffer[offset++]&255;}
            @Override public int read(byte[] data,int start,int length)throws IOException{if(length==0)return 0;if(closed)throw new IOException("Sicherung ist geschlossen.");if(offset==buffer.length&&!next())return -1;int size=Math.min(length,buffer.length-offset);System.arraycopy(buffer,offset,data,start,size);offset+=size;return size;}
            @Override public void close()throws IOException{if(closed)return;closed=true;Arrays.fill(key,(byte)0);Arrays.fill(buffer,(byte)0);in.close();}
        };
    }
    private MigrationCipher(){}
}
