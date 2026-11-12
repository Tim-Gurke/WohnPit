package de.wattpit.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

/** Launcher and role selection. The central path never constructs a WebView. */
public final class MainActivity extends Activity {
    private final java.util.concurrent.ExecutorService migration=java.util.concurrent.Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(MigrationBackup.pending(this)){
            LinearLayout progress=new LinearLayout(this);progress.setOrientation(LinearLayout.VERTICAL);progress.setPadding(32,40,32,40);progress.setBackgroundColor(0xff101822);progress.addView(new ProgressBar(this));text(progress,"Gesicherte Daten werden übernommen …",20);text(progress,"Bitte warten. Die bisherigen Daten bleiben zusätzlich gesichert. Danach startet AutarPie mit dem übernommenen Stand.",16);setContentView(progress);
            migration.execute(()->{try{MigrationBackup.applyPending(this);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())ready();});}catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())failure("Die Übernahme konnte nicht abgeschlossen werden. Die bisherigen Daten wurden gesichert. Bitte Speicherplatz prüfen und erneut öffnen.");});}});
        }else ready();
    }
    private void ready(){
        String role=AppRole.infer(this);
        if(!getIntent().getBooleanExtra("chooseRole",false)&&AppRole.valid(role)){
            try{if(!AppRole.get(this).equals(role))AppRole.save(this,role);launch(role);return;}catch(Exception e){failure("Die bisherige Betriebsart konnte nicht übernommen werden.");return;}
        }
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,36,32,36);root.setBackgroundColor(0xff101822);scroll.addView(root);setContentView(scroll);
        text(root,"AutarPie",30);text(root,"Energie, Gas und Wasser im Blick",20);text(root,"AutarPie vereint Watt’n’Pit und die bisherige AutarPie-Zentrale. Dein lokales Cockpit für Wohnwagen, Ferienhaus, Gartenhaus oder Balkonkraftwerk. Welche Aufgabe übernimmt dieses Handy?",16);
        button(root,"Zentrale","Geräte auslesen, Wetter sammeln und Daten für andere Handys speichern. Schlanke Ansicht für den Dauerbetrieb.","central");
        button(root,"Anzeigehandy","Eine Zentrale koppeln, Werte anzeigen, berechnen und die Anlage bedienen.","viewer");
        button(root,"Eigenständig · Victron direkt","Ohne Zentrale: Victron-Geräte direkt verbinden. Aufzeichnung, Wetter, Berechnung und Anzeige laufen auf diesem Handy.","direct");
        Button restore=new Button(this);restore.setText("Vollständige Übernahmesicherung einlesen");root.addView(restore);restore.setOnClickListener(v->startActivity(new Intent(this,MigrationActivity.class)));
        text(root,"Android 8+ · Lokal ohne Konto · © Tim Weißbach",13);
    }
    private void text(LinearLayout root,String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(Color.WHITE);text.setPadding(0,10,0,16);root.addView(text);}
    private void button(LinearLayout root,String label,String detail,String role){Button b=new Button(this);b.setText(label+"\n"+detail);b.setAllCaps(false);b.setPadding(16,18,16,18);root.addView(b);b.setOnClickListener(v->choose(role));}
    private void choose(String role){
        Repository repo=Repository.get(this);
        if(repo.running||repo.archiveBusy){new AlertDialog.Builder(this).setMessage("Bitte zuerst die Aufzeichnung pausieren und den Archivabruf beenden. Danach die Betriebsart ändern.").setPositiveButton("Verstanden",null).show();return;}
        Runnable apply=()->{try{stopService(new Intent(this,LanService.class));AppRole.save(this,role);launch(role);}catch(Exception e){failure("Die Betriebsart konnte nicht gespeichert werden.");}};
        if(AppRole.valid(AppRole.get(this))&&!AppRole.get(this).equals(role))new AlertDialog.Builder(this).setTitle("Betriebsart wechseln?").setMessage("Die aktuelle Verbindung wird beendet. Messdaten und Kacheln bleiben erhalten. Die Kopplung der bisherigen Rolle wird für die Rückkehr gespeichert. Eine neue Anzeige wird anschließend mit ihrer Zentrale gekoppelt.").setNegativeButton("Behalten",null).setPositiveButton("Wechseln",(d,w)->apply.run()).show();else apply.run();
    }
    private void launch(String role){startActivity(new Intent(this,role.equals("central")?CentralActivity.class:DisplayActivity.class).addFlags(getIntent().getBooleanExtra("chooseRole",false)?Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK:Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}
    private void failure(String message){new AlertDialog.Builder(this).setTitle("AutarPie").setMessage(message).setCancelable(false).setPositiveButton("Schließen",(d,w)->finish()).show();}
    @Override protected void onDestroy(){migration.shutdown();super.onDestroy();}
}
