package de.wattpit.app.core;

import java.util.*;

/** Independent, measured input/output integration on the same supported time intervals. */
public final class ChargeEfficiency {
    public static final class Segment {
        public final long from,to;public final double first,last;public final String scooter;
        public Segment(long a,long b,double p,double q,String id){from=a;to=b;first=p;last=q;scooter=id;}
        double at(long t){return first+(last-first)*(t-from)/(double)(to-from);}
    }
    public static final class Result {public double inputWh,batteryWh;public long millis;public Double percent(){return millis>0&&inputWh>.01?100*batteryWh/inputWh:null;}}
    public static Result integrate(List<Segment> battery,List<Segment> input,long from,long to){
        Result result=new Result();int i=0,j=0;
        while(i<battery.size()&&j<input.size()){
            Segment b=battery.get(i),p=input.get(j);long a=Math.max(from,Math.max(b.from,p.from)),z=Math.min(to,Math.min(b.to,p.to));
            if(z>a&&b.scooter.equals(p.scooter)&&b.to>b.from&&p.to>p.from){
                double x=Math.max(0,b.at(a)),y=Math.max(0,b.at(z)),u=p.at(a),v=p.at(z);
                if(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(u)&&Double.isFinite(v)&&u>=0&&v>=0){double hours=(z-a)/3600000.0;result.batteryWh+=(x+y)*.5*hours;result.inputWh+=(u+v)*.5*hours;result.millis+=z-a;}
            }
            if(b.to<=p.to)i++;if(p.to<=b.to)j++;
        }
        return result;
    }
    private ChargeEfficiency(){}
}
