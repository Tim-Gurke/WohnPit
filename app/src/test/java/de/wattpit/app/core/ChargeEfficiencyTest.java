package de.wattpit.app.core;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ChargeEfficiencyTest {
 @Test public void integratesOnlySharedObservedTimes(){
  List<ChargeEfficiency.Segment> battery=Arrays.asList(new ChargeEfficiency.Segment(1000,11000,96,96,"a"),new ChargeEfficiency.Segment(21000,31000,96,96,"a"));
  List<ChargeEfficiency.Segment> input=Arrays.asList(new ChargeEfficiency.Segment(1000,31000,120,120,"a"));
  ChargeEfficiency.Result r=ChargeEfficiency.integrate(battery,input,0,40000);assertEquals(20000,r.millis);assertEquals(80,r.percent(),1e-9);assertEquals(120*20/3600.0,r.inputWh,1e-9);
 }
 @Test public void clipsBothSidesAtTheSameBoundaryAndRejectsAnotherScooter(){
  List<ChargeEfficiency.Segment> battery=Arrays.asList(new ChargeEfficiency.Segment(1000,11000,50,100,"a"));
  ChargeEfficiency.Result r=ChargeEfficiency.integrate(battery,Arrays.asList(new ChargeEfficiency.Segment(6000,16000,100,100,"a")),0,20000);assertEquals(5000,r.millis);assertEquals(87.5,r.percent(),1e-9);
  assertEquals(0,ChargeEfficiency.integrate(battery,Arrays.asList(new ChargeEfficiency.Segment(1000,11000,100,100,"b")),0,20000).millis);
 }
 @Test public void zeroInputAndAbsentIntervalsRemainUnknown(){assertNull(ChargeEfficiency.integrate(Collections.emptyList(),Collections.emptyList(),0,100).percent());}
 @Test public void validCellFramesAreDecodedWithoutInventingCells(){
  byte[] frame={(byte)0xdd,4,0,6,0x0e,0x74,0x0e,(byte)0x88,0x0e,(byte)0x92,0,0,0x77};int sum=0;for(int i=2;i<10;i++)sum+=frame[i]&255;int check=(-sum)&65535;frame[10]=(byte)(check>>8);frame[11]=(byte)check;
  assertArrayEquals(new double[]{3.7,3.72,3.73},BmsPacket.decodeCells(frame),.00001);frame[5]++;assertNull(BmsPacket.decodeCells(frame));assertNull(BmsPacket.decodeCells(Arrays.copyOf(frame,12)));
 }
}
