const {test}=require('node:test'),assert=require('node:assert/strict'),A=require('../app/src/main/assets/autarpie-core.js'),D=require('../app/src/main/assets/dashboard-core.js'),E=require('../app/src/main/assets/equipment.js'),B=require('../app/src/main/assets/settings-backup.js');
test('workspace migration retains both orientations and creates independent custom copies',()=>{
 const legacy=D.defaults(),w=A.normalize({},legacy);assert.deepEqual(w.pages[0].board,legacy);const copy=A.createPage(w,'Winter',legacy,'custom-winter');copy.pages.at(-1).board.tiles[0].layouts.compact.x=9;assert.notEqual(w.pages[0].board.tiles[0].layouts.compact.x,9);assert.throws(()=>A.deletePage(w,'overview'));assert.equal(A.deletePage(copy,'custom-winter').pages.length,8);assert.deepEqual(A.normalize({navigation:[]}).navigation,[]);
});
test('area inheritance updates common colors while per-widget values and surfaces survive normalization',()=>{
 const a=A.appearance({colors:{solar:'#ef6000'},metricColors:{voltage:'#ffffff'}});assert.equal(A.metricColor(a,'solar',D.CATALOG.solar),'#ef6000');assert.equal(A.metricColor(a,'voltage',D.CATALOG.voltage),'#ffffff');assert.equal(A.metricColor(a,'gasStock',D.CATALOG.gasStock),a.colors.gas);
 const t=D.tile('soc','my-soc');t.colorSource='custom';t.color='#abcdef';t.backgroundSurface={mode:'gradient',first:'#112233',second:'#334455',angle:359};const n=D.normalize({version:1,tiles:[t]}).tiles[0];assert.deepEqual(n.backgroundSurface,t.backgroundSurface);assert.equal(n.color,'#abcdef');assert.equal(A.surfaceCss(n.backgroundSurface),'linear-gradient(359deg,#112233,#334455)');
});
test('smooth scale colors meet both neighbors without a hard threshold jump',()=>{
 const s=A.appearance().scales;assert.equal(A.scaleColor(s,0),s.colors[0]);assert.equal(A.scaleColor(s,100),s.colors[2]);assert.notEqual(A.scaleColor(s,20),s.colors[0]);assert.notEqual(A.scaleColor(s,20),s.colors[1]);assert.equal(A.scaleColor(s,30),s.colors[1]);assert.match(A.scaleColor({...s,count:2},20),/^#[0-9a-f]{6}$/);
});
test('charge permits require fresh BMS, reserve, configured surplus, stable start and explicit enable',()=>{
 const now=1000000,r={enabled:true,startW:150,stopW:100,delaySeconds:120},input={assignment:'scooter',at:now,houseSoc:80,scooterSoc:30,surplusW:200,forecastWh:1500};
 let d=A.chargeDecision(r,input,{},now);assert.equal(d.allow,false);assert.equal(d.since,now);d=A.chargeDecision(r,{...input,at:now+120001},{since:now},now+120001);assert.equal(d.allow,true);assert.equal(d.expires,now+180001);
 for(const changed of [{at:now-45001},{scooterSoc:85},{houseSoc:20},{scooterSoc:null},{forecastWh:null},{surplusW:120},{assignment:'model'}])assert.equal(A.chargeDecision(r,{...input,...changed},{since:now-200000},now).allow,false);
 assert.equal(A.chargeDecision(r,input,{manualOff:true,since:0},now).allow,false);assert.equal(A.chargeDecision({...r,enabled:false},input,{since:0},now).allow,false);
 assert.equal(A.chargeDecision(r,{...input,surplusW:120},{charging:true},now).allow,true);
});
test('shared 75/15 output clips 350 Wp together and follows battery voltage, independent regulators do not combine',()=>{
 const g=id=>({id,count:1,electrical:E.electrical({regulator:'mppt',maxChargeA:15},1)});const rows=[{g:g('first'),t:1,wh:200,factor:1},{g:g('third'),t:1,wh:150,factor:1}];let limited=E.limitContributions(rows,12);assert.equal(limited.reduce((s,r)=>s+r.wh,0),180);limited=E.limitContributions(rows,14);assert.equal(limited.reduce((s,r)=>s+r.wh,0),210);assert.equal(E.limitContributions(rows.map(r=>({...r,g:{...r.g,electrical:{...r.g.electrical,regulator:r.g.id}}})),14).reduce((s,r)=>s+r.wh,0),350);assert.throws(()=>E.electrical({series:2},3));
});
test('phase timeline never fabricates an average state or fills a recording gap',()=>{
 const p=A.phases([{t:1000,v:3},{t:20000,v:4},{t:90000,v:5}],0,100000);assert.deepEqual(p.map(x=>x.state),['unknown',3,4,'unknown',5]);assert.equal(p.reduce((s,x)=>s+x.to-x.from,0),100000);assert.equal(A.phases([],0,100)[0].state,'unknown');
});

test('live solar surplus excludes external charging and never exceeds measured solar',()=>{
 assert.equal(A.availableSurplus(500,0),0);assert.equal(A.availableSurplus(500,80),80);assert.equal(A.availableSurplus(-10,160,150),140);assert.equal(A.availableSurplus(100,160,0,140),-40);assert.equal(A.availableSurplus(null,160),null);
});
