package de.wattpit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import de.wattpit.app.core.CsvCodec;
import de.wattpit.app.core.PairingQr;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

/** Small native host console. Measurements and the LAN outlive this Activity. */
public final class CentralActivity extends Activity {
    private static final int PERMISSIONS=11,BLUETOOTH=12,IMPORT_SETTINGS=20,IMPORT_CSV=21,EXPORT_CSV=22,DIAGNOSTICS=23,IMPORT_HISTORY=24;
    private int BG=0xff0b1523,CARD=0xff142236,TEXT=0xffe7eff8,MUTED=0xffa0b1c5,ACCENT=0xff80b7ed;
    private GridLayout sections;private final java.util.List<LinearLayout> panels=new java.util.ArrayList<>();private JSONObject appearance=new JSONObject();private String selectedPanel="Energiegeräte";private TextView output;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService files=Executors.newSingleThreadExecutor();
    private final AtomicBoolean reading=new AtomicBoolean();
    private Repository repository;
    private LanSync lan;
    private LinearLayout devices;
    private TextView recording,live,network,roller,archive,message;
    private TextView historyHint,archivePolicy,suppliesStatus;
    private Button startButton,pauseButton,archiveButton,cancelArchiveButton,pairButton,reconnectButton,historyButton;
    private final SharedPreferences.OnSharedPreferenceChangeListener appearanceChanged=(preferences,key)->{if(AutarPieConfig.APPEARANCE.equals(key))main.post(()->{colors();panels.clear();build();update();});};
    private boolean foreground,pendingStart;
    private volatile boolean restoringHistory;
    private String pendingArchive,lastDeviceList="";
    private final Runnable refresh=new Runnable(){@Override public void run(){if(!foreground)return;update();main.postDelayed(this,3000);}};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);repository=Repository.get(this);try{UiPreset.install(this,repository.prefs);}catch(Exception ignored){}lan=LanSync.get(this);
        if(state!=null){pendingStart=state.getBoolean("pendingStart");pendingArchive=state.getString("pendingArchive");}
        try{if(!lan.host()&&!lan.viewer())lan.create("AutarPie");}catch(Exception e){notice(Repository.safeError(e));}
        repository.prefs.registerOnSharedPreferenceChangeListener(appearanceChanged);colors();build();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("pendingStart",pendingStart);out.putString("pendingArchive",pendingArchive);}
    @Override protected void onStart(){super.onStart();foreground=true;BackgroundBleAccess.offer(this);SuppliesWeather.refresh(repository,false);main.post(refresh);if(repository.prefs.getBoolean("recording",false)&&permissions())startReady();}
    @Override protected void onStop(){foreground=false;main.removeCallbacks(refresh);super.onStop();}
    @Override protected void onDestroy(){repository.prefs.unregisterOnSharedPreferenceChangeListener(appearanceChanged);main.removeCallbacksAndMessages(null);files.shutdown();super.onDestroy();}
    private void colors(){BG=0xff0b1523;CARD=0xff142236;TEXT=0xffe7eff8;MUTED=0xffa0b1c5;try{appearance=new JSONObject(repository.prefs.getString(AutarPieConfig.APPEARANCE,"{}"));String theme=appearance.optString("theme",repository.prefs.getString("theme","dark"));boolean light=theme.equals("light")||theme.equals("system")&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_NO;if(light){BG=0xffeef3f8;CARD=0xffffffff;TEXT=0xff182d42;MUTED=0xff50677e;}ACCENT=Color.parseColor(appearance.optString("accent","#80b7ed"));}catch(Exception ignored){}}
    private android.graphics.drawable.Drawable surface(String key,int fallback){JSONObject s=appearance.optJSONObject(key);if(!appearance.optBoolean("customSurfaces")||s==null){GradientDrawable d=new GradientDrawable();d.setColor(fallback);d.setCornerRadius(key.equals("tile")?dp(12):0);return d;}try{return new Surface(Color.parseColor(s.getString("first")),Color.parseColor(s.getString("second")),s.optString("mode").equals("gradient"),s.optDouble("angle",135),key.equals("tile")?dp(12):0);}catch(Exception e){return new android.graphics.drawable.ColorDrawable(fallback);}}
    private static final class Surface extends android.graphics.drawable.Drawable{
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int first,second;final boolean gradient;final double angle;final float radius;
        Surface(int first,int second,boolean gradient,double angle,float radius){this.first=first;this.second=second;this.gradient=gradient;this.angle=angle;this.radius=radius;}
        @Override public void draw(Canvas canvas){Rect b=getBounds();paint.setColor(first);if(gradient){double a=Math.toRadians(angle),dx=Math.sin(a),dy=-Math.cos(a),span=Math.abs(dx)*b.width()+Math.abs(dy)*b.height();float x=b.exactCenterX(),y=b.exactCenterY();paint.setShader(new LinearGradient(x-(float)(dx*span/2),y-(float)(dy*span/2),x+(float)(dx*span/2),y+(float)(dy*span/2),first,second,Shader.TileMode.CLAMP));}canvas.drawRoundRect(new RectF(b),radius,radius,paint);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(LinearLayout parent,String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);v.setPadding(0,dp(5),0,dp(5));parent.addView(v);return v;}
    private LinearLayout card(LinearLayout parent,String title){
        LinearLayout v=column();v.setPadding(dp(18),dp(12),dp(18),dp(16));v.setBackground(surface("tile",CARD));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(v,p);
        if(!title.equals("Aufzeichnung")){panels.add(v);v.setVisibility(title.equals(selectedPanel)?View.VISIBLE:View.GONE);Button choose=new Button(this);choose.setText(title);choose.setAllCaps(false);choose.setTextSize(12);tint(choose,false);GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(3),dp(3),dp(3),dp(3));sections.addView(choose,gp);choose.setOnClickListener(button->{selectedPanel=title;for(LinearLayout panel:panels)panel.setVisibility(panel==v?View.VISIBLE:View.GONE);});}
        text(v,title,18,ACCENT).setTypeface(null,Typeface.BOLD);return v;
    }
    private Button button(LinearLayout parent,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->{action.run();update();});
        tint(b,false);parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
    }
    private void tint(Button button,boolean primary){
        int[][] states={new int[]{-android.R.attr.state_enabled},new int[]{}};
        button.setBackgroundTintList(new ColorStateList(states,new int[]{0xff24313f,primary?ACCENT:0xff314357}));
        button.setTextColor(new ColorStateList(states,new int[]{0xff8091a2,primary?BG:TEXT}));
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setFitsSystemWindows(true);scroll.setBackground(surface("app",BG));
        LinearLayout root=column();root.setPadding(dp(18),dp(18),dp(18),dp(20));scroll.addView(root);setContentView(scroll);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.bottomMargin=dp(16);root.addView(header,hp);
        ImageView icon=new ImageView(this);icon.setImageResource(getApplicationInfo().icon);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(60),dp(60));ip.setMarginEnd(dp(12));header.addView(icon,ip);
        LinearLayout brand=column();header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        text(brand,"AutarPie",20,TEXT).setTypeface(null,Typeface.BOLD);
        TextView tagline=text(brand,"DEINE ZENTRALE IM WOHNWAGEN",10,ACCENT);tagline.setLetterSpacing(.08f);
        text(brand,"Version "+BuildConfig.VERSION_NAME,12,MUTED);
        sections=new GridLayout(this);sections.setColumnCount(2);root.addView(sections,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout status=card(root,"Aufzeichnung");recording=text(status,"Bereit",20,TEXT);live=text(status,"Noch keine Messwerte",15,MUTED);
        text(status,"Sammelt, speichert und steuert auch bei ausgeschaltetem Display. Prognosen und Auswertungen berechnen deine Anzeigehandys.",14,MUTED);
        startButton=button(status,"Aufzeichnung starten",()->{startButton.setEnabled(false);repository.prefs.edit().putBoolean("cockpitNotice-v1",true).apply();requestStart(null);});tint(startButton,true);startButton.setEnabled(false);
        pauseButton=button(status,"Aufzeichnung pausieren",()->{pauseButton.setEnabled(false);pendingStart=false;pendingArchive=null;repository.prefs.edit().putBoolean("recording",false).apply();stopService(new Intent(this,MonitorService.class));});pauseButton.setEnabled(false);
        LinearLayout supplies=card(root,"Gas, Wasser und Wetter");suppliesStatus=text(supplies,"Noch keine Einträge",15,TEXT);text(supplies,"AutarPie speichert Beschaffungen, Gaswechsel und Wetterdaten. Einträge, Verbrauchslernen und Reichweiten bedienst du im AutarPie-Cockpit auf dem Anzeigehandy.",14,MUTED);
        LinearLayout plant=card(root,"Energiegeräte");devices=column();plant.addView(devices);button(plant,"Victron-Gerät hinzufügen",()->editDevice(null));
        archive=text(plant,"Kein Archivabruf aktiv.",14,MUTED);archivePolicy=text(plant,"Automatischer Abruf: "+archiveIntervalLabel(),13,MUTED);
        button(plant,"Automatischen Abruf einstellen",this::archiveSettings);
        archiveButton=button(plant,"Fehlende Archivdaten abrufen",()->{archiveButton.setEnabled(false);requestStart("MANUAL");});archiveButton.setEnabled(false);
        cancelArchiveButton=button(plant,"Archivabruf abbrechen",()->{cancelArchiveButton.setEnabled(false);if(repository.archiveBusy)startService(new Intent(this,ArchiveService.class).setAction("CANCEL"));});cancelArchiveButton.setEnabled(false);
        LinearLayout load=card(root,"Lastausgang");output=text(load,"Noch keine bestätigten Werte",16,TEXT);text(load,"Zuordnung und Ladeplanung auf einem Anzeigehandy einstellen. Die Zentrale führt zeitlich begrenzte Pläne aus und überwacht Abschaltbedingungen.",14,MUTED);
        button(load,"Lastausgang ausschalten",()->{files.execute(()->{try{EnergyStore.manual(this,false);notice("Ausschalten angefordert; bestätigten Zustand abwarten.");}catch(Exception e){notice(Repository.safeError(e));}});});
        button(load,"Lastausgang einschalten",()->{files.execute(()->{try{EnergyStore.manual(this,true);notice("Einschalten angefordert; bestätigten Zustand abwarten.");}catch(Exception e){notice(Repository.safeError(e));}});});
        LinearLayout ride=card(root,"Roller und ScootPit");roller=text(ride,"Noch kein Roller eingerichtet",15,TEXT);
        text(ride,"Auf dem Fahrhandy arbeiten AutarPie und ScootPit wie bisher zusammen. Die bestehende BMS-Übergabe bleibt erhalten.",14,MUTED);
        button(ride,"Roller-BMS einrichten",this::editRoller);reconnectButton=button(ride,"BMS erneut verbinden",()->{RollerHandover.get(this).reconnect();notice("BMS-Verbindung wird geprüft.");});reconnectButton.setEnabled(false);
        LinearLayout lanCard=card(root,"Anlagenverbund");network=text(lanCard,"WLAN-Verbindung wird vorbereitet",15,TEXT);
        pairButton=button(lanCard,"Anzeigehandy koppeln",this::pairing);pairButton.setEnabled(false);
        text(lanCard,"Alle Handys müssen im selben WLAN sein. Den Code in AutarPie unter Anlagenverbund einlesen oder einfügen.",14,MUTED);
        LinearLayout settings=card(root,"Einstellungen und Daten");button(settings,"Betriebsart ändern",()->startActivity(new Intent(this,MainActivity.class).putExtra("chooseRole",true)));button(settings,"Übernahmesicherung einlesen",()->startActivity(new Intent(this,MigrationActivity.class)));button(settings,"Vollständige Übernahmesicherung",()->startActivity(new Intent(this,MigrationActivity.class).putExtra("export",true)));button(settings,"Speicherintervall und Akkukapazität",this::settings);
        button(settings,"Autostart einstellen",()->{Switch toggle=new Switch(this);toggle.setText("Nach Neustart wieder starten");toggle.setChecked(repository.prefs.getBoolean("autostart",true));new AlertDialog.Builder(this).setTitle("Autostart").setMessage("Eine pausierte Aufzeichnung bleibt pausiert. Android kann den ersten Geräte-Entsperrvorgang und zusätzliche Herstellerfreigaben verlangen.").setView(toggle).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->repository.prefs.edit().putBoolean("autostart",toggle.isChecked()).apply()).show();});
        button(settings,"AutarPie-Einstellungen importieren",()->choose(IMPORT_SETTINGS,false,"application/json",null));
        button(settings,"AutarPie-Verlauf importieren",()->choose(IMPORT_CSV,false,"text/*",null));
        historyButton=button(settings,"Verbrauch und Lernstand übernehmen",()->{
            if(repository.running||repository.archiveBusy){notice("Bitte zuerst die Aufzeichnung pausieren und einen laufenden Archivabruf beenden.");return;}
            choose(IMPORT_HISTORY,false,"application/zip",null);
        });
        historyButton.setEnabled(false);historyHint=text(settings,"Übernimmt frühere Anlagen- und Wetterdaten sowie vorhandene Lernabschnitte und Prognosevergleiche aus der AutarPie-Diagnose. Dafür Aufzeichnung und Archivabruf pausieren.",13,MUTED);
        button(settings,"Verlauf als CSV sichern",()->choose(EXPORT_CSV,true,"text/csv","AutarPie-Verlauf.csv"));
        button(settings,"Diagnose speichern",()->choose(DIAGNOSTICS,true,"application/zip","AutarPie-Diagnose-"+BuildConfig.VERSION_NAME+".zip"));
        button(settings,"Android-App-Einstellungen",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        message=text(root,"",14,ACCENT);text(root,"© Tim Weißbach · AutarPie",12,MUTED);
    }
    private void update(){
        if(files.isShutdown()||!reading.compareAndSet(false,true))return;
        files.execute(()->{try{
            JSONObject energy=EnergyStore.snapshot(repository),o=energy.getJSONObject("output");
            JSONObject supply=SuppliesStore.snapshot(repository);JSONObject data=repository.centralState(),r=RollerHandover.view(this),n=lan.state();String sync=repository.syncState();
            runOnUiThread(()->{if(!foreground)return;try{
                if(output!=null){long now=System.currentTimeMillis(),at=o.optLong("at");boolean fresh=at>0&&now>=at&&now-at<=45000;output.setText((!fresh?"Keine aktuellen Ausgangswerte":(o.optInt("state",-1)==1?"An":o.optInt("state",-1)==0?"Aus":"Unbekannt")+" · "+(o.has("current")?String.format(Locale.GERMANY,"%.2f A",o.optDouble("current")):"Strom unbekannt"))+(energy.optString("outputError").isEmpty()?"":"\n"+energy.optString("outputError")));}
                boolean active=data.optBoolean("running"),busy=new JSONObject(sync).optBoolean("busy");
                JSONObject weather=supply.optJSONObject("weather");long weatherAt=weather==null?0:weather.optLong("updated");suppliesStatus.setText(supply.getJSONArray("events").length()+" gespeicherte Einträge"+(weatherAt>0?" · Wetterstand "+android.text.format.DateFormat.format("dd.MM. HH:mm",weatherAt):" · Wetter noch nicht ergänzt")+(weather!=null&&weather.optBoolean("pending")?" · Abruf läuft …":"")+(weather!=null&&!weather.optString("error").isEmpty()?"\n"+weather.optString("error"):""));
                recording.setText(active?"Aufzeichnung aktiv":pendingStart?"Start wird vorbereitet …":"Aufzeichnung pausiert");recording.setTextColor(active?ACCENT:0xffe8bb57);
                startButton.setEnabled(!active&&!pendingStart&&!restoringHistory);pauseButton.setEnabled((active||pendingStart)&&!restoringHistory);
                StringBuilder values=new StringBuilder(data.optString("status"));JSONArray rows=data.optJSONArray("readings");
                if(values.toString().equals(recording.getText().toString()))values.setLength(0);
                if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject p=rows.getJSONObject(i);long age=Math.max(0,(System.currentTimeMillis()-p.optLong("t"))/1000);if(values.length()>0)values.append("\n");values.append(p.optString("kind").equals("solar")?"Solar":"Batterie").append(": ");
                    if(p.optString("kind").equals("solar"))values.append(number(p,"solar")).append(" W");else values.append(number(p,"voltage")).append(" V · ").append(number(p,"current")).append(" A · ").append(number(p,"soc")).append(" %");
                    values.append(age>45?" · letzter Wert vor "+age+" s":" · aktuell");}
                live.setText(values.toString());live.setVisibility(values.length()==0?View.GONE:View.VISIBLE);JSONObject progress=new JSONObject(sync).optJSONObject("archive");String detail=progress==null?"":progress.optString("text");
                archive.setText(busy?"Archivabruf läuft\n"+detail:"Kein Archivabruf aktiv."+(detail.isEmpty()?"\nArchivdaten werden nur bei Bedarf ergänzt.":"\nLetzter Abruf: "+detail));archive.setTextColor(busy?0xffe8bb57:MUTED);archivePolicy.setText("Automatischer Abruf: "+archiveIntervalLabel());
                cancelArchiveButton.setEnabled(busy);archiveButton.setEnabled(!busy&&!pendingStart&&!restoringHistory&&data.getJSONArray("devices").length()>0);
                historyButton.setEnabled(!active&&!busy&&!pendingStart&&!restoringHistory);
                historyHint.setText(active||busy?"Für diesen Import zuerst die Aufzeichnung pausieren und den Archivabruf beenden.":"Diagnose-ZIP der bisherigen AutarPie-Zentrale auswählen. Ergänzt den historischen Verbrauch und den bereits gespeicherten Lernstand.");
                JSONArray peers=n.optJSONArray("peers");network.setText((n.optBoolean("hosting")?"Zentrale im WLAN erreichbar":"Zentrale wartet auf den Start")+"\nAnzeigehandys verbunden: "+(peers==null?0:peers.length())+(!n.optString("error").isEmpty()?"\n"+n.optString("error"):""));pairButton.setEnabled(n.optBoolean("hosting"));
                JSONObject profile=r.optJSONObject("profile"),h=r.optJSONObject("handover");roller.setText((profile==null?"Roller":profile.optString("name"))+"\n"+r.optString("status")+(h==null?"":"\n"+h.optString("status")));
                reconnectButton.setEnabled(active&&profile!=null&&!profile.optString("mac").isEmpty()&&(h==null||h.optString("phase").equals("host")));
                JSONArray list=data.getJSONArray("devices"),found=data.getJSONArray("found");String signature=list.toString()+found.toString();if(!signature.equals(lastDeviceList)){lastDeviceList=signature;showDevices(list,found);}
            }catch(Exception ignored){}});
        }catch(Exception ignored){}finally{reading.set(false);}});
    }
    private static String number(JSONObject p,String field){double v=p.optDouble(field,Double.NaN);return Double.isFinite(v)?String.format(Locale.GERMANY,"%.1f",v):"–";}
    private void showDevices(JSONArray list,JSONArray found)throws JSONException{
        devices.removeAllViews();Set<String> saved=new HashSet<>();
        if(list.length()==0)text(devices,"SmartSolar und SmartShunt hier einrichten oder vom Anzeigehandy aus hinzufügen.",14,MUTED);
        for(int i=0;i<list.length();i++){JSONObject device=list.getJSONObject(i);saved.add(device.getString("mac"));button(devices,device.optString("name")+" · bearbeiten",()->editDevice(device));}
        for(int i=0;i<found.length();i++){JSONObject device=found.getJSONObject(i);if(!saved.contains(device.optString("mac")))button(devices,"Gefunden: "+device.optString("name"),()->editDevice(device));}
    }
    private EditText field(LinearLayout form,String label,String value,boolean numeric){text(form,label,14,MUTED);EditText input=new EditText(this);input.setTextColor(TEXT);input.setSingleLine(true);input.setText(value);if(numeric)input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);form.addView(input);return input;}
    private LinearLayout form(){LinearLayout v=column();v.setPadding(dp(20),dp(8),dp(20),dp(8));return v;}
    private interface Save {void run()throws Exception;}
    private void dialog(String title,LinearLayout form,Save save,Runnable remove){
        ScrollView scroll=new ScrollView(this);scroll.addView(form);AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Speichern",null).setNegativeButton("Abbrechen",null).create();
        if(remove!=null)d.setButton(AlertDialog.BUTTON_NEUTRAL,"Entfernen",(a,b)->new AlertDialog.Builder(this).setMessage("Gerät entfernen? Gespeicherte Messwerte bleiben erhalten.").setPositiveButton("Entfernen",(x,y)->remove.run()).setNegativeButton("Abbrechen",null).show());
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{save.run();d.dismiss();lastDeviceList="";update();}catch(Exception e){Toast.makeText(this,Repository.safeError(e),Toast.LENGTH_LONG).show();}}));d.show();
    }
    private void editDevice(JSONObject given){
        JSONObject row=given==null?new JSONObject():given;LinearLayout f=form();EditText name=field(f,"Name",row.optString("name"),false),mac=field(f,"Bluetooth-Adresse",row.optString("mac"),false);
        text(f,"Gerätetyp",14,MUTED);Spinner kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"SmartSolar","SmartShunt"}));kind.setSelection(row.optString("kind").equals("shunt")?1:0);f.addView(kind);
        EditText key=field(f,"Instant-Readout-Schlüssel (32 Zeichen)","",false);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        text(f,"Den Schlüssel findest du in VictronConnect. Bei einem eingerichteten Gerät lässt du das Feld für einen unveränderten Schlüssel leer.",13,MUTED);
        dialog("Victron-Gerät",f,()->{JSONObject input=new JSONObject().put("mac",mac.getText().toString()).put("name",name.getText().toString()).put("kind",kind.getSelectedItemPosition()==0?"solar":"shunt").put("key",key.getText().toString());if(row.optBoolean("hasKey"))input.put("original",row.optString("mac"));repository.saveDevice(input);notice("Gerät gespeichert.");},row.optBoolean("hasKey")?()->{try{repository.removeDevice(row.getString("mac"));lastDeviceList="";update();}catch(Exception e){notice(Repository.safeError(e));}}:null);
    }
    private void editRoller(){try{
        JSONObject profile=RollerStore.get(this).snapshot().getJSONObject("profile");LinearLayout f=form();EditText name=field(f,"Name",profile.optString("name","Roller"),false),mac=field(f,"Bluetooth-Adresse des BMS",profile.optString("mac"),false);
        CheckBox auto=new CheckBox(this);auto.setText("Rollerladung automatisch als Sondernutzung erfassen");auto.setTextColor(TEXT);auto.setChecked(profile.optBoolean("autoSpecialUse"));f.addView(auto);
        text(f,"Kapazität, Ladeziel und weitere Werte kannst du wie bisher in AutarPie einstellen.",13,MUTED);
        dialog("Roller-BMS",f,()->{profile.put("name",name.getText().toString()).put("mac",mac.getText().toString()).put("autoSpecialUse",auto.isChecked());RollerStore.get(this).configure(profile.toString());notice("Roller-BMS gespeichert.");},null);
    }catch(Exception e){notice(Repository.safeError(e));}}
    private void settings(){LinearLayout f=form();EditText capacity=field(f,"Wohnwagenakku in Ah",String.valueOf(repository.prefs.getFloat("capacity",100)),true);text(f,"Messwerte speichern alle",14,MUTED);Spinner seconds=new Spinner(this);int[] choices={5,10,30,60};seconds.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"5 Sekunden","10 Sekunden","30 Sekunden","60 Sekunden"}));for(int i=0;i<choices.length;i++)if(choices[i]==repository.prefs.getInt("sampleSeconds",10))seconds.setSelection(i);f.addView(seconds);
        dialog("Aufzeichnung",f,()->{repository.saveSettings(new JSONObject().put("capacity",Double.parseDouble(capacity.getText().toString().replace(',','.'))).put("sampleSeconds",choices[seconds.getSelectedItemPosition()]));notice("Aufzeichnungseinstellungen gespeichert.");},null);
    }
    private String archiveIntervalLabel(){int minutes=repository.archiveIntervalMinutes();return minutes==0?"nur manuell":minutes==60?"stündlich":"alle "+minutes+" Minuten";}
    private void archiveSettings(){
        LinearLayout f=form();text(f,"Wie oft soll AutarPie fehlende Gerätedaten nachladen? Livewerte und BMS-Übergabe laufen unabhängig weiter.",14,MUTED);
        int[] values={15,30,60,0};Spinner interval=new Spinner(this);interval.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Alle 15 Minuten","Alle 30 Minuten","Stündlich (60 Minuten)","Nur manuell"}));
        for(int i=0;i<values.length;i++)if(values[i]==repository.archiveIntervalMinutes())interval.setSelection(i);f.addView(interval);
        text(f,"Stündlich ist die Voreinstellung. Die nachladbare 30-Sekunden-Stufe reicht etwa zwei Stunden zurück; die 1-Sekunden-Stufe nur etwa zwei Minuten.",13,MUTED);
        dialog("Automatischer Archivabruf",f,()->{repository.saveArchiveInterval(values[interval.getSelectedItemPosition()]);CentralLog.add(this,"Archiv","Automatischer Abruf: "+archiveIntervalLabel());notice("Automatischer Archivabruf: "+archiveIntervalLabel());},null);
    }
    private void pairing(){try{
        if(!lan.state().optBoolean("hosting")){notice("Bitte zuerst die Aufzeichnung starten. Danach lässt sich das Anzeigehandy koppeln.");return;}
        String code=lan.pairingCode();LinearLayout f=form();text(f,"In AutarPie auf dem Anzeigehandy scannen oder den Verbindungscode einfügen.",14,TEXT);
        ImageView qr=new ImageView(this);Bitmap image=Bitmap.createBitmap(PairingQr.pixels(code),PairingQr.SIZE,PairingQr.SIZE,Bitmap.Config.ARGB_8888);qr.setImageBitmap(image);qr.setContentDescription("QR-Code für den Anlagenverbund");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(248),dp(248));p.gravity=Gravity.CENTER_HORIZONTAL;f.addView(qr,p);
        TextView value=text(f,code,11,MUTED);value.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(f);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Anzeigehandy koppeln").setView(scroll).setPositiveButton("Code kopieren",(a,b)->{getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("AutarPie-Verbindungscode",code));notice("Verbindungscode kopiert.");}).setNegativeButton("Schließen",null).create();d.setOnDismissListener(x->{qr.setImageDrawable(null);image.recycle();});d.show();
    }catch(Exception e){notice(Repository.safeError(e));}}
    private boolean permissions(){return Build.VERSION.SDK_INT>=31?checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED:checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void requestStart(String archiveMac){pendingStart=true;pendingArchive=archiveMac;List<String> need=new ArrayList<>();if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)need.add(Manifest.permission.BLUETOOTH_SCAN);if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)need.add(Manifest.permission.BLUETOOTH_CONNECT);}else if(!permissions())need.add(Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)need.add(Manifest.permission.POST_NOTIFICATIONS);if(!need.isEmpty())requestPermissions(need.toArray(new String[0]),PERMISSIONS);else startReady();}
    @SuppressLint("MissingPermission") private void startReady(){
        if(!permissions()){pendingStart=false;pendingArchive=null;notice("Bitte die Bluetooth-Berechtigungen in den Android-App-Einstellungen erlauben.");return;}
        BluetoothManager manager=getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null){pendingStart=false;pendingArchive=null;notice("Dieses Gerät unterstützt kein Bluetooth LE.");return;}
        if(!adapter.isEnabled()){pendingStart=true;startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),BLUETOOTH);return;}
        try{pendingStart=false;startForegroundService(new Intent(this,MonitorService.class));startForegroundService(new Intent(this,LanService.class));if(pendingArchive!=null){startForegroundService(new Intent(this,ArchiveService.class).putExtra("mac",pendingArchive));pendingArchive=null;}update();}catch(Exception e){notice("Bitte die App öffnen und die Aufzeichnung erneut starten.");}
    }
    @Override public void onRequestPermissionsResult(int code,String[] names,int[] results){super.onRequestPermissionsResult(code,names,results);if(code==PERMISSIONS&&pendingStart)startReady();}
    private void choose(int code,boolean create,String type,String title){Intent i=new Intent(create?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type);if(title!=null)i.putExtra(Intent.EXTRA_TITLE,title);try{startActivityForResult(i,code);}catch(Exception e){notice("Dateiauswahl konnte nicht geöffnet werden.");}}
    private String read(Uri uri,int limit)throws IOException{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()+n>limit)throw new IOException("Datei zu groß");out.write(block,0,n);}return out.toString("UTF-8");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==BLUETOOTH){if(result==RESULT_OK&&pendingStart)startReady();else{pendingStart=false;pendingArchive=null;}return;}if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(request==IMPORT_HISTORY){restoringHistory=true;startButton.setEnabled(false);archiveButton.setEnabled(false);historyButton.setEnabled(false);}files.execute(()->{try{
        if(request==IMPORT_SETTINGS){repository.importConfiguration(read(uri,ConfigurationBackup.MAX_BYTES));notice("Einstellungen übernommen. Bluetooth-Geräte und Schlüssel werden separat eingerichtet.");}
        if(request==IMPORT_CSV){CsvCodec.Table table=CsvCodec.parse(read(uri,25*1024*1024));Map<String,Integer> fields=CsvCodec.guess(table.headers);if(!fields.containsKey("timestamp")||!fields.containsKey("kind")||!fields.containsKey("device"))throw new IllegalArgumentException("Bitte eine vollständige AutarPie-Verlaufsdatei importieren.");int[] counts=repository.db.importTable(table,fields,"solar");repository.invalidateStats();notice(counts[0]+" Messwerte übernommen · "+counts[1]+" bereits vorhanden · "+counts[2]+" verworfen.");}
        if(request==IMPORT_HISTORY){
            notice("Historische Anlagen-, Wetter- und Lerndaten werden übernommen …");
            JSONObject input;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();input=HistoryMigration.readZip(in);}
            SolarPlanning plan=new SolarPlanning(getApplicationContext(),repository,value->{});
            try{
                HistoryMigration.Result transfer=HistoryMigration.restore(repository,plan,RollerLedger.get(this),input);
                CentralLog.add(this,"Umzug",transfer.inserted+" historische Grundlagen ergänzt · "+transfer.updated+" Wetterarchive vervollständigt · "+transfer.tables.optInt("observations")+" Lernabschnitte · "+transfer.tables.optInt("forecasts")+" Prognosevergleiche · "+transfer.existing+" bereits vorhanden");
                notice(transfer.inserted+" Grundlagen ergänzt, davon "+transfer.tables.optInt("observations")+" Lernabschnitte und "+transfer.tables.optInt("forecasts")+" Prognosevergleiche. AutarPie danach neu öffnen.");
            }finally{plan.shutdown();plan.close();}
        }
        if(request==EXPORT_CSV)try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();repository.db.exportTo(out);notice("Verlauf gespeichert.");}
        if(request==DIAGNOSTICS)try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();JSONObject diagnostic=new JSONObject().put("app","AutarPie").put("version",BuildConfig.VERSION_NAME).put("at",System.currentTimeMillis()).put("sync",new JSONObject(repository.syncState())).put("archive",repository.diagnosticText()).put("roller",RollerHandover.view(this)).put("events",CentralLog.read(this));try(ZipOutputStream zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("diagnose.json"));zip.write(diagnostic.toString(2).getBytes(StandardCharsets.UTF_8));zip.closeEntry();}notice("Diagnose gespeichert.");}
    }catch(Exception e){notice(Repository.safeError(e));}finally{if(request==IMPORT_HISTORY){restoringHistory=false;runOnUiThread(this::update);}}});}
    private void notice(String value){runOnUiThread(()->{if(isDestroyed())return;if(message!=null)message.setText(value);Toast.makeText(this,value,Toast.LENGTH_LONG).show();});}
}
