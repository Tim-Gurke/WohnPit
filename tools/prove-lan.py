"""Exercise production LAN host/client and SQL on the JVM; no Android radio/lifecycle test."""
import argparse
import json
import os
import re
from pathlib import Path
import subprocess

p=argparse.ArgumentParser()
p.add_argument('--output',type=Path,required=True)
p.add_argument('--android-jar',type=Path,required=True)
p.add_argument('--sqlite-jar',type=Path,required=True)
p.add_argument('--slf4j-jar',type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
generated=out/'generated';package=generated/'de/wattpit/app';package.mkdir(parents=True,exist_ok=True)
(package/'R.java').write_text('package de.wattpit.app; final class R { static final class drawable { static final int ic_bolt=1,ic_launcher_foreground=2; } }')
version=re.search(r"versionName '([^']+)'",(root/'app/build.gradle').read_text()).group(1)
(package/'BuildConfig.java').write_text('package de.wattpit.app; final class BuildConfig { static final String VERSION_NAME='+json.dumps(version)+'; }')
android=generated/'android/os';android.mkdir(parents=True,exist_ok=True)
(android/'SystemClock.java').write_text('package android.os; public final class SystemClock { public static long elapsedRealtime(){return System.nanoTime()/1000000;} public static long uptimeMillis(){return elapsedRealtime();} public static long elapsedRealtimeNanos(){return System.nanoTime();} }')
(android/'Build.java').write_text('package android.os; public final class Build { public static final String MODEL="JVM test display"; public static final class VERSION { public static final int SDK_INT=35; } }')
classes=out/'classes';classes.mkdir(exist_ok=True)
sources=list((root/'app/src/main/java').rglob('*.java'))+list((root/'tools/testing').rglob('*.java'))+list(generated.rglob('*.java'))+[root/'tools/LanSyncProof.java',root/'tools/PresenceProof.java',root/'tools/ViewerAnalysisProof.java',root/'tools/SolarInputFixture.java',root/'tools/MobileReplicaProof.java',root/'tools/MobileDisplayProof.java',root/'tools/MigrationProof.java',root/'tools/CentralArchiveProof.java']
native_cp=os.pathsep.join(map(str,[a.android_jar.resolve(),*a.android_jar.resolve().parent.glob('zxing-*.jar')]))
sources.append(root/'tools/LearningMigrationProof.java')
sources.append(root/'tools/ViewerRefreshProof.java')
sources.append(root/'tools/AutarPieConfigurationProof.java')
sources.append(root/'tools/CentralConcurrencyProof.java')
sources.append(root/'tools/LearningRetentionProof.java')
sources.append(root/'tools/RollerObservationProof.java')
sources.append(root/'tools/LearningRecoveryProof.java')
sources.append(root/'tools/HistoricalWeatherLearningProof.java')
subprocess.run(['java','com.sun.tools.javac.Main','-cp',native_cp,'-d',str(classes),*map(str,sources)],check=True)
classpath=os.pathsep.join(map(str,[classes,a.android_jar.resolve(),a.sqlite_jar.resolve(),a.slf4j_jar.resolve(),*a.android_jar.resolve().parent.glob('zxing-*.jar')]))
r=subprocess.run(['java','-cp',classpath,'de.wattpit.app.LanSyncProof',str(out/'devices')],capture_output=True,text=True,timeout=90)
print(r.stdout+r.stderr,end='');(out/'results.log').write_text(r.stdout+r.stderr)
(out/'report.json').write_text(json.dumps({'exit_code':r.returncode,'checks':[line[5:] for line in r.stdout.splitlines() if line.startswith('PASS ')],'scope':'Real production TCP host/client, AEAD and SQLite. Android clock, device model and Context are test adapters; physical Android services, NSD, WLAN and hotspots remain untested.'},indent=2)+'\n')
r.check_returncode()
r2=subprocess.run(['java','-cp',classpath,'de.wattpit.app.PresenceProof',str(out/'presence')],capture_output=True,text=True,timeout=30)
print(r2.stdout+r2.stderr,end='');(out/'presence-results.log').write_text(r2.stdout+r2.stderr)
r2.check_returncode()
r3=subprocess.run(['java','-Xmx512m','-cp',classpath,'de.wattpit.app.ViewerAnalysisProof',str(out/'replicas')],capture_output=True,text=True,timeout=240)
print(r3.stdout+r3.stderr,end='');(out/'viewer-results.log').write_text(r3.stdout+r3.stderr)
(out/'viewer-report.json').write_text(json.dumps({'exit_code':r3.returncode,'checks':[line[5:] for line in r3.stdout.splitlines() if line.startswith('PASS ')],'scope':'Actual encrypted TCP and SQLite, over 75,000 source rows, two independent native viewer replicas. Physical Android lifecycle, radios and handset CPU/RAM are not measured.'},indent=2)+'\n')
r3.check_returncode()
r4=subprocess.run(['java','-cp',classpath,'de.wattpit.app.MobileReplicaProof',str(out/'mobile-replicas')],capture_output=True,text=True,timeout=90)
print(r4.stdout+r4.stderr,end='');(out/'mobile-viewer-results.log').write_text(r4.stdout+r4.stderr);r4.check_returncode()
r5=subprocess.run(['java','-Xmx512m','-cp',classpath,'de.wattpit.app.MigrationProof',str(out/'migration')],capture_output=True,text=True,timeout=60)
print(r5.stdout+r5.stderr,end='');(out/'migration-results.log').write_text(r5.stdout+r5.stderr)
(out/'migration-report.json').write_text(json.dumps({'exit_code':r5.returncode,'checks':[line[5:] for line in r5.stdout.splitlines() if line.startswith('PASS ')],'scope':'Actual CSV and diagnosis export, separate SQLite databases and existing consumption calculations. No physical BMS or phone power measurement.'},indent=2)+'\n')
r5.check_returncode()
r6=subprocess.run(['java','-cp',classpath,'de.wattpit.app.CentralArchiveProof',str(out/'archive-cadence')],capture_output=True,text=True,timeout=30)
print(r6.stdout+r6.stderr,end='');(out/'archive-cadence-results.log').write_text(r6.stdout+r6.stderr);r6.check_returncode()
for attempt in range(3):
    fixture=out/('learning-migration' if attempt==0 else 'learning-migration-repeat-'+str(attempt+1))
    r7=subprocess.run(['java','-Xmx512m','-cp',classpath,'de.wattpit.app.LearningMigrationProof',str(fixture)],capture_output=True,text=True,timeout=120)
    print(r7.stdout+r7.stderr,end='');(out/('learning-migration-'+str(attempt+1)+'-results.log')).write_text(r7.stdout+r7.stderr);r7.check_returncode()
    r8=subprocess.run(['node',str(root/'tools/learning-state-check.cjs'),str(fixture/'learning-states.json')],capture_output=True,text=True,timeout=30)
    print(r8.stdout+r8.stderr,end='');(out/('learning-display-'+str(attempt+1)+'-results.log')).write_text(r8.stdout+r8.stderr);r8.check_returncode()
r9=subprocess.run(['java','-Xmx512m','-cp',classpath,'de.wattpit.app.ViewerRefreshProof',str(out/'statistics')],capture_output=True,text=True,timeout=90)
print(r9.stdout+r9.stderr,end='');(out/'statistics-results.log').write_text(r9.stdout+r9.stderr);r9.check_returncode()

r10=subprocess.run(['java','-cp',classpath,'de.wattpit.app.AutarPieConfigurationProof',str(out/'autarpie-config')],capture_output=True,text=True,timeout=30)
print(r10.stdout+r10.stderr,end='');(out/'autarpie-config-results.log').write_text(r10.stdout+r10.stderr);r10.check_returncode()

r11=subprocess.run(['java','-cp',classpath,'de.wattpit.app.CentralConcurrencyProof',str(out/'central-concurrency')],capture_output=True,text=True,timeout=20)
print(r11.stdout+r11.stderr,end='');(out/'central-concurrency-results.log').write_text(r11.stdout+r11.stderr);r11.check_returncode()

for name in ['LearningRetentionProof','RollerObservationProof','LearningRecoveryProof','HistoricalWeatherLearningProof']:
    proof=subprocess.run(['java','-Xmx512m','-cp',classpath,'de.wattpit.app.'+name,str(out/name)],capture_output=True,text=True,timeout=120)
    print(proof.stdout+proof.stderr,end='');(out/(name+'-results.log')).write_text(proof.stdout+proof.stderr);proof.check_returncode()
