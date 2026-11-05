package de.wattpit.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.wattpit.app.core.MigrationCipher;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Full encrypted takeover, including private pairing/device credentials. Never a public export. */
final class MigrationBackup {
    static final long MAX_BYTES=2L*1024*1024*1024;
    private static final String STAGE="wohnpit-takeover",PENDING="wohnpit-takeover.pending";
    private static volatile boolean stagedHere;
    static boolean safeEntry(String name){return name.equals("preferences.json")||name.equals("manifest.json")||name.equals("files/lan-display.json")||name.matches("databases/[A-Za-z0-9_-][A-Za-z0-9_.-]{0,100}")&&!name.contains("..");}
    private static File stage(Context c){return new File(c.getFilesDir(),STAGE);}
    private static File marker(Context c){return new File(c.getFilesDir(),PENDING);}
    static boolean pending(Context c){return marker(c).isFile();}
    private static JSONObject preferences(Context c)throws Exception{
        JSONObject result=new JSONObject();File[] files=new File(c.getApplicationInfo().dataDir,"shared_prefs").listFiles();
        if(files!=null)for(File file:files){String name=file.getName();if(!name.endsWith(".xml"))continue;name=name.substring(0,name.length()-4);JSONObject values=new JSONObject();
            for(Map.Entry<String,?> e:c.getSharedPreferences(name,0).getAll().entrySet()){
                Object value=e.getValue();String type=value instanceof String?"string":value instanceof Integer?"int":value instanceof Long?"long":value instanceof Float?"float":value instanceof Boolean?"boolean":value instanceof Set?"set":"";
                if(type.isEmpty())throw new IOException("Nicht unterstützter Einstellungswert.");
                values.put(e.getKey(),new JSONObject().put("type",type).put("value",value instanceof Set?new JSONArray((Set<?>)value):value));
            }
            result.put(name,values);
        }return result;
    }
    private static void restorePreferences(Context c,JSONObject snapshot)throws Exception{
        for(Iterator<String> names=snapshot.keys();names.hasNext();){String name=names.next();if(!name.matches("[A-Za-z0-9_.-]{1,120}")||name.contains(".."))throw new IOException("Ungültiger Einstellungsname.");
            SharedPreferences.Editor edit=c.getSharedPreferences(name,0).edit().clear();JSONObject values=snapshot.getJSONObject(name);
            for(Iterator<String> keys=values.keys();keys.hasNext();){String key=keys.next();JSONObject row=values.getJSONObject(key);switch(row.getString("type")){
                case "string":edit.putString(key,row.getString("value"));break;case "int":edit.putInt(key,row.getInt("value"));break;case "long":edit.putLong(key,row.getLong("value"));break;case "float":edit.putFloat(key,(float)row.getDouble("value"));break;case "boolean":edit.putBoolean(key,row.getBoolean("value"));break;
                case "set":JSONArray list=row.getJSONArray("value");Set<String> set=new HashSet<>();for(int i=0;i<list.length();i++)set.add(list.getString(i));edit.putStringSet(key,set);break;
                default:throw new IOException("Ungültiger Einstellungstyp.");
            }}if(!edit.commit())throw new IOException("Einstellungen konnten nicht gespeichert werden.");
        }
    }
    private static void put(ZipOutputStream zip,String name,byte[] data)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();}
    private static void put(ZipOutputStream zip,String name,File file)throws Exception{zip.putNextEntry(new ZipEntry(name));try(InputStream in=new FileInputStream(file)){copy(in,zip,MAX_BYTES);}zip.closeEntry();}
    private static long copy(InputStream in,OutputStream out,long limit)throws IOException{byte[] buffer=new byte[32768];long total=0;int n;while((n=in.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("Übernahmesicherung ist zu groß.");out.write(buffer,0,n);}return total;}
    static void export(Context c,OutputStream destination,char[] password)throws Exception{
        Repository repo=Repository.get(c);if(repo.running||repo.archiveBusy||repo.prefs.getBoolean("recording",false))throw new IOException("Bitte zuerst die Aufzeichnung pausieren und den Archivabruf beenden.");
        if(SuppliesWeather.busy())throw new IOException("Bitte den laufenden Wetterabruf abwarten.");
        c.stopService(new Intent(c,LanService.class));LanSync.get(c).stopHost();
        JSONObject prefs=preferences(c),manifest=new JSONObject().put("format","wohnpit-takeover").put("version",1).put("sourcePackage",c.getPackageName()).put("sourceVersion",BuildConfig.VERSION_NAME).put("createdAt",System.currentTimeMillis());
        SQLiteDatabase db=repo.db.getWritableDatabase();db.beginTransaction();
        try(ZipOutputStream zip=new ZipOutputStream(MigrationCipher.encrypt(destination,password))){
            put(zip,"manifest.json",manifest.toString().getBytes(StandardCharsets.UTF_8));put(zip,"preferences.json",prefs.toString().getBytes(StandardCharsets.UTF_8));
            File folder=c.getDatabasePath("wattpit-history.db").getParentFile();File[] databases=folder.listFiles();
            if(databases!=null)for(File file:databases)if(file.isFile()&&safeEntry("databases/"+file.getName())&&!file.getName().endsWith("-shm")&&!file.getName().endsWith("-journal"))put(zip,"databases/"+file.getName(),file);
            File cache=new File(c.getFilesDir(),"lan-display.json");if(cache.isFile())put(zip,"files/lan-display.json",cache);
        }finally{db.endTransaction();}
    }
    static JSONObject prepare(Context c,InputStream source,char[] password)throws Exception{
        if(pending(c))throw new IOException("Eine bereits geprüfte Übernahme wartet auf den Neustart.");
        File temp=new File(c.getFilesDir(),STAGE+"-checking");remove(temp);if(!temp.mkdirs())throw new IOException("Kein Speicherplatz für die Prüfung.");
        try(InputStream plain=MigrationCipher.decrypt(source,password)){
            ZipInputStream zip=new ZipInputStream(plain);Set<String> seen=new HashSet<>();ZipEntry entry;long total=0;
            while((entry=zip.getNextEntry())!=null){String name=entry.getName();if(entry.isDirectory()||!safeEntry(name)||!seen.add(name)||seen.size()>256)throw new IOException("Ungültiger Inhalt der Übernahmesicherung.");
                File out=new File(temp,name);File parent=out.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Kein Speicherplatz für die Prüfung.");
                try(OutputStream data=new FileOutputStream(out)){total+=copy(zip,data,MAX_BYTES-total);}zip.closeEntry();
            }
            // Consume and authenticate the AES-GCM tag before touching any current data.
            while(plain.read()!=-1){}
            JSONObject manifest=json(new File(temp,"manifest.json")),prefs=json(new File(temp,"preferences.json"));
            if(!manifest.optString("format").equals("wohnpit-takeover")||manifest.optInt("version")!=1||!Arrays.asList("de.wohnpit.app","de.wattpit.app").contains(manifest.optString("sourcePackage"))||!prefs.has("wattpit"))throw new IOException("Keine unterstützte vollständige WohnPit-Sicherung.");
            validatePreferences(prefs);
            File primary=new File(temp,"databases/wattpit-history.db");if(!primary.isFile())throw new IOException("Die Messwertdatenbank fehlt.");
            File[] databases=new File(temp,"databases").listFiles();if(databases!=null)for(File file:databases)if(file.getName().endsWith(".db")){
                try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS);Cursor cursor=db.rawQuery("PRAGMA quick_check",null)){if(!cursor.moveToFirst()||!cursor.getString(0).equals("ok"))throw new IOException("Die Datenbankprüfung ist fehlgeschlagen.");}
            }
            remove(stage(c));Files.move(temp.toPath(),stage(c).toPath(),StandardCopyOption.ATOMIC_MOVE);return manifest.put("bytes",total);
        }catch(Exception e){remove(temp);throw e;}
    }
    static synchronized void schedule(Context c)throws Exception{
        if(pending(c)||!new File(stage(c),"manifest.json").isFile()||!new File(stage(c),"preferences.json").isFile())throw new IOException("Keine geprüfte Sicherung zur Übernahme bereit.");
        File mark=new File(marker(c).getPath()+".tmp");write(mark,UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));Files.move(mark.toPath(),marker(c).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);stagedHere=true;
    }
    static synchronized void discard(Context c)throws IOException{if(!pending(c))remove(stage(c));}
    private static void validatePreferences(JSONObject prefs)throws Exception{
        for(Iterator<String> names=prefs.keys();names.hasNext();){String name=names.next();if(!name.matches("[A-Za-z0-9_.-]{1,120}")||name.contains(".."))throw new IOException("Ungültiger Einstellungsname.");JSONObject rows=prefs.getJSONObject(name);
            for(Iterator<String> keys=rows.keys();keys.hasNext();){JSONObject row=rows.getJSONObject(keys.next());switch(row.getString("type")){case "string":row.getString("value");break;case "int":row.getInt("value");break;case "long":row.getLong("value");break;case "float":if(!Double.isFinite(row.getDouble("value")))throw new IOException();break;case "boolean":row.getBoolean("value");break;case "set":JSONArray a=row.getJSONArray("value");for(int i=0;i<a.length();i++)a.getString(i);break;default:throw new IOException("Ungültiger Einstellungstyp.");}}
        }
    }
    static synchronized void applyPending(Context c)throws Exception{
        if(!pending(c))return;if(stagedHere)throw new IOException("Bitte WohnPit zuerst vollständig neu starten.");
        String session=new String(Files.readAllBytes(marker(c).toPath()),StandardCharsets.UTF_8);
        if(!session.matches("[a-f0-9-]{36}"))throw new IOException("Ungültiger Übernahmestand.");
        File input=stage(c);JSONObject incoming=json(new File(input,"preferences.json"));validatePreferences(incoming);
        File rollback=new File(c.getFilesDir(),STAGE+"-previous");
        File ready=new File(rollback,"ready");
        if(!ready.isFile()||!new String(Files.readAllBytes(ready.toPath()),StandardCharsets.UTF_8).equals(session)){
            remove(rollback);if(!rollback.mkdirs())throw new IOException("Kein Platz für die bisherige Installation.");
            write(new File(rollback,"preferences.json"),preferences(c).toString().getBytes(StandardCharsets.UTF_8));File dbFolder=c.getDatabasePath("wattpit-history.db").getParentFile();copyFolder(dbFolder,new File(rollback,"databases"));
            File cache=new File(c.getFilesDir(),"lan-display.json");if(cache.isFile())atomicCopy(cache,new File(rollback,"files/lan-display.json"));write(new File(rollback,"ready"),session.getBytes(StandardCharsets.UTF_8));
        }
        // Marker and original backup survive interruption; the next cold start repeats safely.
        File target=c.getDatabasePath("wattpit-history.db").getParentFile();clearFiles(target);copyFolder(new File(input,"databases"),target);
        restorePreferences(c,incoming);c.getSharedPreferences("wattpit",0).edit().putBoolean("recording",false).commit();
        File cache=new File(input,"files/lan-display.json");if(cache.isFile())atomicCopy(cache,new File(c.getFilesDir(),"lan-display.json"));
        if(!c.getSharedPreferences("wohnpit-role",0).edit().remove("role").commit())throw new IOException("Betriebsart konnte nicht übernommen werden.");
        if(!marker(c).delete())throw new IOException("Übernahme konnte nicht abgeschlossen werden.");remove(input);
        c.getSharedPreferences("wohnpit-migration",0).edit().putBoolean("completed",true).putLong("at",System.currentTimeMillis()).commit();
    }
    private static JSONObject json(File file)throws Exception{if(file.length()>64*1024*1024)throw new IOException("Ungültige Übernahmedaten.");return new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));}
    private static void write(File file,byte[] data)throws IOException{File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException();try(FileOutputStream out=new FileOutputStream(file)){out.write(data);out.getFD().sync();}}
    private static void atomicCopy(File from,File to)throws IOException{File temp=new File(to.getPath()+".takeover-tmp");File parent=to.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException();try(InputStream in=new FileInputStream(from);FileOutputStream out=new FileOutputStream(temp)){copy(in,out,MAX_BYTES);out.getFD().sync();}Files.move(temp.toPath(),to.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
    private static void copyFolder(File from,File to)throws IOException{File[] list=from.listFiles();if(list!=null)for(File file:list)if(file.isFile())atomicCopy(file,new File(to,file.getName()));}
    private static void clearFiles(File folder)throws IOException{if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();File[] list=folder.listFiles();if(list!=null)for(File file:list)if(file.isFile()&&!file.delete())throw new IOException("Bisherige Datenbank konnte nicht ersetzt werden.");}
    private static void remove(File file)throws IOException{if(!file.exists())return;if(file.isDirectory()){File[] list=file.listFiles();if(list!=null)for(File child:list)remove(child);}if(!file.delete())throw new IOException("Temporäre Übernahmedaten konnten nicht entfernt werden.");}
    private MigrationBackup(){}
}
