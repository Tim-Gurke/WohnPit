package de.wattpit.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Finite observed charger-input estimates, separate from measured plant energy. */
final class RollerLedger extends SQLiteOpenHelper {
    private static RollerLedger instance;
    static synchronized RollerLedger get(Context c){if(instance==null)instance=new RollerLedger(c.getApplicationContext());return instance;}
    private long from,lastTo,session,lastPrune,lastLoadPrune;
    private double wh;
    private String source="",mac="";
    private RollerLedger(Context c){this(c,"wattpit-roller-input.db");}
    RollerLedger(Context c,String name){super(c,name,null,3);setWriteAheadLoggingEnabled(true);}
    synchronized void replace(String plant,long from,long to,JSONArray rows)throws Exception{
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            db.delete("input_intervals","source=? AND end>? AND start<?",new String[]{plant,""+from,""+to});
            for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);long a=r.getLong("from"),b=r.getLong("to");double value=r.getDouble("wh");if(b<=a||!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Ungültiger Rollerabschnitt.");ContentValues v=new ContentValues();v.put("source",plant);v.put("start",a);v.put("end",b);v.put("wh",value);v.put("mac","");v.put("session",0);db.insertWithOnConflict("input_intervals",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE input_intervals(source TEXT NOT NULL,start INTEGER NOT NULL,end INTEGER NOT NULL,wh REAL NOT NULL,mac TEXT NOT NULL,session INTEGER NOT NULL,PRIMARY KEY(source,start))");db.execSQL("CREATE INDEX input_end ON input_intervals(source,end)");rawSchema(db);}
    private static void rawSchema(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS roller_raw(source TEXT NOT NULL,start INTEGER NOT NULL,end INTEGER NOT NULL,payload TEXT NOT NULL,mac TEXT NOT NULL,PRIMARY KEY(source,start,mac))");loadSchema(db);}
    private static void loadSchema(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS load_raw(source TEXT NOT NULL,start INTEGER NOT NULL,end INTEGER NOT NULL,payload TEXT NOT NULL,mac TEXT NOT NULL,PRIMARY KEY(source,start,mac))");db.execSQL("CREATE INDEX IF NOT EXISTS load_end ON load_raw(source,end)");}
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next){if(old<2)rawSchema(db);if(old<3)loadSchema(db);}
    synchronized void loadRaw(String plant,String regulator,JSONObject sample,boolean exclusive,String scooter)throws Exception{
        long at=sample.optLong("at"),bucket=at/60000*60000;if(at<=0||!sample.has("current")||!sample.has("voltage")||!sample.has("state"))return;
        JSONObject point=new JSONObject().put("at",at).put("voltage",sample.getDouble("voltage")).put("current",sample.getDouble("current")).put("state",sample.getInt("state")).put("mode",sample.has("mode")?sample.get("mode"):JSONObject.NULL).put("exclusive",exclusive).put("scooter",scooter);
        SQLiteDatabase db=getWritableDatabase();JSONArray values=new JSONArray();try(Cursor c=db.rawQuery("SELECT payload FROM load_raw WHERE source=? AND start=? AND mac=?",new String[]{plant,""+bucket,regulator})){if(c.moveToFirst())values=new JSONArray(c.getString(0));}
        if(values.length()>=128)return;values.put(point);ContentValues row=new ContentValues();row.put("source",plant);row.put("start",bucket);row.put("end",at);row.put("payload",values.toString());row.put("mac",regulator);db.insertWithOnConflict("load_raw",null,row,SQLiteDatabase.CONFLICT_REPLACE);
        if(at-lastLoadPrune>3600000){db.delete("load_raw","end<?",new String[]{""+LearningWindow.since(at)});lastLoadPrune=at;}
    }
    synchronized void rawAdd(String plant,String mac,long session,long from,long to,double first,double last,double idle,double efficiency)throws JSONException{rawAdd(plant,mac,session,from,to,first,last,idle,efficiency,null,null);}
    synchronized void rawAdd(String plant,String mac,long session,long from,long to,double first,double last,double idle,double efficiency,JSONObject before,JSONObject after)throws JSONException{
        if(from<=0||to<=from||to-from>10000)return;long bucket=from/60000*60000;SQLiteDatabase db=getWritableDatabase();JSONArray values=new JSONArray();try(Cursor c=db.rawQuery("SELECT payload FROM roller_raw WHERE source=? AND start=? AND mac=?",new String[]{plant,""+bucket,mac})){if(c.moveToFirst())values=new JSONArray(c.getString(0));}
        if(values.length()>=128)return;JSONArray sample=new JSONArray().put(from).put(to).put(first).put(last).put(idle).put(efficiency).put(session);if(before!=null&&after!=null){sample.put(sampleMetadata(before));sample.put(sampleMetadata(after));}values.put(sample);ContentValues row=new ContentValues();row.put("source",plant);row.put("start",bucket);row.put("end",to);row.put("payload",values.toString());row.put("mac",mac);db.insertWithOnConflict("roller_raw",null,row,SQLiteDatabase.CONFLICT_REPLACE);
        if(to-lastPrune>3600000){db.delete("roller_raw","end<?",new String[]{""+LearningWindow.since(to)});lastPrune=to;}
    }
    private static JSONObject sampleMetadata(JSONObject value)throws JSONException{JSONObject out=new JSONObject();for(String k:new String[]{"at","voltage","current","soc"})if(value.has(k))out.put(k,value.get(k));return out;}
    synchronized void derive(long from,long to)throws JSONException{
        try(Cursor c=getReadableDatabase().rawQuery("SELECT source,mac,payload FROM roller_raw WHERE end>? AND start<? ORDER BY start",new String[]{""+from,""+to})){while(c.moveToNext()){JSONArray values=new JSONArray(c.getString(2));for(int i=0;i<values.length();i++){JSONArray v=values.getJSONArray(i);long a=v.getLong(0),b=v.getLong(1);if(b<=from||a>=to)continue;double p=v.getDouble(2),q=v.getDouble(3),idle=v.getDouble(4),eff=v.getDouble(5);double input=(de.wattpit.app.core.RollerEnergy.inputWatts(Math.max(0,p),idle,eff)+de.wattpit.app.core.RollerEnergy.inputWatts(Math.max(0,q),idle,eff))*.5*(b-a)/3600000.0;add(c.getString(0),c.getString(1),v.getLong(6),a,b,input);}}}
    }
    synchronized JSONObject loadHistory(String plant,long from,long to,String field,String device)throws JSONException{
        if(!java.util.Arrays.asList("voltage","current","power","state","mode").contains(field)||from<0||to<=from)throw new IllegalArgumentException("Unbekannter Lastausgangswert.");
        JSONArray points=new JSONArray();String selected=device==null?"":device;long previous=0;double total=0,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY,last=0;int count=0;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT mac,payload FROM load_raw WHERE source=? AND end>=? AND start<=? ORDER BY start",new String[]{plant,""+from,""+to})){
            while(c.moveToNext()){String mac=c.getString(0);if(selected.isEmpty())selected=mac;if(!selected.equals(mac))continue;JSONArray rows=new JSONArray(c.getString(1));for(int i=0;i<rows.length();i++){JSONObject p=rows.getJSONObject(i);long at=p.optLong("at");if(at<from||at>to)continue;double v=field.equals("power")?p.optDouble("voltage",Double.NaN)*p.optDouble("current",Double.NaN):p.optDouble(field,Double.NaN);if(!Double.isFinite(v))continue;points.put(new JSONObject().put("t",at).put("last",at).put("device",mac).put("kind","output").put("origin","live").put("v",v).put("min",v).put("max",v).put("n",1).put("period",15).put("gap",previous==0||at-previous>45000));previous=at;total+=v;min=Math.min(min,v);max=Math.max(max,v);last=v;count++;}}
        }
        JSONObject summary=new JSONObject();if(count>0)summary.put("avg",total/count).put("last",last).put("min",min).put("max",max).put("count",count);
        return new JSONObject().put("points",points).put("summary",summary);
    }
    synchronized JSONObject summary(String plant,long from,long to)throws JSONException{
        double battery=0,input=0;long observed=0;JSONArray samples=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM roller_raw WHERE source=? AND end>? AND start<? ORDER BY start",new String[]{plant,""+from,""+to})){while(c.moveToNext()){JSONArray rows=new JSONArray(c.getString(0));for(int i=0;i<rows.length();i++){JSONArray v=rows.getJSONArray(i);if(v.length()>8){JSONObject sample=new JSONObject(v.getJSONObject(8).toString());sample.put("power",v.getDouble(3)).put("gap",v.getLong(1)-v.getLong(0)>10000);samples.put(sample);}long a=Math.max(from,v.getLong(0)),b=Math.min(to,v.getLong(1));if(b<=a)continue;double p=Math.max(0,v.getDouble(2)),q=Math.max(0,v.getDouble(3)),idle=v.getDouble(4),eff=v.getDouble(5);battery+=(p+q)*.5*(b-a)/3600000.0;input+=(de.wattpit.app.core.RollerEnergy.inputWatts(p,idle,eff)+de.wattpit.app.core.RollerEnergy.inputWatts(q,idle,eff))*.5*(b-a)/3600000.0;observed+=b-a;}}}
        JSONObject result=new JSONObject().put("batteryWh",observed>0?battery:JSONObject.NULL).put("inputWh",observed>0?input:JSONObject.NULL).put("observedMillis",observed).put("gapMillis",Math.max(0,to-from-observed));
        java.util.List<de.wattpit.app.core.ChargeEfficiency.Segment> bms=new java.util.ArrayList<>(),loads=new java.util.ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT mac,payload FROM roller_raw WHERE source=? AND end>? AND start<? ORDER BY start",new String[]{plant,""+from,""+to})){while(c.moveToNext()){JSONArray rows=new JSONArray(c.getString(1));for(int i=0;i<rows.length();i++){JSONArray v=rows.getJSONArray(i);bms.add(new de.wattpit.app.core.ChargeEfficiency.Segment(v.getLong(0),v.getLong(1),v.getDouble(2),v.getDouble(3),c.getString(0)));}}}
        java.util.Map<String,JSONObject> previous=new java.util.HashMap<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT mac,payload FROM load_raw WHERE source=? AND end>? AND start<? ORDER BY start",new String[]{plant,""+Math.max(0,from-45000),""+(to+45000)})){while(c.moveToNext()){JSONArray rows=new JSONArray(c.getString(1));String regulator=c.getString(0);for(int i=0;i<rows.length();i++){JSONObject q=rows.getJSONObject(i),p=previous.put(regulator,q);if(p==null)continue;long a=p.optLong("at"),z=q.optLong("at");if(z<=a||z-a>45000||!p.optBoolean("exclusive")||!q.optBoolean("exclusive")||!p.optString("scooter").equals(q.optString("scooter")))continue;double u=p.getDouble("voltage")*p.getDouble("current"),v=q.getDouble("voltage")*q.getDouble("current");loads.add(new de.wattpit.app.core.ChargeEfficiency.Segment(a,z,u,v,q.getString("scooter")));}}}
        bms.sort(java.util.Comparator.comparingLong(x->x.from));loads.sort(java.util.Comparator.comparingLong(x->x.from));
        de.wattpit.app.core.ChargeEfficiency.Result paired=de.wattpit.app.core.ChargeEfficiency.integrate(bms,loads,from,to);Double percent=paired.percent();
        JSONArray compact=new JSONArray();int step=Math.max(1,(int)Math.ceil(samples.length()/300.0));for(int i=0;i<samples.length();i+=step){int low=i,high=i,last=Math.min(samples.length()-1,i+step-1);for(int j=i;j<=last;j++){if(samples.getJSONObject(j).optDouble("power")<samples.getJSONObject(low).optDouble("power"))low=j;if(samples.getJSONObject(j).optDouble("power")>samples.getJSONObject(high).optDouble("power"))high=j;}java.util.TreeSet<Integer> indices=new java.util.TreeSet<>();indices.add(i);indices.add(low);indices.add(high);indices.add(last);for(int index:indices)compact.put(samples.getJSONObject(index));}
        return result.put("samples",compact).put("pairedMillis",paired.millis).put("measuredInputWh",paired.millis>0?paired.inputWh:JSONObject.NULL).put("pairedBatteryWh",paired.millis>0?paired.batteryWh:JSONObject.NULL).put("measuredEfficiency",percent==null?JSONObject.NULL:percent).put("measuredLossWh",paired.millis>0?paired.inputWh-paired.batteryWh:JSONObject.NULL);
    }
    static String source(SharedPreferences prefs){try{
        String[] selected={"",""};JSONArray devices=new JSONArray(prefs.getString("devices","[]"));
        for(int i=0;i<devices.length();i++){JSONObject d=devices.getJSONObject(i);if(d.optString("kind").equals("solar"))selected[0]=d.optString("mac");if(d.optString("kind").equals("shunt"))selected[1]=d.optString("mac");}
        StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(String.join("|",selected).getBytes(StandardCharsets.UTF_8)))result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();
    }catch(Exception e){return "";}}
    synchronized void add(String plant,String address,long started,long a,long b,double value){
        if(plant.isEmpty()||b<=a||b-a>10000||!Double.isFinite(value)||value<0)return;
        if(!plant.equals(source)||!address.equals(mac)||started!=session||lastTo!=a||b-from>60000){from=a;wh=0;}
        source=plant;mac=address;session=started;lastTo=b;wh+=value;
        ContentValues row=new ContentValues();row.put("source",plant);row.put("start",from);row.put("end",b);row.put("wh",wh);row.put("mac",address);row.put("session",started);
        SQLiteDatabase db=getWritableDatabase();db.insertWithOnConflict("input_intervals",null,row,SQLiteDatabase.CONFLICT_REPLACE);
        if(b-lastPrune>3600000){db.delete("input_intervals","end<?",new String[]{""+(LearningWindow.since(b))});lastPrune=b;}
    }
    synchronized JSONArray read(String plant,long a,long b)throws JSONException{
        JSONArray out=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT start,end,wh FROM input_intervals WHERE source=? AND end>? AND start<? ORDER BY start",new String[]{plant,""+a,""+b})){
            while(c.moveToNext())out.put(new JSONObject().put("from",c.getLong(0)).put("to",c.getLong(1)).put("wh",c.getDouble(2)));
        }return out;
    }
    synchronized long[] range(String plant){try(Cursor c=getReadableDatabase().rawQuery("SELECT MIN(start),MAX(end) FROM input_intervals WHERE source=?",new String[]{plant})){if(c.moveToFirst()&&!c.isNull(0))return new long[]{c.getLong(0),c.getLong(1)};}return new long[]{0,0};}
    static Double estimate(JSONArray rows,long a,long b)throws JSONException{
        long cursor=a;double value=0;
        for(int i=0;i<rows.length()&&cursor<b;i++){JSONObject row=rows.getJSONObject(i);long from=row.getLong("from"),to=row.getLong("to");if(to<=cursor)continue;if(from>cursor)return null;long end=Math.min(b,to);if(end<=cursor||to<=from)return null;
            double wh=row.optDouble("wh",Double.NaN);if(!Double.isFinite(wh)||wh<0)return null;value+=wh*(end-cursor)/(to-from);cursor=end;
        }return cursor==b?value:null;
    }
}
