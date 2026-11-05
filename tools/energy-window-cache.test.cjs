const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const C=require('../app/src/main/assets/dashboard-core.js'),Bridge=require('../app/src/main/assets/read-bridge.js');
// The reported midnight and daylight windows belong to the same local date.
process.env.TZ='Europe/Berlin';
const context={window:{WattDashboardCore:C},document:{getElementById:()=>null},Date,Intl};
vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,'../app/src/main/assets/dashboard.js'),'utf8'),context);
const Dashboard=context.window.WattDashboard;
const day=Date.parse('2026-11-03T23:00:00Z'),rise=day+8.5*3600000,set=day+18*3600000,now=day+19.25*3600000;
function fixture(){
 const requests=[],bridge=new Bridge({isLanViewer:()=>true,lanState:()=>'{"id":"window-test"}',requestHistory:q=>requests.push(JSON.parse(q))},()=>{});
 const dashboard=Object.create(Dashboard.prototype);dashboard.cache=new Map();dashboard.a={isDemo:()=>false,isBlocked:()=>false,native:{energyHistoryWindow:()=>{}},read:(method,args,scope)=>bridge.read(method,args,scope)};
 const tiles=Object.fromEntries(['solarEnergy','mobileSolarEnergy','consumptionEnergy','energyBalance'].map(metric=>[metric,C.tile(metric,metric)]));
 const period={...tiles.solarEnergy.period,mode:'sunrise'};for(const t of Object.values(tiles))t.period={...period};
 let answered=0;
 return {requests,bridge,dashboard,tiles,read:(metric,from,to,scope)=>dashboard.data(tiles[metric],{from,to},rise,set,scope),
  answer(delta=0){const q=requests[answered++];assert.ok(q,'an actual asynchronous query must exist');const daylight=q.args[0]===rise,total={solar:39.4,mobile:(daylight?42.84:48.12)+delta,balance:daylight?-20:-25.22};total.consumption=total.solar+total.mobile-total.balance;
   const data={accountingVersion:2,analysis:{revision:7,tables:{model_weather:'unchanged'}}};for(const [kind,last] of Object.entries(total))data[kind]={points:[],summary:{last,from:q.args[0],to:q.args[1],coverage:1},analysis:data.analysis};
   bridge.result({id:q.id,data});dashboard.cache.clear();
  },close(){bridge.setActive(false);clearTimeout(bridge.timer);}};
}
for(const detail of [false,true])test(`unchanged full-day and sunrise flow windows settle independently${detail?' with an explicit comparison scope':''}`,()=>{
 const f=fixture(),scope=detail?'detail-comparison':undefined;
 // The detail period is independent of the original tile's stored period.
 if(detail)f.tiles.solarEnergy.period.mode='today';
 try{
  f.read('solarEnergy',day,now,scope);f.answer();
  const pending=f.read('mobileSolarEnergy',rise,set,scope);assert.equal(pending.summary?.last,undefined,'a cold sunrise tile must not borrow the full-day mobile total');f.answer();
  for(let i=0;i<12;i++){
   assert.equal(f.read('solarEnergy',day,now,scope).summary.last,39.4);
   const mobile=f.read('mobileSolarEnergy',rise,set,scope);assert.equal(mobile.summary.last,42.84);assert.equal(mobile.summary.from,rise);assert.equal(mobile.pending,undefined);f.dashboard.cache.clear();
  }
  assert.equal(f.requests.length,2,'complete responses must not repeatedly evict and re-request each other');
 }finally{f.close();}
});
test('identical Today flow windows still share one complete energy response',()=>{
 const f=fixture();try{
  for(const t of Object.values(f.tiles))t.period.mode='today';
  f.read('solarEnergy',day,now);f.answer();
  assert.equal(f.read('mobileSolarEnergy',day,now).summary.last,48.12);
  assert.ok(Math.abs(f.read('consumptionEnergy',day,now).summary.last-112.74)<1e-9);
  assert.equal(f.read('energyBalance',day,now).summary.last,-25.22);assert.equal(f.requests.length,1);
 }finally{f.close();}
});
test('a revised estimate retains only its own window while pending and can legitimately change',()=>{
 const f=fixture();try{
  f.read('solarEnergy',day,now);f.answer();f.read('mobileSolarEnergy',rise,set);f.answer();
  f.bridge.invalidate();f.dashboard.cache.clear();
  const stale=f.read('mobileSolarEnergy',rise,set);assert.equal(stale.summary.last,42.84);assert.equal(stale.summary.from,rise);assert.equal(stale.pending,true);assert.equal(stale.stale,true);
  f.answer(2);const updated=f.read('mobileSolarEnergy',rise,set);assert.equal(updated.summary.last,44.84);assert.equal(updated.pending,undefined);
  const solar=f.read('solarEnergy',day,now);assert.equal(solar.summary.from,day);assert.equal(solar.pending,true);f.answer(2);
  assert.equal(f.read('mobileSolarEnergy',rise,set).summary.last,44.84);assert.equal(f.requests.length,4);
 }finally{f.close();}
});
