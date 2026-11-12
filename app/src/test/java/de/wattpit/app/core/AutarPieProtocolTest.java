package de.wattpit.app.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AutarPieProtocolTest {
    @Test public void onlyOutputModeBitsAreChanged(){byte[] on=EnergyProtocol.output(0xa1,true),off=EnergyProtocol.output(0xa4,false);assertEquals(0xa4,on[7]&255);assertEquals(0xa0,off[7]&255);assertArrayEquals(new byte[]{5,3,(byte)0x81,0x19,(byte)0xed,(byte)0xbb},EnergyProtocol.solarRead(0xedbb));}
    @Test public void unknownRegistersAndUnavailableReadingsAreRejected(){try{EnergyProtocol.solarRead(0x1000);fail();}catch(IllegalArgumentException expected){}assertNull(EnergyProtocol.solarValue(0xedad,new byte[]{-1,-1}));assertNull(EnergyProtocol.solarValue(0xeda8,new byte[]{2}));assertEquals(12.87,EnergyProtocol.solarValue(0xeda9,new byte[]{7,5}),.001);}
    @Test public void chargerStatusUsesCellCountAndUnsignedDirection(){byte[] b=new byte[35];b[0]=0x31;b[1]=(byte)0xe5;b[6]=3;b[8]=(byte)0xf8;b[9]=4;b[10]=(byte)0x80;b[11]=0x2c;b[14]=0x35;b[15]=3;b[18]=0x1b;b[19]=1;b[22]=3;for(int i=0;i<3;i++){b[23+4*i]=(byte)0xeb;b[24+4*i]=0x0e;}EnergyProtocol.ChargerSample sample=EnergyProtocol.charger(b);assertNotNull(sample);assertEquals(3,sample.mode);assertEquals(1.272,sample.current,.001);assertEquals(11.392,sample.voltage,.001);assertEquals(821,sample.elapsed);assertEquals(283,sample.capacity);assertEquals(3,sample.cells);assertNull(EnergyProtocol.charger(new byte[24]));b[22]=6;assertNull(EnergyProtocol.charger(b));}
    @Test public void unknownChargeProfilesCannotBeReplayed(){assertEquals(11,EnergyProtocol.chargerStart(3,4210,500).length);try{EnergyProtocol.chargerStart(4,4200,1000);fail();}catch(IllegalArgumentException expected){}assertArrayEquals(new byte[]{0x12,(byte)0xe6},EnergyProtocol.chargerStop());}
    @Test public void autostartRespectsPauseRoleAndUnlock(){assertTrue(StartupPolicy.restoreCollector(true,true,"central",true));assertTrue(StartupPolicy.restoreNetwork(true,"central",true));assertFalse(StartupPolicy.restoreCollector(true,false,"central",true));assertFalse(StartupPolicy.restoreCollector(true,true,"viewer",true));assertFalse(StartupPolicy.restoreCollector(true,true,"direct",false));assertFalse(StartupPolicy.restoreCollector(false,true,"central",true));}
}
