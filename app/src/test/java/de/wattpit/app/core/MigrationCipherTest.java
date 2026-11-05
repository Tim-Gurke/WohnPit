package de.wattpit.app.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.Arrays;

public final class MigrationCipherTest {
    private static final char[] PASSWORD="WohnPit-Test-10-Zeichen".toCharArray();
    private static byte[] encrypt(byte[] data)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(OutputStream encrypted=MigrationCipher.encrypt(out,PASSWORD)){encrypted.write(data);}return out.toByteArray();}
    private static byte[] decrypt(byte[] data,char[] password)throws Exception{try(InputStream in=MigrationCipher.decrypt(new ByteArrayInputStream(data),password);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}}
    @Test public void largeMultiRecordBackupRoundTripsAndUsesFreshSalt()throws Exception{byte[] data=new byte[350123];for(int i=0;i<data.length;i++)data[i]=(byte)(i*37);byte[] first=encrypt(data);assertArrayEquals(data,decrypt(first,PASSWORD));assertFalse(Arrays.equals(first,encrypt(data)));}
    @Test public void wrongPasswordNeverReturnsAValidStream()throws Exception{byte[] encrypted=encrypt(new byte[200000]);try{decrypt(encrypted,"anderes-sicheres-Kennwort".toCharArray());fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Kennwort"));}}
    @Test public void changedRecordAndTruncatedEndAreRejected()throws Exception{byte[] encrypted=encrypt(new byte[200000]);for(int where:new int[]{100,70000,encrypted.length-1}){byte[] changed=encrypted.clone();changed[where]^=1;try{decrypt(changed,PASSWORD);fail();}catch(IOException expected){}}try{decrypt(Arrays.copyOf(encrypted,encrypted.length-20),PASSWORD);fail();}catch(IOException expected){}}
    @Test public void appendedDataAndInvalidRecordLengthAreRejected()throws Exception{byte[] encrypted=encrypt(new byte[100]);byte[] appended=Arrays.copyOf(encrypted,encrypted.length+1);try{decrypt(appended,PASSWORD);fail();}catch(IOException expected){}encrypted[44]=(byte)0x7f;try{decrypt(encrypted,PASSWORD);fail();}catch(IOException expected){}}
    @Test public void emptyStreamIsAuthenticatedAndShortPasswordsAreRejected()throws Exception{assertArrayEquals(new byte[0],decrypt(encrypt(new byte[0]),PASSWORD));try{MigrationCipher.encrypt(new ByteArrayOutputStream(),"kurz".toCharArray());fail();}catch(IllegalArgumentException expected){}}
}
