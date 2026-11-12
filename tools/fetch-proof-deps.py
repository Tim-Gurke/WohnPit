"""Fetch pinned public JVM proof dependencies from Maven Central, with bounded retries."""
from pathlib import Path
import argparse, time, urllib.request, zipfile
parser=argparse.ArgumentParser();parser.add_argument('directory',type=Path);args=parser.parse_args();args.directory.mkdir(parents=True,exist_ok=True)
files={
 'sqlite-jdbc-3.46.1.0.jar':'org/xerial/sqlite-jdbc/3.46.1.0/sqlite-jdbc-3.46.1.0.jar',
 'slf4j-api-1.7.36.jar':'org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar',
 'android-api35.jar':'org/robolectric/android-all/15-robolectric-12468137/android-all-15-robolectric-12468137.jar',
 'zxing-core.jar':'com/google/zxing/core/3.5.3/core-3.5.3.jar',
 'zxing-embedded.aar':'com/journeyapps/zxing-android-embedded/4.3.0/zxing-android-embedded-4.3.0.aar'}
for name,path in files.items():
 destination=args.directory/name;error=None
 for base in ['https://repo.maven.apache.org/maven2/','https://repo1.maven.org/maven2/']:
  for attempt in range(3):
   try:
    urllib.request.urlretrieve(base+path,destination)
    with zipfile.ZipFile(destination) as archive:
     if archive.testzip() is not None:raise ValueError('Invalid ZIP archive: '+name)
    error=None;break
   except Exception as failure:error=failure;time.sleep(min(2**attempt,4))
  if error is None:break
 if error is not None:raise RuntimeError('Unable to fetch pinned Maven artifact '+path) from error
 print('Fetched '+name,flush=True)
with zipfile.ZipFile(args.directory/'zxing-embedded.aar') as archive:(args.directory/'zxing-embedded.jar').write_bytes(archive.read('classes.jar'))
