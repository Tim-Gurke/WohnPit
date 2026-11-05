package de.wattpit.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Explicit user-controlled encrypted file transfer, independent of the ordinary portable settings. */
public final class MigrationActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView status;private Button action;private boolean exporting,busy;
    @Override public void onCreate(Bundle state){super.onCreate(state);exporting=getIntent().getBooleanExtra("export",false);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,32,28,32);setContentView(root);
        TextView title=new TextView(this);title.setText(exporting?"Vollständige Übernahmesicherung":"Übernahmesicherung einlesen");title.setTextSize(24);root.addView(title);
        status=new TextView(this);status.setTextSize(16);status.setPadding(0,24,0,24);status.setText(exporting?"Sichert Messwerte, Gas-/Wasserjournal, Wetter, Lernstände und Einstellungen einschließlich Geräte- und Kopplungsschlüsseln. Die Datei wird mit deinem Kennwort verschlüsselt und lokal gespeichert. Sie enthält private Zugangsdaten und gehört nicht auf öffentliche Seiten. Bitte zuerst Aufzeichnung und Archivabruf pausieren. Bewahre Datei und Kennwort für die Übernahme auf.":"Liest eine vollständige verschlüsselte Sicherung aus der Übernahmeversion. Zuerst werden Kennwort, Inhalt und Datenbanken geprüft. Die Übernahme ersetzt die Daten dieser Installation erst beim anschließenden Neustart. Die alte App bleibt erhalten. Aufzeichnung und Archivabruf müssen pausiert sein.");root.addView(status);
        action=new Button(this);action.setText(exporting?"Sicherungsdatei erstellen":"Sicherungsdatei wählen");root.addView(action);action.setOnClickListener(v->choose());
    }
    private void choose(){
        Repository repo=Repository.get(this);if(repo.running||repo.archiveBusy||repo.prefs.getBoolean("recording",false)){status.setText("Bitte zuerst Aufzeichnung und Archivabruf pausieren und danach erneut öffnen.");return;}
        Intent intent=new Intent(exporting?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_LOCAL_ONLY,true).setType("application/octet-stream");
        if(exporting)intent.putExtra(Intent.EXTRA_TITLE,"WohnPit-Uebernahme-"+BuildConfig.VERSION_NAME+".wohnpit");else intent.setType("*/*");startActivityForResult(intent,30);
    }
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==30&&result==RESULT_OK&&data!=null&&data.getData()!=null)password(data.getData());}
    private void password(Uri uri){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(30,10,30,10);EditText first=new EditText(this);first.setHint("Kennwort · mindestens 10 Zeichen");first.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);fields.addView(first);EditText second=new EditText(this);
        if(exporting){second.setHint("Kennwort wiederholen");second.setInputType(first.getInputType());fields.addView(second);}
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(exporting?"Sicherung verschlüsseln":"Sicherung entschlüsseln").setView(fields).setNegativeButton("Abbrechen",null).setPositiveButton(exporting?"Sichern":"Prüfen",null).create();dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=first.getText().toString();if(text.length()<10||exporting&&!text.equals(second.getText().toString())){first.setError(exporting?"Mindestens zehn Zeichen und übereinstimmende Kennwörter erforderlich.":"Bitte das Sicherungskennwort eingeben.");return;}char[] key=text.toCharArray();first.setText("");second.setText("");dialog.dismiss();run(uri,key);}));dialog.show();
    }
    private void run(Uri uri,char[] password){busy=true;action.setEnabled(false);status.setText(exporting?"Vollständige Sicherung wird verschlüsselt …":"Sicherung wird entschlüsselt und geprüft …");
        worker.execute(()->{try{
            if(exporting){try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Datei konnte nicht geöffnet werden.");MigrationBackup.export(this,out,password);}runOnUiThread(()->status.setText("Vollständige verschlüsselte Sicherung gespeichert. Die bisherigen App-Daten bleiben erhalten. Für die Übernahme dieselbe Datei und dasselbe Kennwort in der neuen WohnPit-App verwenden."));}
            else{JSONObject result;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Datei konnte nicht geöffnet werden.");result=MigrationBackup.prepare(this,in,password);}runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Sicherung geprüft").setMessage("Quelle: "+result.optString("sourceVersion")+". Kennwort, Sicherungsinhalt und Datenbanken wurden geprüft. WohnPit wird jetzt vollständig geschlossen. Öffne die App anschließend erneut; sie übernimmt dann die gesicherten Daten. Die bisherige Installation bleibt zusätzlich intern gesichert.").setCancelable(false).setPositiveButton("Schließen und neu öffnen",(d,w)->{getSharedPreferences("wattpit",0).edit().putBoolean("recording",false).commit();stopService(new Intent(this,MonitorService.class));stopService(new Intent(this,ArchiveService.class));stopService(new Intent(this,LanService.class));finishAffinity();android.os.Process.killProcess(android.os.Process.myPid());}).show());}
        }catch(Exception e){if(exporting)try{android.provider.DocumentsContract.deleteDocument(getContentResolver(),uri);}catch(Exception ignored){}runOnUiThread(()->status.setText("Übernahme nicht durchgeführt: "+(e.getMessage()==null?"Kennwort, Datei und freien Speicher prüfen.":e.getMessage())));}
        finally{Arrays.fill(password,'\0');runOnUiThread(()->{busy=false;action.setEnabled(true);});}});
    }
    @Override public void onBackPressed(){if(busy){status.setText("Bitte den laufenden Sicherungs- oder Prüfvorgang abwarten.");return;}super.onBackPressed();}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
