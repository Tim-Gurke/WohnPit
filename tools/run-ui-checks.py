"""Run all existing browser component checks and collect every failure in one reviewable report."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import os,subprocess,json,time
names=['sync-coverage','energy-balance','energy-tile','tile-period-isolation','energy-refresh','viewer-statistics','gps-place','tile-detail','mini-history','reload-daily','charge-time','lifecycle','background-sync','solar-planning','special-use','supplies','value-colors','mobile-solar','display-layout','presence','roller','lan','plant-control','history-retention','display-pause','dashboard-usability','dashboard-contrast']
output=Path(os.environ.get('WATTPIT_SCREENSHOT_DIR','app/build/reports/ui-checks'));output.mkdir(parents=True,exist_ok=True)
def run(name):
 start=time.monotonic();directory=output/name;directory.mkdir(parents=True,exist_ok=True);env=dict(os.environ,WATTPIT_SCREENSHOT_DIR=str(directory))
 try:
  result=subprocess.run(['node','tools/'+name+'-smoke.cjs'],env=env,capture_output=True,text=True,timeout=180);code=result.returncode;log=result.stdout+result.stderr
 except subprocess.TimeoutExpired as e:code=124;log='Component check exceeded its 180-second limit.'
 (directory/'result.log').write_text(log);print(('PASS ' if code==0 else 'FAIL ')+name,flush=True)
 return {'name':name,'exit_code':code,'seconds':round(time.monotonic()-start,1),'log':str(directory/'result.log')}
with ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(run,names))
(output/'ui-checks.json').write_text(json.dumps(results,indent=2)+'\n')
for r in results:
 if r['exit_code']!=0:print('\n'+r['name']+' failure:\n'+Path(r['log']).read_text()[-5000:])
raise SystemExit(1 if any(r['exit_code'] for r in results) else 0)
