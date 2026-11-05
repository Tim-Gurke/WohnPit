#!/usr/bin/env python3
"""Fail before source/release publication if private backup or signing files are tracked."""
from pathlib import Path
import subprocess,sys,zipfile

def private(name):
    name=name.lower().replace('\\','/')
    return (name.endswith(('.wohnpit','.p12','.jks','.keystore')) or name.rsplit('/',1)[-1]=='signing.json'
            or any(part.startswith('private-') for part in name.split('/'))
            or 'signatur-privat' in name or 'signierung-backup' in name)

root=Path(__file__).resolve().parents[1]
tracked=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
problems=[]
for name in filter(None,tracked):
    path=root/name
    if not path.is_file():continue
    if private(name):problems.append(name);continue
    with path.open('rb') as stream:
        if stream.read(24).startswith(b'WOHNPIT-MIGRATION-'):problems.append(name);continue
    if path.suffix.lower()=='.zip':
        try:
            with zipfile.ZipFile(path) as archive:
                if any(private(info.filename) for info in archive.infolist()):problems.append(name)
        except (zipfile.BadZipFile,RuntimeError):problems.append(name+' (unverifiable archive)')
if problems:
    print('STOP: private material must not be published:',*problems,sep='\n',file=sys.stderr)
    raise SystemExit(1)
print('PASS: no tracked migration backups or private signing files.')
