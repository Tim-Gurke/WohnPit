const {test}=require('node:test'),assert=require('node:assert/strict');
const X=require('../app/src/main/assets/special-use-core.js'),S=require('../app/src/main/assets/solar-core.js'),R=require('../app/src/main/assets/roller-core.js');
const H=3600000,D=24*H,start=Date.parse('2026-11-01T00:00:00Z'),now=start+5*H;
function day(offset=0){return {day:new Date(start+offset*D).toISOString().slice(0,10),sunset:start+offset*D+17*H,complete:true,spread:0,usage:{parts:[110,50,80],hours:[11,5,8],manual:true},timeline:Array.from({length:24},(_,i)=>({from:start+offset*D+i*H,to:start+offset*D+(i+1)*H,slot:S.part(i),wh:i>=8&&i<12?75:0}))};}
const profile=(more={})=>({id:'oven',kind:'device',name:'Minibackofen',mode:'manual',wh:200,minutes:60,hour:12,enabled:true,...more});
const config=p=>({version:1,profiles:[p],events:[]});
const roller={fresh:true,profile:{...R.defaults,mac:'AA:BB:CC:DD:EE:FF',planCharge:true},live:{soc:45},session:{active:false}};
test('separate extras preserve the normal battery result during BMS gaps',()=>{
 const d=day(),battery={soc:49,capacity:100,voltage:13.11,t:now},baseline=S.batteryForecast([d],{days:[d]},battery,now)[0].evening.soc;
 const on=X.build([d],{},roller,R,now),gap=X.build([d],{},{...roller,fresh:false},R,now);
 assert.ok(on.byDay.get(d.day).wh>800);assert.equal(gap.byDay.get(d.day).wh,null);assert.equal(gap.loads.length,0);assert.match(gap.byDay.get(d.day).reason,/BMS/);
 assert.equal(S.batteryForecast([d],{days:[d]},battery,now)[0].evening.soc,baseline);
 assert.ok(S.batteryForecast([d],{days:[d]},battery,now,on.loads)[0].evening.soc<baseline);
});
test('named device load uses Wh once and splits at midnight',()=>{
 const d=day(),tomorrow=day(1),p=profile(),c=config(p);c.events=[{id:'e',profileId:p.id,from:start+23.5*H,to:start+24.5*H,wh:400}];
 const plan=X.build([d,tomorrow],c,{},R,now);
 assert.equal(plan.byDay.get(d.day).wh,200);assert.equal(plan.byDay.get(tomorrow.day).wh,200);assert.equal(plan.loads.reduce((sum,l)=>sum+(l.to-l.from)/H*l.watts,0),400);
});
test('a single planned use replaces that day habit instead of adding it twice',()=>{
 const d=day(),p=profile(),c=config(p);c.events=[{id:'e',profileId:p.id,from:start+13*H,to:start+14*H,wh:350}];assert.equal(X.build([d],c,{},R,now).byDay.get(d.day).wh,350);
});
test('a disabled special use never changes the extras budget',()=>assert.equal(X.build([day()],config(profile({enabled:false})),{},R,now).loads.length,0));
test('manual power times duration and elapsed fractions retain consistent Wh',()=>{
 const d=day(),c=config(profile({wh:0,watts:800,minutes:30,hour:12}));assert.equal(X.build([d],c,{},R,now).byDay.get(d.day).wh,400);assert.equal(X.build([d],c,{},R,start+12.25*H).byDay.get(d.day).wh,200);
});
test('missing days never teach zero use; explicitly confirmed days can',()=>{
 const p=profile({mode:'learn'}),c=config(p);c.events=[{profileId:p.id,from:start-3*D,to:start-3*D+H,wh:300},{profileId:p.id,from:start-D,to:start-D+H,wh:300}];
 assert.equal(X.learned(p,c,{},'UTC',now).wh,null);c.events.push({profileId:p.id,from:start-2*D,to:start-2*D+H,wh:0});assert.equal(X.learned(p,c,{},'UTC',now).wh,200);
});
test('roller history requires observed days and rejects interrupted charge sessions',()=>{
 const p=profile({id:'roller',kind:'roller',mode:'learn'}),c=config(p),r={...roller,journal:[]};
 for(let i=1;i<=3;i++){const end=start-i*D+H,day=new Date(end).toISOString().slice(0,10);c.rollerDays??={};c.rollerDays[day]={mac:r.profile.mac,millis:21*H,unassignedMillis:0,wh:300};r.journal.push({started:end-H,ended:end,inputWh:300,observedMillis:3600000,gapMillis:0});}
 assert.equal(X.learned(p,c,r,'UTC',now).wh,300);r.journal[0].gapMillis=H;assert.equal(X.learned(p,c,r,'UTC',now).wh,null);
});
test('the 19 Wh per kilometre starting point includes charger and inverter losses once',()=>{
 const p=profile({id:'roller',kind:'roller',km:10,whPerKm:19}),r={...roller,profile:{...roller.profile,planCharge:false}};
 assert.ok(Math.abs(X.build([day()],config(p),r,R,now).byDay.get(day().day).wh-190*138/96)<1e-8);
});
test('a concrete charge replaces the usual roller demand and a reached target costs zero',()=>{
 const p=profile({id:'roller',kind:'roller',wh:999});assert.ok(X.build([day()],config(p),roller,R,now).byDay.get(day().day).wh<999);
 assert.equal(X.build([day()],config(p),{...roller,live:{soc:95}},R,now).byDay.get(day().day).wh,0);
});
test('invalid names, nonfinite energy and duplicate ids are rejected',()=>{
 for(const p of [profile({name:''}),profile({wh:NaN}),profile({hour:25})])assert.throws(()=>X.normalize(config(p)));
 assert.throws(()=>X.normalize({profiles:[profile(),profile()]}));
});

test('overnight device energy is assigned to both observed dates, never all to the starting day',()=>{
 const p=profile({mode:'learn'}),c=config(p);c.events=[{profileId:p.id,from:start-2*D-H,to:start-2*D+H,wh:200},{profileId:p.id,from:start-D,to:start-D+H,wh:100}];assert.equal(X.learned(p,c,{},'UTC',now).wh,100);
});
test('unassigned charging and another BMS cannot teach a false zero day',()=>{
 const p=profile({id:'roller',kind:'roller',mode:'learn'}),c=config(p),r={...roller};c.rollerDays={};for(let i=1;i<=3;i++)c.rollerDays[new Date(start-i*D).toISOString().slice(0,10)]={mac:r.profile.mac,millis:21*H,wh:0,unassignedMillis:0};assert.equal(X.learned(p,c,r,'UTC',now).wh,0);c.rollerDays['2026-10-31'].unassignedMillis=120000;assert.equal(X.learned(p,c,r,'UTC',now).wh,null);assert.equal(X.learned(p,c,{...r,profile:{mac:'OTHER'}},'UTC',now).wh,null);
});

test('explicit known roller days can teach usage even without automatic BMS coverage',()=>{
 const p=profile({id:'roller',kind:'roller',mode:'learn'}),c=config(p);for(let i=1;i<=3;i++)c.events.push({profileId:p.id,from:start-i*D,to:start-i*D+H,wh:200});assert.equal(X.learned(p,c,roller,'UTC',now).wh,200);
});
