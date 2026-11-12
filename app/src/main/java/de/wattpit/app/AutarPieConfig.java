package de.wattpit.app;

import org.json.*;
import java.util.*;

/** Typed public appearance/layout configuration; no addresses, device keys or command strings. */
final class AutarPieConfig {
    static final String WORKSPACE="autarpie-workspace-v1",APPEARANCE="autarpie-appearance-v1";
    private static boolean color(String s){return s.matches("#[0-9a-fA-F]{6}");}
    static JSONObject surface(JSONObject input)throws Exception{
        if(input==null)throw new IllegalArgumentException("Hintergrund fehlt.");String mode=input.optString("mode"),first=input.optString("first"),second=input.optString("second");double angle=input.optDouble("angle",Double.NaN);
        if(!Arrays.asList("solid","gradient").contains(mode)||!color(first)||!color(second)||!Double.isFinite(angle)||angle<0||angle>360)throw new IllegalArgumentException("Bitte Farben und einen Winkel zwischen 0 und 360° wählen.");
        return new JSONObject().put("mode",mode).put("first",first.toLowerCase(Locale.ROOT)).put("second",second.toLowerCase(Locale.ROOT)).put("angle",angle);
    }
    static JSONObject appearance(String json)throws Exception{
        if(json==null||json.length()>30000)throw new IllegalArgumentException("Die Farbvorgabe ist zu groß.");JSONObject in=new JSONObject(json),colors=in.getJSONObject("colors"),out=new JSONObject(),clean=new JSONObject();
        String theme=in.optString("theme","system"),accent=in.optString("accent");if(!Arrays.asList("system","light","dark").contains(theme)||!color(accent))throw new IllegalArgumentException("Ungültige Darstellung.");
        for(String id:new String[]{"gas","water","solar","calculated","model","scooter","storage","system"}){String value=colors.getString(id);if(!color(value))throw new IllegalArgumentException("Ungültige Bereichsfarbe.");clean.put(id,value.toLowerCase(Locale.ROOT));}
        out.put("version",1).put("theme",theme).put("accent",accent).put("colors",clean).put("app",surface(in.getJSONObject("app"))).put("tile",surface(in.getJSONObject("tile"))).put("customSurfaces",in.optBoolean("customSurfaces"));
        JSONObject scales=in.getJSONObject("scales");double low=scales.getDouble("low"),high=scales.getDouble("high"),width=scales.getDouble("width");int count=scales.getInt("count");JSONArray given=scales.getJSONArray("colors"),range=new JSONArray();
        if((count!=2&&count!=3)||!Double.isFinite(low)||!Double.isFinite(high)||!Double.isFinite(width)||low<1||high>99||high<=low||width<1||width>100||given.length()!=3)throw new IllegalArgumentException("Ungültige Skalengrenzen.");for(int i=0;i<3;i++){if(!color(given.getString(i)))throw new IllegalArgumentException("Ungültige Skalenfarbe.");range.put(given.getString(i));}
        JSONObject overrides=in.optJSONObject("metricColors"),checked=new JSONObject();if(overrides!=null){if(overrides.length()>128)throw new IllegalArgumentException("Zu viele eigene Farben.");for(Iterator<String> k=overrides.keys();k.hasNext();){String id=k.next(),v=overrides.getString(id);if(!id.matches("[a-zA-Z0-9_-]{1,80}")||!color(v))throw new IllegalArgumentException("Ungültige eigene Farbe.");checked.put(id,v);}}
        return out.put("metricColors",checked).put("scales",new JSONObject().put("count",count).put("low",low).put("high",high).put("width",width).put("colors",range));
    }
    static JSONObject workspace(String json)throws Exception{
        if(json==null||json.length()>1500000)throw new IllegalArgumentException("Die Dashboards sind zu groß.");JSONObject in=new JSONObject(json);JSONArray pages=in.getJSONArray("pages"),clean=new JSONArray(),navigation=in.getJSONArray("navigation");Set<String> ids=new HashSet<>();
        if(pages.length()>40||navigation.length()>40)throw new IllegalArgumentException("Zu viele Dashboards.");
        for(int i=0;i<pages.length();i++){JSONObject p=pages.getJSONObject(i);String id=p.getString("id"),name=p.getString("name");if(!id.matches("[a-zA-Z0-9_-]{1,80}")||!ids.add(id)||name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Ungültiges Dashboard.");JSONObject row=new JSONObject().put("id",id).put("name",name).put("builtin",Arrays.asList("overview","storage","solar","supplies","model","history","devices","settings").contains(id));JSONObject board=p.optJSONObject("board");if(board!=null){if(board.optInt("version")!=1||board.getJSONArray("tiles").length()>64||board.toString().length()>128000)throw new IllegalArgumentException("Ungültige Kacheln.");row.put("board",ConfigurationBackup.dashboard(board));}clean.put(row);}
        if(!ids.containsAll(Arrays.asList("overview","storage","solar","supplies","model","history","devices","settings")))throw new IllegalArgumentException("Ein Standardbereich fehlt.");
        Set<String> pins=new HashSet<>();for(int i=0;i<navigation.length();i++)if(!ids.contains(navigation.getString(i))||!pins.add(navigation.getString(i)))throw new IllegalArgumentException("Ungültige Kurzwahl.");return new JSONObject().put("version",1).put("pages",clean).put("navigation",navigation);
    }
    private AutarPieConfig(){}
}
