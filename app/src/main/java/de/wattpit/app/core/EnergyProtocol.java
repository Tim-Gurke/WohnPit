package de.wattpit.app.core;

import java.util.*;

/** Narrow commands established from the user's captures, without credentials or arbitrary writes. */
public final class EnergyProtocol {
    private EnergyProtocol(){}
    public static byte[] solarRead(int reg){
        if(!Arrays.asList(0xedab,0xeda8,0xeda9,0xedad,0xedbb).contains(reg))throw new IllegalArgumentException("Unbekannter Livewert.");
        return new byte[]{5,3,(byte)0x81,0x19,(byte)(reg>>8),(byte)reg};
    }
    public static byte[] output(int previousMode,boolean on){
        if(previousMode<0||previousMode>255)throw new IllegalArgumentException("Der Ausgangsmodus ist unbekannt.");
        return new byte[]{6,3,(byte)0x82,0x19,(byte)0xed,(byte)0xab,0x41,(byte)((previousMode&0xf8)|(on?4:0))};
    }
    public static Double solarValue(int reg,byte[] data){
        int size=reg==0xedab||reg==0xeda8?1:2;if(data==null||data.length!=size)return null;
        long raw=ArchiveCodec.u(data,0,size);if(raw==(size==1?255:65535))return null;
        if(reg==0xeda8&&raw>1)return null;return raw/(reg==0xedad?10.0:reg==0xeda9||reg==0xedbb?100.0:1.0);
    }
    public static byte[] chargerPoll(){return new byte[]{0x12,(byte)0xe4};}
    public static byte[] chargerStop(){return new byte[]{0x12,(byte)0xe6};}
    /** Only the actually captured 3S LiPo / 4.21 V / 0.5 A profile is supported for app start. */
    public static byte[] chargerStart(int cells,int millivolts,int milliamps){
        if(cells!=3||millivolts!=4210||milliamps!=500)throw new IllegalArgumentException("App-Start ist bisher nur für das geprüfte 3S-Profil 4,21 V / 0,50 A verfügbar. Andere Profile am Lader starten.");
        return new byte[]{0x12,(byte)0xea,1,1,1,3,0x72,0x10,(byte)0xf4,1,1};
    }
    public static final class ChargerSample {
        public final int mode,cells;public final double current,voltage;public final long elapsed,capacity;public final double[] cellVoltages;
        private ChargerSample(byte[] b){mode=b[6]&255;current=ArchiveCodec.u(b,8,2)/1000.0;voltage=ArchiveCodec.u(b,10,2)/1000.0;elapsed=ArchiveCodec.u(b,14,4);capacity=ArchiveCodec.u(b,18,4);cells=b[22]&255;cellVoltages=new double[cells];for(int i=0;i<cells;i++)cellVoltages[i]=ArchiveCodec.u(b,23+4*i,2)/1000.0;}
    }
    public static ChargerSample charger(byte[] b){
        if(b==null||b.length<23||(b[0]&255)!=0x31||(b[1]&255)!=0xe5)return null;
        int n=b[22]&255,mode=b[6]&255;if(n>6||b.length!=23+4*n||!Arrays.asList(0,1,3).contains(mode))return null;
        ChargerSample s=new ChargerSample(b);if(s.voltage>32||s.current>25)return null;for(double v:s.cellVoltages)if(v>5.5)return null;return s;
    }
}
