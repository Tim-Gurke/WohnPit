package de.wattpit.app.core;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Extracts only the private AF02 identity from an explicitly selected local Android capture. */
public final class ChargerCapture {
    private ChargerCapture(){}
    public static byte[] read(InputStream source)throws IOException{
        BufferedInputStream input=new BufferedInputStream(source);input.mark(16);byte[] magic=bytes(input,8);input.reset();
        if(magic.length>=2&&magic[0]=='P'&&magic[1]=='K'){try(ZipInputStream zip=new ZipInputStream(input)){ZipEntry e;while((e=zip.getNextEntry())!=null)if(!e.isDirectory()&&e.getName().endsWith("/btsnoop_hci.log"))return snoop(zip);}throw new IOException("Kein Bluetooth-HCI-Protokoll im Fehlerbericht.");}
        return snoop(input);
    }
    private static byte[] bytes(InputStream input,int count)throws IOException{byte[] b=new byte[count];int at=0;while(at<count){int n=input.read(b,at,count-at);if(n<0)break;if(n==0)continue;at+=n;}return at==count?b:Arrays.copyOf(b,at);}
    private static byte[] snoop(InputStream input)throws IOException{
        DataInputStream in=new DataInputStream(input);byte[] header=bytes(in,16);if(header.length!=16||!new String(header,0,8,java.nio.charset.StandardCharsets.US_ASCII).equals("btsnoop\0"))throw new IOException("Keine unterstützte Bluetooth-Aufzeichnung.");
        Map<Integer,Integer> authHandles=new HashMap<>();Map<String,ByteArrayOutputStream> fragments=new HashMap<>();long total=0;byte[] result=null;
        while(true){int original;try{original=in.readInt();}catch(EOFException end){break;}int count=in.readInt(),flags=in.readInt();in.readInt();in.readLong();if(count<0||count>65540||original<count||(total+=count)>512L*1024*1024)throw new IOException("Ungültige oder zu große Bluetooth-Aufzeichnung.");byte[] hci=bytes(in,count);if(hci.length!=count)throw new EOFException();if(count<5||hci[0]!=2)continue;
            int raw=(hci[1]&255)|((hci[2]&255)<<8),handle=raw&0xfff,pb=(raw>>12)&3;String key=handle+":"+(flags&1);ByteArrayOutputStream frame;
            if(pb==2||pb==0){frame=new ByteArrayOutputStream();fragments.put(key,frame);}else{frame=fragments.get(key);if(frame==null)continue;}frame.write(hci,5,count-5);byte[] l2=frame.toByteArray();if(l2.length<4)continue;int length=(l2[0]&255)|((l2[1]&255)<<8),cid=(l2[2]&255)|((l2[3]&255)<<8);if(length>65535)throw new IOException();if(l2.length<length+4)continue;fragments.remove(key);if(cid!=4||length<1)continue;byte[] att=Arrays.copyOfRange(l2,4,4+length);int op=att[0]&255;
            if((flags&1)==1&&op==9&&att.length>=2){int stride=att[1]&255;if(stride==7)for(int p=2;p+7<=att.length;p+=7){int uuid=(att[p+5]&255)|((att[p+6]&255)<<8);if(uuid==0xaf02)authHandles.put(handle,(att[p+3]&255)|((att[p+4]&255)<<8));}}
            if((flags&1)==0&&(op==0x52||op==0x12)&&att.length==22){int characteristic=(att[1]&255)|((att[2]&255)<<8);if(authHandles.getOrDefault(handle,-1)==characteristic&&(att[3]&255)==0x18){byte[] next=Arrays.copyOfRange(att,4,20);if(result!=null&&!Arrays.equals(result,next))throw new IOException("Mehrere unterschiedliche 608PD-Verbindungen enthalten. Bitte eine eindeutige Aufzeichnung verwenden.");result=next;}}
        }
        if(result==null)throw new IOException("Keine vollständige 608PD-Anmeldung gefunden.");return result;
    }
}
