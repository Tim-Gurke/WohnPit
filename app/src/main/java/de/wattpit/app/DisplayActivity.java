package de.wattpit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import de.wattpit.app.core.CsvCodec;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public final class DisplayActivity extends Activity {
    private WebView web;
    private Repository repository;
    private LanSync lan;
    private SolarPlanning solarPlanning;
    private SiteSensors siteSensors;
    private boolean pendingSiteLocation;
    private static final int SITE_PERMISSION=24;
    private final ExecutorService files = Executors.newSingleThreadExecutor();
    private final ExecutorService historyReads = Executors.newSingleThreadExecutor();
    private CsvCodec.Table pendingImport;
    private volatile String pendingSettingsExport;
    private boolean pageReady;
    private JSONObject pendingSettingsPreview;
    private JSONObject pendingLanScan;
    private boolean pendingStart;
    private String pendingArchive;
    private long stoppedAt;
    private boolean entered,foreground,checkingArchive;
    private static final int PERMISSIONS = 11, BLUETOOTH = 12, IMPORT = 20, EXPORT = 21, IMPORT_SETTINGS = 22, EXPORT_SETTINGS = 23, EXPORT_DIAGNOSTICS = 25, IMPORT_CHARGER = 26;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null)pendingSettingsExport=savedInstanceState.getString("pendingSettingsExport");
        repository = Repository.get(this);
        try{UiPreset.install(this,repository.prefs);DashboardPreset.install(repository.prefs,DashboardPreset.read(this));}catch(Exception e){repository.message(Repository.safeError(e));}
        lan=LanSync.get(this);
        solarPlanning=new SolarPlanning(this,repository,value->call("onSolarPlanData",value));
        if(!lan.viewer())PresenceStore.get(this).tick();
        siteSensors=new SiteSensors(this,repository,this::call);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(13,18,27));
        web.setFitsSystemWindows(true);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setAllowFileAccessFromFileURLs(false);
        web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setBlockNetworkLoads(true);
        web.getSettings().setTextZoom(100);
        web.getSettings().setSupportZoom(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url){
                pageReady=true;
                if(AppRole.viewerOnly(DisplayActivity.this)&&!lan.viewer())web.evaluateJavascript("document.getElementById('lanManage').click()",null);
                if(pendingSettingsPreview!=null){JSONObject result=pendingSettingsPreview;pendingSettingsPreview=null;call("onSettingsImportPreview",result);}
                if(pendingLanScan!=null){JSONObject result=pendingLanScan;pendingLanScan=null;call("onLanScanned",result);}
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith("file:///android_asset/")) return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                return null;
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(),"WattPit");
        setContentView(web);
        applyTheme(repository.prefs.getString("theme","dark"));
        web.loadUrl("file:///android_asset/index.html");

    }

    @Override protected void onSaveInstanceState(Bundle state){
        if(pendingSettingsExport!=null)state.putString("pendingSettingsExport",pendingSettingsExport);
        super.onSaveInstanceState(state);
    }
    @Override protected void onStart(){
        super.onStart();
        foreground=true;
        if(!AppRole.viewerOnly(this)&&!lan.viewer())BackgroundBleAccess.offer(this);
        web.onResume();web.resumeTimers();solarPlanning.displayActive(true);
        try{call("onDisplayActive",new JSONObject().put("active",true));}catch(JSONException ignored){}
        for(java.util.Map.Entry<String,JSONObject> pending:new java.util.HashMap<>(deferredCalls).entrySet())call(pending.getKey(),pending.getValue());deferredCalls.clear();
        lan.foreground(bundle->{});
        try{LanService.reconcile(this,lan);}catch(Exception ignored){}
        web.removeCallbacks(retryIncompleteArchive);web.postDelayed(retryIncompleteArchive,15000);
        // Refresh on each foreground entry; WeatherClient skips disabled weather
        // and coalesces an already running request. Cached data stays available.
        if(!AppRole.viewerOnly(this)&&!lan.viewer()){WeatherClient.refresh(repository,true);SuppliesWeather.refresh(repository,false);}
        if(!entered||stoppedAt>0&&SystemClock.elapsedRealtime()-stoppedAt>30000){
            entered=true;web.postDelayed(()->{
                startAutomaticImport();
            },800);
        }
    }
    @Override protected void onStop(){pendingSiteLocation=false;siteSensors.stop();solarPlanning.displayActive(false);web.evaluateJavascript("window.onDisplayActive && window.onDisplayActive({active:false})",null);foreground=false;lan.background();web.removeCallbacks(retryIncompleteArchive);web.onPause();web.pauseTimers();stoppedAt=SystemClock.elapsedRealtime();super.onStop();}
    private final Runnable retryIncompleteArchive=new Runnable(){@Override public void run(){startAutomaticImport(true);web.postDelayed(this,15000);}};
    private void startAutomaticImport(){
        startAutomaticImport(false);
    }
    private void startAutomaticImport(boolean retriesOnly){
        if(AppRole.viewerOnly(this))return;
        if(lan.viewer())return;
        if(!foreground||isFinishing()||isDestroyed()||pendingStart||pendingArchive!=null||checkingArchive||repositoryArchiveActive()||!repository.prefs.getBoolean("cockpitNotice-v1",false))return;
        checkingArchive=true;
        files.execute(()->{
            boolean due=false;try{due=repository.automaticArchiveDevices(retriesOnly).length()>0;}catch(Exception ignored){}
            final boolean needed=due;
            runOnUiThread(()->{
                checkingArchive=false;
                if(!foreground||isFinishing()||isDestroyed()||pendingStart||pendingArchive!=null||repositoryArchiveActive())return;
                if(needed)pendingArchive=retriesOnly?"RETRY":"AUTO";
                if(pendingArchive!=null||repository.prefs.getBoolean("recording",false)&&!repository.running)requestStart();
            });
        });
    }
    private void applyTheme(String theme) {
        boolean light = theme.equals("light");
        String palette=repository.prefs.getString("palette","classic");
        String background;
        switch(palette){
            case "blue":background=light?"#f2f6fb":"#101620";break;
            case "green":background=light?"#f3f7f1":"#111914";break;
            case "turquoise":background=light?"#f0f7f7":"#0e191c";break;
            case "orange":background=light?"#faf5ef":"#1c1612";break;
            case "neutral":background=light?"#f5f6f7":"#151719";break;
            default:background=light?"#f4f6f9":"#0d121b";
        }
        int color=Color.parseColor(background);if(web!=null)web.setBackgroundColor(color);
        getWindow().setStatusBarColor(color); getWindow().setNavigationBarColor(color);
        int flags = light ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    private final java.util.Map<String,JSONObject> deferredCalls=new java.util.HashMap<>();
    private void call(String method, JSONObject value) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && web != null){if(!foreground){deferredCalls.put(method,value);return;}web.evaluateJavascript("window." + method + " && window." + method + "(" + value.toString() + ")",null);} });
    }
    private void error(Exception e) {
        repository.message(Repository.safeError(e));
        try { call("nativeError",new JSONObject().put("message",Repository.safeError(e))); } catch (JSONException ignored) { }
    }

    @SuppressLint("MissingPermission")
    private void requestStart() {
        if(lan.viewer()){repository.message(LanSync.READ_ONLY);return;}
        if (isFinishing() || isDestroyed()) return;
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!permissions.isEmpty()) { pendingStart = true; requestPermissions(permissions.toArray(new String[0]),PERMISSIONS); return; }
        startAfterPermissions();
    }
    @SuppressLint("MissingPermission")
    private void startAfterPermissions() {
        if(lan.viewer()){pendingStart=false;pendingArchive=null;return;}
        if (isFinishing() || isDestroyed()) return;
        boolean allowed = Build.VERSION.SDK_INT >= 31 ? checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                : checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!allowed) { pendingStart=false;pendingArchive=null;repository.message("Bitte die Bluetooth-Berechtigung in den Android-App-Einstellungen erlauben."); return; }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) { repository.message("Dieses Gerät unterstützt kein Bluetooth LE. Verlauf und Demo sind weiterhin verfügbar."); return; }
        if (!adapter.isEnabled()) { pendingStart = true; startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),BLUETOOTH); return; }
        pendingStart = false;
        try {
            if(pendingArchive!=null){String mac=pendingArchive;pendingArchive=null;if(repository.prefs.getBoolean("recording",false)&&!repository.running)startForegroundService(new Intent(this,MonitorService.class));startForegroundService(new Intent(this,ArchiveService.class).putExtra("mac",mac));}
            else startForegroundService(new Intent(this,MonitorService.class));
        }
        catch (IllegalStateException | SecurityException e) { repository.message("Bitte die App öffnen, um die Aufzeichnung zu starten."); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if (requestCode == PERMISSIONS && pendingStart) startAfterPermissions();
        if(requestCode==SITE_PERMISSION&&pendingSiteLocation){pendingSiteLocation=false;siteSensors.locate();}
        if(requestCode==27&&Build.VERSION.SDK_INT>=33&&lan.viewer()&&lan.backgroundPresenceEnabled()&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            try{lan.backgroundPresence(false);LanService.reconcile(this,lan);repository.message("Hintergrundhinweise brauchen erlaubte App-Benachrichtigungen.");}catch(Exception ignored){}
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        try{JSONObject scanned=LanQr.result(requestCode,resultCode,data);if(scanned!=null){if(pageReady)call("onLanScanned",scanned);else pendingLanScan=scanned;return;}}catch(Exception e){error(e);return;}
        if (requestCode == BLUETOOTH) {
            if (resultCode == RESULT_OK) startAfterPermissions();
            else { pendingStart = false; pendingArchive=null; repository.message("Bluetooth wurde nicht eingeschaltet."); }
            return;
        }
        if(requestCode==EXPORT_SETTINGS&&resultCode!=RESULT_OK)pendingSettingsExport=null;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if(requestCode==IMPORT_CHARGER)files.execute(()->{try(InputStream input=getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException();byte[] auth=de.wattpit.app.core.ChargerCapture.read(input);StringBuilder hex=new StringBuilder();for(byte b:auth)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));java.util.Arrays.fill(auth,(byte)0);
            JSONObject cfg=lan.viewer()?new JSONObject(lan.snapshot()).getJSONObject("energy").getJSONObject("chargerConfig"):ChargerBle.publicConfig(repository);cfg.put("auth",hex.toString());cfg.put("enabled",false);
            if(lan.viewer())lan.control("saveCharger",new JSONArray().put(cfg.toString()),lan.controlRevision());else lan.localControl("saveCharger",new JSONArray().put(cfg.toString()),solarPlanning);
            repository.message("Private 608PD-Verbindung übernommen. Jetzt in den Lader-Einstellungen verbinden.");
        }catch(Exception e){error(e instanceof IOException?new IllegalArgumentException(e.getMessage()):e);}});
        if (requestCode == IMPORT) files.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] block = new byte[8192]; int n;
                while ((n = input.read(block)) != -1) { buffer.write(block,0,n); if (buffer.size() > 25*1024*1024) throw new IllegalArgumentException("Die CSV darf höchstens 25 MB groß sein."); }
                byte[] bytes = buffer.toByteArray();
                String text;
                if (bytes.length >= 2 && ((bytes[0] == (byte)0xff && bytes[1] == (byte)0xfe) || (bytes[0] == (byte)0xfe && bytes[1] == (byte)0xff))) text = new String(bytes,StandardCharsets.UTF_16);
                else {
                    try { text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
                    catch (CharacterCodingException e) { text = new String(bytes,Charset.forName("windows-1252")); }
                }
                CsvCodec.Table table = CsvCodec.parse(text);
                pendingImport = table;
                JSONArray preview = new JSONArray();
                for (int i=0;i<Math.min(3,table.rows.size());i++) preview.put(new JSONArray(table.rows.get(i)));
                JSONObject result = new JSONObject().put("headers",new JSONArray(table.headers)).put("rows",table.rows.size())
                        .put("preview",preview).put("map",new JSONObject(CsvCodec.guess(table.headers))).put("name",displayName(uri));
                call("onImportPreview",result);
            } catch (Exception e) { pendingImport = null; error(e); }
        });
        if (requestCode == EXPORT) files.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri,"wt")) {
                if (output == null) throw new IOException();
                repository.db.exportTo(output); repository.message("Verlauf als CSV gespeichert.");
            } catch (Exception e) { error(e); }
        });
        if(requestCode==EXPORT_DIAGNOSTICS)files.execute(()->{
            try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){
                if(output==null)throw new IOException();
                JSONObject diagnostic=solarPlanning.diagnosticSnapshot().get(2,TimeUnit.MINUTES);
                diagnostic.put("roller",RollerStore.get(DisplayActivity.this).snapshot());
                try(ZipOutputStream zip=new ZipOutputStream(output)){
                    zip.putNextEntry(new ZipEntry("diagnose.json"));zip.write(diagnostic.toString(2).getBytes(StandardCharsets.UTF_8));zip.closeEntry();
                    zip.putNextEntry(new ZipEntry("verlauf.csv"));repository.db.exportTo(zip);zip.closeEntry();
                }
                repository.message("Mess- und Lerndiagnose gespeichert.");
            }catch(Exception e){error(e);}
        });
        if(requestCode==IMPORT_SETTINGS)files.execute(()->{
            try(InputStream input=getContentResolver().openInputStream(uri);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
                if(input==null)throw new IOException();byte[] block=new byte[8192];int n;
                while((n=input.read(block))!=-1){buffer.write(block,0,n);if(buffer.size()>ConfigurationBackup.MAX_BYTES)throw new IllegalArgumentException("Die Einstellungsdatei darf höchstens 1,8 MB groß sein.");}
                String json=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(buffer.toByteArray())).toString();
                if(json.startsWith("\uFEFF"))json=json.substring(1);
                JSONObject backup=ConfigurationBackup.normalize(json);
                JSONObject result=new JSONObject().put("backup",backup).put("name",displayName(uri));
                runOnUiThread(()->{if(pageReady)call("onSettingsImportPreview",result);else pendingSettingsPreview=result;});
            }catch(Exception e){error(e instanceof JSONException?new IllegalArgumentException("Keine gültige AutarPie-Einstellungsdatei."):e);}
        });
        if(requestCode==EXPORT_SETTINGS){
            final String json=pendingSettingsExport;pendingSettingsExport=null;
            files.execute(()->{
                if(json==null){error(new IllegalArgumentException("Bitte die Einstellungssicherung erneut starten."));return;}
                try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){
                    if(output==null)throw new IOException();output.write(json.getBytes(StandardCharsets.UTF_8));
                    repository.message("Einstellungen als JSON gespeichert.");
                }catch(Exception e){error(e);}
            });
        }
    }
    private String displayName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) {
            return c != null && c.moveToFirst() ? c.getString(0) : "CSV-Datei";
        } catch (Exception ignored) { return "CSV-Datei"; }
    }

    private boolean repositoryArchiveActive(){return repository.archiveBusy;}
    private void requestArchive(String target){
        if(lan.viewer()){repository.message(LanSync.READ_ONLY);return;}
        runOnUiThread(()->{
            if(repositoryArchiveActive()||pendingArchive!=null){repository.message("Ein Archivimport läuft bereits oder wird vorbereitet.");return;}
            pendingArchive=target;requestStart();
        });
    }
    final class Bridge {
        private String localControl(String action,JSONArray args){try{if(AppRole.viewerOnly(DisplayActivity.this)&&!lan.viewer())return "Bitte zuerst eine Zentrale koppeln oder die Betriebsart ändern.";lan.localControl(action,args,solarPlanning);return "";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public long controlRevision(){return lan.controlRevision();}
        @JavascriptInterface public void requestPlantCommand(String json){Object plant=lan.identity();files.execute(()->{
            JSONObject result=new JSONObject();try{
                if(json.length()>2500000)throw new IllegalArgumentException("Die Bedienanfrage ist zu groß.");
                JSONObject command=new JSONObject(json);result.put("id",command.getLong("id"));
                if(plant!=lan.identity()||!lan.viewer())throw new IllegalArgumentException("Die Anlage wurde gewechselt. Bitte erneut öffnen.");
                String action=command.getString("action");JSONArray args=command.getJSONArray("args");
                lan.control(action,args,command.getLong("revision"));
                if(plant!=lan.identity())throw new IllegalArgumentException("Die Anlage wurde gewechselt. Bitte erneut öffnen.");
                if(action.equals("importSettings")){repository.importLocalAppearance(args.getString(0));runOnUiThread(()->applyTheme(repository.prefs.getString("theme","dark")));}
                result.put("ok",true);
            }catch(Exception e){try{result.put("error",e instanceof IllegalArgumentException?e.getMessage():"Keine Bestätigung vom Hauptgerät. Bitte Verbindung und aktuellen Stand prüfen.");}catch(JSONException ignored){}}
            call("onPlantResult",result);
        });}

        @JavascriptInterface public boolean isLanViewer(){return AppRole.viewerOnly(DisplayActivity.this)||lan.viewer();}
        @JavascriptInterface public String lanState(){try{JSONObject value=lan.state();if(AppRole.viewerOnly(DisplayActivity.this)&&!lan.viewer())value.put("role","viewer").put("connected",false).put("name","Noch keine Zentrale gekoppelt").put("pendingPairing",true);return value.toString();}catch(JSONException e){return "{}";}}
        @JavascriptInterface public boolean consumeLanJoined(){return lan.consumeJoined();}
        @JavascriptInterface public String createLan(String name){
            try{if(AppRole.viewerOnly(DisplayActivity.this))return "Bitte für eine Zentrale zuerst die Betriebsart ändern.";lan.create(name);runOnUiThread(()->{try{startForegroundService(new Intent(DisplayActivity.this,LanService.class));}catch(Exception e){error(e);}});return "";}
            catch(Exception e){return Repository.safeError(e);}
        }
        @JavascriptInterface public void joinLan(String code){files.execute(()->{JSONObject result=new JSONObject();try{lan.join(code);AppRole.save(DisplayActivity.this,"viewer");runOnUiThread(()->{try{LanService.reconcile(DisplayActivity.this,lan);}catch(Exception ignored){}});result.put("ok",true);}catch(Exception e){try{result.put("error",e instanceof IllegalArgumentException?e.getMessage():"Hauptgerät nicht erreichbar. Beide Geräte im selben WLAN, Hauptgerät geöffnet und Code vollständig?");}catch(JSONException ignored){}}call("onLanResult",result);});}
        @JavascriptInterface public String setLanBackgroundPresence(boolean enabled){
            boolean previous=lan.backgroundPresenceEnabled();
            try{
                lan.backgroundPresence(enabled);LanService.reconcile(DisplayActivity.this,lan);
                if(enabled&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)runOnUiThread(()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},27));
                return "";
            }
            catch(Exception e){try{lan.backgroundPresence(previous);LanService.reconcile(DisplayActivity.this,lan);}catch(Exception ignored){}return "Die Hintergrundhinweise konnten nicht umgestellt werden. App öffnen und erneut versuchen.";}
        }
        @JavascriptInterface public String leaveLan(){try{boolean resume=lan.leave();runOnUiThread(()->{stopService(new Intent(DisplayActivity.this,LanService.class));if(resume)requestStart();});return "";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public String lanPairingCode(){try{return new JSONObject().put("code",lan.pairingCode()).toString();}catch(Exception e){try{return new JSONObject().put("error",Repository.safeError(e)).toString();}catch(Exception ignored){return "{}";}}}
        @JavascriptInterface public String copyLanCode(){try{String code=lan.pairingCode();runOnUiThread(()->getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("AutarPie-Verbindungscode",code)));return "";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public String lanQrCode(){try{return new JSONObject().put("image",LanQr.image(lan.pairingCode())).toString();}catch(Exception e){try{return new JSONObject().put("error",e instanceof IllegalArgumentException?e.getMessage():"QR-Code konnte gerade nicht erstellt werden. Bitte erneut versuchen.").toString();}catch(Exception ignored){return "{}";}}}
        @JavascriptInterface public void scanLanCode(){runOnUiThread(()->{try{LanQr.scan(DisplayActivity.this);}catch(Exception e){try{call("onLanScanned",new JSONObject().put("error","Kamera konnte nicht geöffnet werden. Du kannst den Textcode einfügen."));}catch(JSONException ignored){}}});}
        @JavascriptInterface public void refreshLan(){lan.refreshNow();}
        @JavascriptInterface public String rollerSnapshot(){if(lan.viewer())return lan.part("roller");try{return RollerHandover.view(DisplayActivity.this).toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String specialUses(){if(lan.viewer())return lan.part("specialUses");try{return SpecialUses.snapshot(repository.prefs).toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String saveSpecialUses(String json){return localControl("saveSpecialUses",new JSONArray().put(json));}
        @JavascriptInterface public void presenceNotifications(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},27);else try{startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(Exception ignored){}});}
        @JavascriptInterface public String presenceSnapshot(){if(lan.viewer())return lan.part("presence");try{PresenceStore.get(DisplayActivity.this).tick();return PresenceStore.get(DisplayActivity.this).snapshot().toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String savePresence(String mode){return localControl("savePresence",new JSONArray().put(mode));}
        @JavascriptInterface public String saveAbsence(String json){return localControl("saveAbsence",new JSONArray().put(json));}
        @JavascriptInterface public String resolveAbsence(String id,String action){return localControl("resolveAbsence",new JSONArray().put(id).put(action));}
        @JavascriptInterface public String rollerReconnect(){return localControl("rollerReconnect",new JSONArray());}
        @JavascriptInterface public String saveRoller(String json){return localControl("saveRoller",new JSONArray().put(json));}
        @JavascriptInterface public String rollerSession(boolean start){if(lan.viewer())return LanSync.READ_ONLY;try{RollerStore.get(DisplayActivity.this).toggle(start);repository.invalidateStats();solarPlanning.request();return "";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public void requestRollerSession(boolean start,long expectedRevision){Object plant=lan.identity();files.execute(()->{
            JSONObject result=new JSONObject();try{
                if(plant!=lan.identity())throw new IllegalArgumentException("Anlage wurde gewechselt. Bitte den Ladestatus erneut prüfen.");
                JSONObject roller;
                if(lan.viewer())roller=lan.rollerSession(start,expectedRevision);
                else{RollerStore store=RollerStore.get(DisplayActivity.this);store.command(start,expectedRevision,de.wattpit.app.core.LanWire.random(16));repository.invalidateStats();solarPlanning.request();roller=store.snapshot();}
                result.put("ok",true).put("roller",roller);
            }catch(Exception e){try{result.put("error",e instanceof IllegalArgumentException?e.getMessage():"Keine Bestätigung vom Hauptgerät. Der Ladestatus wird bei der nächsten Verbindung erneut abgefragt.");}catch(JSONException ignored){}}
            call("onRollerResult",result);
        });}
        @JavascriptInterface public String rollerDevices(){return lan.bondedDevices();}
        @JavascriptInterface public String appVersion(){return BuildConfig.VERSION_NAME;}
        @JavascriptInterface public void openMonitorNotifications(){runOnUiThread(()->{
            try{
                MonitorService.ensureChannel(DisplayActivity.this);
                startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,MonitorService.CHANNEL));
            }catch(ActivityNotFoundException e){
                try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
                catch(Exception unavailable){repository.message("Bitte die Benachrichtigung „AutarPie-Aufzeichnung“ in den Android-App-Einstellungen öffnen.");}
            }
        });}
        @JavascriptInterface public void requestSunsetSoc(long at,long id){
            if(at<=0||at>System.currentTimeMillis())return;
            historyReads.execute(()->{
                JSONObject result=new JSONObject();
                try{
                    if(lan.viewer()){result=lan.query("sunsetSoc",new JSONArray().put(at)).put("id",id);if(!isDestroyed())call("onSolarSunsetSoc",result);return;}
                    String shunt="";JSONArray devices=repository.configs();
                    for(int i=0;i<devices.length();i++)if(devices.getJSONObject(i).optString("kind").equals("shunt")){shunt=devices.getJSONObject(i).optString("mac");break;}
                    JSONObject point=shunt.isEmpty()?null:repository.db.sunsetSoc(at,System.currentTimeMillis(),shunt);
                    result.put("id",id).put("point",point==null?JSONObject.NULL:point);
                }catch(Exception e){try{result.put("id",id).put("error","Der gespeicherte Akkustand konnte gerade nicht gelesen werden.");}catch(JSONException ignored){}}
                if(!isDestroyed())call("onSolarSunsetSoc",result);
            });
        }
        @JavascriptInterface public String syncState(){return lan.viewer()?lan.remoteSyncState():repository.syncState();}
        @JavascriptInterface public String backgroundArchive(long runId){return repository.archiveRun.background(runId)?"":"Dieser Synchronisationslauf ist bereits beendet. Bitte erneut versuchen.";}
        @JavascriptInterface public void requestHistory(String json){
            try{
                if(json.length()>4096)throw new IllegalArgumentException();
                JSONObject request=new JSONObject(json);long id=request.getLong("id");String method=request.getString("method");JSONArray args=request.getJSONArray("args");
                long from=args.getLong(0),to=args.getLong(1);if(from<0||to<=from)throw new IllegalArgumentException();final boolean remote=lan.viewer();final Object owner=lan.identity();
                historyReads.execute(()->{
                    JSONObject response=new JSONObject();
                    try{
                        JSONObject data;
                        if(remote!=lan.viewer()||owner!=lan.identity())return;
                        if(!foreground)throw new java.io.IOException("Anzeige pausiert. Beim Öffnen wird neu geladen.");
                        if(remote)data=lan.query(method,args,message->{try{call("onHistoryProgress",new JSONObject().put("id",id).put("message",message));}catch(JSONException ignored){}});else switch(method){
                            case "loadHistory":data=RollerLedger.get(DisplayActivity.this).loadHistory(RollerLedger.source(repository.prefs),from,to,args.getString(2),args.optString(3));break;
                            case "rollerEnergy":data=RollerLedger.get(DisplayActivity.this).summary(RollerLedger.source(repository.prefs),from,to);break;
                            case "history":data=new JSONObject().put("points",solarPlanning.history(from,to,args.getString(2),args.getString(3)));break;
                            case "dashboardHistory":data=solarPlanning.dashboardData(from,to,args.getString(2));break;
                            case "energyHistoryWindow":data=solarPlanning.energyFlow(from,to,args.getLong(3),args.getLong(4),args.getString(2));break;
                            case "energyHistory":data=solarPlanning.energyFlow(from,to,args.getLong(3),args.getString(2));break;
                            case "dailyEnergy":data=solarPlanning.dailyEnergy(from,to,new JSONObject(repository.prefs.getString("weatherCache","{}")));break;
                            default:throw new IllegalArgumentException();
                        }
                        response.put("id",id).put("data",data);
                    }catch(Exception e){try{response.put("id",id).put("data",new JSONObject().put("error",e instanceof java.io.IOException||e instanceof IllegalArgumentException?e.getMessage():"Zeitraum konnte nicht geladen werden."));}catch(JSONException ignored){}}
                    if(!isDestroyed()&&remote==lan.viewer()&&owner==lan.identity())call("onHistoryResult",response);
                });
            }catch(Exception ignored){}
        }
        @JavascriptInterface public String acknowledgeCockpit(){
            if(!repository.prefs.edit().putBoolean("cockpitNotice-v1",true).commit())return "Der Hinweis konnte nicht bestätigt werden. Bitte erneut versuchen.";
            runOnUiThread(()->web.postDelayed(DisplayActivity.this::startAutomaticImport,250));return "";
        }
        @JavascriptInterface public String snapshot() { if(lan.viewer())return lan.snapshot();if(repository.archiveBusy)return repository.backgroundSnapshot();try { JSONObject value=repository.snapshot();if(AppRole.viewerOnly(DisplayActivity.this))value.put("lan",new JSONObject(lanState())).put("running",false).put("status","Bitte zuerst eine Zentrale koppeln.").put("devices",new JSONArray()).put("readings",new JSONArray()).put("found",new JSONArray());return value.toString(); } catch (Exception e) { return "{}"; } }
        @JavascriptInterface public String history(long from, long to, String metric, String source) {
            if(lan.viewer())return "{\"pending\":true}";
            if(repository.archiveBusy)return "{\"syncing\":true}";
            try { return new JSONObject().put("points",solarPlanning.history(from,to,metric,source)).toString(); }
            catch (Exception e) { return "{\"points\":[],\"error\":\"Verlauf konnte nicht geladen werden.\"}"; }
        }
        @JavascriptInterface public String suppliesSnapshot(){if(lan.viewer())return lan.part("supplies");try{return SuppliesStore.snapshot(repository).toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String saveSupplies(String json){return localControl("saveSupplies",new JSONArray().put(json));}
        @JavascriptInterface public String refreshSuppliesWeather(){return localControl("refreshSuppliesWeather",new JSONArray());}
        @JavascriptInterface public String copyColorCode(String value){
            if(value==null||!value.matches("#[0-9a-fA-F]{6}"))return "Ungültiger Farbcode.";
            try{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Farbcode",value.toUpperCase(java.util.Locale.ROOT)));return "";}catch(Exception e){return "Der Farbcode konnte nicht kopiert werden.";}
        }
        @JavascriptInterface public void chooseAppRole(){runOnUiThread(()->startActivity(new Intent(DisplayActivity.this,MainActivity.class).putExtra("chooseRole",true)));}
        @JavascriptInterface public void migrationBackup(){runOnUiThread(()->startActivity(new Intent(DisplayActivity.this,MigrationActivity.class).putExtra("export",true)));}
        @JavascriptInterface public void migrationImport(){runOnUiThread(()->startActivity(new Intent(DisplayActivity.this,MigrationActivity.class)));}
        @JavascriptInterface public String appRole(){return AppRole.get(DisplayActivity.this);}
        @JavascriptInterface public String uiDefaults(){try{return repository.prefs.getBoolean(UiPreset.MARKER,false)?UiPreset.read(DisplayActivity.this).toString():"{}";}catch(Exception e){return "{}";}}
        @JavascriptInterface public String dashboardPreset(){try{return UiPreset.read(DisplayActivity.this).getJSONObject("dashboard").toString();}catch(Exception e){return "null";}}
        @JavascriptInterface public String dashboard() { return repository.prefs.getString("dashboard-v1","{}"); }
        @JavascriptInterface public String workspace(){return repository.prefs.getString(AutarPieConfig.WORKSPACE,"{}");}
        @JavascriptInterface public String appearance(){return repository.prefs.getString(AutarPieConfig.APPEARANCE,"{}");}
        @JavascriptInterface public String saveWorkspace(String json){try{JSONObject workspace=AutarPieConfig.workspace(json);SharedPreferences.Editor editor=repository.prefs.edit().putString(AutarPieConfig.WORKSPACE,workspace.toString());JSONArray pages=workspace.getJSONArray("pages");for(int i=0;i<pages.length();i++){JSONObject p=pages.getJSONObject(i);if(p.optString("id").equals("overview")&&p.optJSONObject("board")!=null)editor.putString("dashboard-v1",p.getJSONObject("board").toString());}return editor.commit()?"":"Dashboards konnten nicht gespeichert werden.";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public String requestWeatherTrend(){return localControl("requestWeatherTrend",new JSONArray());}
        @JavascriptInterface public String saveAppearance(String json){return localControl("saveAppearance",new JSONArray().put(json));}
        @JavascriptInterface public void chooseChargerCapture(){runOnUiThread(()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_CHARGER);}catch(Exception e){error(e);}});}
        @JavascriptInterface public String energySnapshot(){if(lan.viewer())try{return new JSONObject(lan.snapshot()).optJSONObject("energy").toString();}catch(Exception e){return "{}";}try{return EnergyStore.snapshot(repository).toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String saveSolarInstallation(String json){return localControl("saveSolarInstallation",new JSONArray().put(json));}
        @JavascriptInterface public String saveLoadConfiguration(String json){return localControl("saveLoadConfiguration",new JSONArray().put(json));}
        @JavascriptInterface public String setLoadOutput(boolean on){return localControl("setLoadOutput",new JSONArray().put(on));}
        @JavascriptInterface public void issueChargePlan(String json){files.execute(()->{JSONObject result=new JSONObject();try{if(lan.viewer())lan.control("submitChargePlan",new JSONArray().put(json),lan.controlRevision());else lan.localControl("submitChargePlan",new JSONArray().put(json),solarPlanning);result.put("ok",true);}catch(Exception e){try{result.put("error",Repository.safeError(e));}catch(JSONException ignored){}}call("onChargePlanResult",result);});}
        @JavascriptInterface public String submitChargePlan(String json){return localControl("submitChargePlan",new JSONArray().put(json));}
        @JavascriptInterface public String saveCharger(String json){return localControl("saveCharger",new JSONArray().put(json));}
        @JavascriptInterface public String chargerCommand(String action){return localControl("chargerCommand",new JSONArray().put(action));}
        @JavascriptInterface public String saveCentralSettings(String json){return localControl("saveCentralSettings",new JSONArray().put(json));}
        @JavascriptInterface public int displayScale(){return repository.prefs.getInt("display-scale-v1",100);}
        @JavascriptInterface public String rollerDisplay(){return repository.prefs.getString("roller-values-v1","[]");}
        @JavascriptInterface public String saveRollerDisplay(String json){
            try{
                if(json==null||json.length()>1000)throw new IllegalArgumentException();JSONArray values=new JSONArray(json);if(values.length()!=6)throw new IllegalArgumentException();
                for(int i=0;i<values.length();i++)if(!(values.get(i) instanceof String)||!values.getString(i).matches("[a-zA-Z][a-zA-Z0-9:]{0,39}"))throw new IllegalArgumentException();
                return repository.prefs.edit().putString("roller-values-v1",values.toString()).commit()?"":"Die Wertauswahl konnte nicht gespeichert werden.";
            }catch(Exception e){return "Ungültige Wertauswahl.";}
        }
        @JavascriptInterface public String saveDisplayScale(int value){
            if(value<50||value>150)return "Bitte eine Darstellungsgröße zwischen 50 und 150 % wählen.";
            return repository.prefs.edit().putInt("display-scale-v1",value).commit()?"":"Darstellungsgröße konnte nicht gespeichert werden.";
        }
        @JavascriptInterface public String saveDashboard(String json) {
            try {
                if(json.length()>128000) throw new IllegalArgumentException("Die Übersicht ist zu groß.");
                JSONObject config=new JSONObject(json);
                if(config.getInt("version")!=1 || config.getJSONArray("tiles").length()>64) throw new IllegalArgumentException("Ungültige Übersicht.");
                if(!repository.prefs.edit().putString("dashboard-v1",config.toString()).commit()) throw new IOException();
                return "";
            } catch(Exception e) { return Repository.safeError(e); }
        }
        @JavascriptInterface public String loadHistory(long from,long to,String field,String device){return "{}";}
        @JavascriptInterface public String rollerEnergy(long from,long to){return "{}";}
        @JavascriptInterface public String dashboardHistory(long from,long to,String metric) {
            if(lan.viewer())return "{\"pending\":true}";
            if(repository.archiveBusy)return "{\"syncing\":true}";
            try { return solarPlanning.dashboardData(from,to,metric).toString(); }
            catch(Exception e) { return "{\"points\":[],\"summary\":{},\"error\":\"Zeitraum konnte nicht geladen werden.\"}"; }
        }
        @JavascriptInterface public String energyHistory(long from,long to,String source,long solarFrom) {
            if(lan.viewer())return "{\"pending\":true}";
            if(repository.archiveBusy)return "{\"syncing\":true}";
            try {return solarPlanning.energyFlow(from,to,solarFrom,source).toString();}
            catch(Exception e){return "{\"error\":\"Energieberechnung konnte nicht geladen werden.\"}";}
        }
        @JavascriptInterface public String energyHistoryWindow(long from,long to,String source,long solarFrom,long solarTo) {
            if(lan.viewer())return "{\"pending\":true}";
            if(repository.archiveBusy)return "{\"syncing\":true}";
            try{return solarPlanning.energyFlow(from,to,solarFrom,solarTo,source).toString();}
            catch(Exception e){return "{\"error\":\"Energieberechnung konnte nicht geladen werden.\"}";}
        }
        @JavascriptInterface public String dailyEnergy(long from,long to,long revision,long slot){
            if(lan.viewer())return "{\"days\":[],\"pending\":true}";
            try{return solarPlanning.dailyEnergy(from,to,new JSONObject(repository.prefs.getString("weatherCache","{}"))).toString();}
            catch(Exception e){return "{\"days\":[],\"error\":\"Tagesverbrauch konnte nicht geladen werden.\"}";}
        }
        @JavascriptInterface public void searchWeather(String query){files.execute(()->{try{call("onWeatherSearch",new JSONObject().put("query",query).put("places",WeatherClient.search(query)));}catch(Exception e){try{call("onWeatherSearch",new JSONObject().put("query",query).put("error","Ortssuche nicht möglich. Bitte Verbindung prüfen und erneut suchen."));}catch(JSONException ignored){}}});}
        @JavascriptInterface public String weatherPlace(String json){return localControl("weatherPlace",new JSONArray().put(json));}
        @JavascriptInterface public void refreshWeather(){if(!AppRole.viewerOnly(DisplayActivity.this)&&!lan.viewer()){WeatherClient.refresh(repository,true);SuppliesWeather.refresh(repository,false);}}
        @JavascriptInterface public String solarSetup(){if(lan.viewer())return lan.part("setup");try{return solarPlanning.setup().toString();}catch(Exception e){return "{}";}}
        @JavascriptInterface public String saveSolarSetup(String json,boolean newSite){return localControl("saveSolarSetup",new JSONArray().put(json).put(newSite));}
        @JavascriptInterface public String saveSolarSite(String json,String place){return localControl("saveSolarSite",new JSONArray().put(json).put(place));}
        @JavascriptInterface public void searchSolarPlaces(String query){files.execute(()->{try{call("onSolarPlaces",new JSONObject().put("query",query).put("places",WeatherClient.search(query)));}catch(Exception e){try{call("onSolarPlaces",new JSONObject().put("query",query).put("error","Ortssuche nicht möglich. Bitte erneut versuchen."));}catch(JSONException ignored){}}});}
        @JavascriptInterface public void rememberSolarContext(String key){if(!lan.viewer())solarPlanning.context(key);}
        @JavascriptInterface public void rememberSolarForecasts(String json){if(!lan.viewer())solarPlanning.remember(json);}
        private final java.util.concurrent.atomic.AtomicBoolean analysisBusy=new java.util.concurrent.atomic.AtomicBoolean();
        @JavascriptInterface public void requestSolarData(){if(!foreground)return;if(!lan.viewer()){solarPlanning.request();return;}if(!analysisBusy.compareAndSet(false,true))return;Object owner=lan.identity();historyReads.execute(()->{try{if(owner!=lan.identity()||!lan.viewer()||!foreground)return;JSONObject result=lan.analysisKnowledge(null);if(owner==lan.identity()&&lan.viewer())call("onSolarPlanData",result);}catch(Exception e){try{if(owner==lan.identity()&&lan.viewer())call("onSolarPlanData",new JSONObject().put("error",e instanceof java.io.IOException?e.getMessage():"Lerndaten gerade nicht verfügbar. Die letzte Vorschau bleibt erhalten."));}catch(JSONException ignored){}}finally{analysisBusy.set(false);}});}
        @JavascriptInterface public void startSolarCompass(String place){runOnUiThread(()->{try{siteSensors.startCompass(new JSONObject(place));}catch(Exception e){siteSensors.startCompass(new JSONObject());}});}
        @JavascriptInterface public void stopSolarCompass(){runOnUiThread(()->siteSensors.stopCompass());}
        @JavascriptInterface public void cancelSolarLocation(){runOnUiThread(()->{pendingSiteLocation=false;siteSensors.cancelLocation();});}
        @JavascriptInterface public void locateSolarSite(){runOnUiThread(()->{
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)siteSensors.locate();
            else {pendingSiteLocation=true;requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},SITE_PERMISSION);}
        });}
        @JavascriptInterface public String saveEquipment(String json){return localControl("saveEquipment",new JSONArray().put(json));}
        @JavascriptInterface public String copyArchiveDiagnostics(){try{String text=repository.diagnosticText();runOnUiThread(()->getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("AutarPie-Archivdiagnose",text)));return "";}catch(Exception e){return Repository.safeError(e);}}
        @JavascriptInterface public void importArchive(String mac) {requestArchive(mac);}
        @JavascriptInterface public void importMissingArchive(){requestArchive("MANUAL");}
        @JavascriptInterface public void cancelArchive(){if(!lan.viewer())runOnUiThread(()->startService(new Intent(DisplayActivity.this,ArchiveService.class).setAction("CANCEL")));}
        @JavascriptInterface public void start() { runOnUiThread(DisplayActivity.this::requestStart); }
        @JavascriptInterface public void stop() {
            if(lan.viewer())return;
            repository.prefs.edit().putBoolean("recording",false).apply();
            runOnUiThread(() -> stopService(new Intent(DisplayActivity.this,MonitorService.class)));
        }
        @JavascriptInterface public String saveDevice(String json) {return localControl("saveDevice",new JSONArray().put(json));}
        @JavascriptInterface public String removeDevice(String mac) {return localControl("removeDevice",new JSONArray().put(mac));}
        @JavascriptInterface public String settings(String json) {
            try { JSONObject s = new JSONObject(json);if(s.has("capacity")||s.has("sampleSeconds")){JSONObject shared=new JSONObject();for(String k:new String[]{"capacity","sampleSeconds"})if(s.has(k))shared.put(k,s.get(k));String failure=localControl("settings",new JSONArray().put(shared.toString()));if(!failure.isEmpty())return failure;} repository.saveSettings(s); runOnUiThread(() -> applyTheme(s.optString("theme",repository.prefs.getString("theme","dark")))); return ""; }
            catch (Exception e) { return Repository.safeError(e); }
        }
        @JavascriptInterface public String chooseSettingsExport(String json){
            try{JSONObject clean=ConfigurationBackup.normalize(json);pendingSettingsExport=clean.toString(2);if(pendingSettingsExport.getBytes(StandardCharsets.UTF_8).length>ConfigurationBackup.MAX_BYTES)pendingSettingsExport=clean.toString();
                runOnUiThread(()->{try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"AutarPie-Einstellungen.json"),EXPORT_SETTINGS);}catch(Exception e){pendingSettingsExport=null;error(e);}});return "";
            }catch(Exception e){pendingSettingsExport=null;return Repository.safeError(e);}
        }
        @JavascriptInterface public void chooseSettingsImport(){runOnUiThread(()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT_SETTINGS);}catch(Exception e){error(e);}});}
        @JavascriptInterface public String importSettings(String json){
            if(lan.viewer())return LanSync.READ_ONLY;
            try{lan.importLocalConfiguration(json,solarPlanning);runOnUiThread(()->applyTheme(repository.prefs.getString("theme","dark")));return "";}
            catch(Exception e){return Repository.safeError(e);}
        }
        @JavascriptInterface public void chooseImport() {
            if(lan.viewer())return;
            runOnUiThread(() -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT));
        }
        @JavascriptInterface public void chooseExport() {
            if(lan.viewer())return;
            runOnUiThread(() -> startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/csv").putExtra(Intent.EXTRA_TITLE,"AutarPie-Verlauf.csv"),EXPORT));
        }
        @JavascriptInterface public void choosePlanningDiagnostics(){
            if(lan.viewer())return;
            runOnUiThread(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/zip").putExtra(Intent.EXTRA_TITLE,"AutarPie-Diagnose-"+BuildConfig.VERSION_NAME+".zip"),EXPORT_DIAGNOSTICS));
        }
        @JavascriptInterface public void importCsv(String mapping, String kind) {
            if(lan.viewer())return;
            files.execute(() -> {
                try {
                    CsvCodec.Table table = pendingImport;
                    if (table == null) throw new IllegalArgumentException("Bitte zuerst eine CSV-Datei auswählen.");
                    JSONObject map = new JSONObject(mapping);
                    Map<String,Integer> fields = new LinkedHashMap<>();
                    for (String field : CsvCodec.FIELDS) {
                        int i = map.optInt(field,-1);
                        if (i >= 0 && i < table.headers.size()) fields.put(field,i);
                    }
                    if (!fields.containsKey("timestamp") && !fields.containsKey("date")) throw new IllegalArgumentException("Bitte die Datum-/Zeitspalte zuordnen.");
                    int[] counts = repository.db.importTable(table,fields,kind);
                    repository.invalidateStats(); pendingImport = null;
                    call("onImportDone",new JSONObject().put("inserted",counts[0]).put("duplicates",counts[1]).put("rejected",counts[2]));
                } catch (Exception e) { error(e); }
            });
        }
        @JavascriptInterface public void cancelImport() { files.execute(() -> pendingImport = null); }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("window.handleBack ? window.handleBack() : false",value -> { if (!"true".equals(value)) finish(); });
    }
    @Override protected void onDestroy() { if(siteSensors!=null)siteSensors.stop();if(solarPlanning!=null)solarPlanning.shutdown();historyReads.shutdownNow();files.shutdown(); if (web != null) { web.removeJavascriptInterface("WattPit"); web.destroy(); web = null; } super.onDestroy(); }
}
