const test=require('node:test'),assert=require('node:assert/strict');
const U=require('../app/src/main/assets/update-core.js'),D=require('../app/src/main/assets/dashboard-core.js'),A=require('../app/src/main/assets/autarpie-core.js'),M=require('../app/src/main/assets/metrics-core.js'),{Outbox}=require('../app/src/main/assets/supply-outbox.js');
test('formulas accept German decimals, precedence, conditions and explicit factors without executing code',()=>{
 assert.equal(U.formula('=A/B*100',{A:120,B:150}),80);assert.equal(U.formula('=2,5*(A+3)^2',{A:1}),40);
 assert.equal(U.formula('=WENN(B>0;A/B*100;0)',{A:12,B:0}),0);assert.equal(U.formula('=RUNDEN(MAX(A;BETRAG(B));2)',{A:1.234,B:-2.345}),2.35);
 assert.equal(U.calculate({expression:'=A+B',variables:{A:{metric:'power'},B:{metric:'solar'}},factor:2,offset:3},()=>({value:5,at:100})).value,23);
 for(const code of ['globalThis.alert(1)','constructor(A)','A[0]','A;B','(A+1','A/0'])assert.throws(()=>U.formula(code,{A:1}));
 assert.throws(()=>U.formula('A+B',{A:1}),/fehlt/);assert.throws(()=>U.calculate({expression:'A+B',variables:{A:{metric:'power'},B:{metric:'solar'}}},s=>({value:2,at:s.metric==='power'?100:20000})),/Zeitpunkten/);
 assert.equal(U.formula('A/(B-1)',{},true),true,'syntax validation must not substitute imaginary measurements');
});
test('partial restore changes only colors or positions and matches stable tile and dashboard identities',()=>{
 const t=D.tile('soc','soc-tile'),current={appearance:A.appearance({accent:'#112233'}),ownColors:['#112233'],dashboard:D.normalize({version:1,tiles:[t],blockOrder:['tiles','history','record'],lanTileVersion:1}),workspace:null};current.workspace=A.normalize({},current.dashboard);
 const saved=structuredClone(current);saved.workspace.pages[0].board.tiles[0].layouts.compact.x=3;saved.workspace.pages[0].board.tiles[0].colorSource='custom';saved.workspace.pages[0].board.tiles[0].color='#aabbcc';saved.appearance.accent='#aabbcc';
 const colors=U.mergeView(current,saved,['colors']);assert.equal(colors.workspace.pages[0].board.tiles[0].color,'#aabbcc');assert.deepEqual(colors.workspace.pages[0].board.tiles[0].layouts,t.layouts);
 const layout=U.mergeView(current,saved,['layout']);assert.equal(layout.workspace.pages[0].board.tiles[0].layouts.compact.x,3);assert.equal(layout.appearance.accent,'#112233');assert.equal(layout.workspace.pages[0].board.tiles[0].color,t.color);
 assert.deepEqual(current.workspace.pages[0].board.tiles[0].layouts,t.layouts);assert.deepEqual(U.mergeView(current,saved,Object.keys(U.VIEW_PARTS)),saved);
});
test('dashboard color inheritance stays optional and does not replace individual tile settings',()=>{
 const base=A.appearance({}),page={design:{enabled:true,app:{first:'#112233',second:'#334455',mode:'solid',angle:0},value:'#aabbcc'}};
 assert.equal(A.pageAppearance(base,page).colors.scooter,'#aabbcc');assert.equal(A.pageAppearance(base,page).app.first,'#112233');assert.equal(A.pageAppearance(base,{design:{...page.design,enabled:false}}).colors.scooter,base.colors.scooter);
 const w=A.normalize({pages:[{id:'overview',name:'Übersicht',board:D.defaults(),design:page.design}]});assert.equal(w.pages[0].design.app.first,'#112233');
});
function snapshot(){const now=Date.now();return {now,settings:{capacity:100,solarSetup:{groups:{}}},devices:[{mac:'solar-1',kind:'solar'}],readings:[{device:'solar-1',kind:'solar',t:now,current:2,load:8,voltage:13}],equipment:{groups:[]},roller:{fresh:true,profile:{capacityAh:26,nominalVoltage:48,whPerKm:19,reservePercent:20,targetPercent:90},live:{at:now,remainingAh:13,fullAh:26,soc:50,current:2,power:96,cells:[3.7,3.72,3.71],cellsAt:now}},energy:{config:{assignment:'scooter',exclusiveScooter:true},output:{at:now,state:1,current:10,voltage:12}}};}
test('new scooter and regulator tiles use actual data and retain their additional settings',()=>{
 const s=snapshot(),read=(metric,extra={})=>M.read({metric,...extra},s,null,null,()=>null,s.now);
 assert.equal(read('solarTotalCurrent').value,10);assert.equal(read('rollerRange').value,624/19);assert.equal(read('rollerRange',{rangeReserve:true}).value,(624-249.6)/19);
 assert.equal(read('rollerCellVoltage',{cell:2}).value,3.72);assert.equal(read('rollerCellMin').value,3.7);assert.ok(Math.abs(read('rollerCellDelta').value-.02)<1e-9);
 assert.equal(read('rollerEfficiency').value,80);assert.equal(read('rollerLossPower').value,24);
 const tile=D.tile('formula','test-formula');tile.formula={expression:'=A/B*100',factor:1,offset:0,variables:{A:{metric:'rollerPower'},B:{metric:'loadPower'}}};tile.unit='%';tile.device='solar-1';tile.cell=2;tile.rangeReserve=true;
 const out=D.normalize({version:1,tiles:[tile],blockOrder:['tiles','history','record'],lanTileVersion:1}).tiles[0];assert.equal(out.formula.expression,'=A/B*100');assert.equal(out.unit,'%');assert.equal(out.device,'solar-1');assert.equal(out.cell,2);assert.equal(out.rangeReserve,true);
});
test('missing, old, unassigned and inconsistent measurements never become a fabricated efficiency',()=>{
 const s=snapshot(),read=()=>M.read({metric:'rollerEfficiency'},s,null,null,()=>null,s.now);
 s.energy.config.exclusiveScooter=false;assert.equal(read().value,null);s.energy.config.exclusiveScooter=true;s.energy.output.at-=11000;assert.equal(read().value,null);s.energy.output.at=s.now;s.roller.live.power=150;assert.equal(read().value,null);
 s.roller.fresh=false;assert.equal(M.read({metric:'rollerRange'},s,null,null,()=>null,s.now).value,null);s.roller.fresh=true;delete s.roller.live.cells;assert.equal(M.read({metric:'rollerCellVoltage'},s,null,null,()=>null,s.now).value,null);
 delete s.readings[0].load;assert.equal(M.read({metric:'solarTotalCurrent'},s,null,null,()=>null,s.now).value,null);
});
test('offline supply entries survive restart, stay bound to their plant and await explicit acknowledgement',()=>{
 const map=new Map(),storage={getItem:k=>map.get(k),setItem:(k,v)=>map.set(k,v)},a=new Outbox(storage),command={op:'add',event:{id:'water-entry-01',kind:'water',type:'supply',amount:20,at:12345,note:'Beispiel'}};
 a.enqueue('plant-a',command,'operation-001');const b=new Outbox(storage),config={events:[],defaults:{water:20,gas:11}};
 assert.equal(b.overlay(config,'plant-a').events[0].at,12345);assert.equal(b.overlay(config,'plant-a').events[0].pendingSync,true);assert.equal(b.overlay(config,'plant-b').events.length,0);assert.equal(config.events.length,0);
 b.update('operation-001',{state:'ack'});assert.equal(b.read().length,1);assert.equal(b.overlay({...config,events:[command.event]},'plant-a').events.length,1);assert.equal(b.read().length,0);
 storage.setItem(b.key,'broken');assert.throws(()=>b.read(),/nicht überschrieben/);assert.equal(storage.getItem(b.key),'broken');
});
test('source switches within the supported sampling interval stay connected while explicit data gaps remain',()=>{
 assert.equal(D.seriesGap({t:1000,device:'a',origin:'live',period:30},{t:30000,device:'a',origin:'archive',period:30}),false);
 assert.equal(D.seriesGap({t:1000,device:'a'},{t:2000,device:'a',gap:true}),true);assert.equal(D.seriesGap({t:1000,device:'a'},{t:2000,device:'b',gap:false}),true);
});
test('formula curves align the same measurement times, retain gaps and never interpolate missing values as zero',()=>{
 const config={expression:'=A/B*100',variables:{A:{metric:'rollerPower'},B:{metric:'loadPower'}}};
 const p=(t,v,extra={})=>({t,v,period:2,device:'same',...extra});
 const aligned=U.formulaSeries(config,{A:{points:[p(0,80),p(2000,100),p(4000,120)]},B:{points:[p(0,100),p(4000,150)]}});
 assert.deepEqual(aligned.points.map(p=>p.v),[80,80,80]);
 const missing=U.formulaSeries(config,{A:{points:[p(0,80),p(2000,100),p(4000,120)]},B:{points:[p(0,null),p(4000,150)]}});
 assert.deepEqual(missing.points.map(p=>p.t),[4000]);
 const gap=U.formulaSeries(config,{A:{points:[p(0,80),p(2000,100),p(4000,120,{gap:true})]},B:{points:[p(0,100),p(4000,150)]}});
 assert.equal(gap.points.at(-1).gap,true);
 const mismatch=U.formulaSeries(config,{A:{points:[p(0,80),p(4000,120,{device:'other'})]},B:{points:[p(0,100),p(2000,125),p(4000,150)]}});
 assert.deepEqual(mismatch.points.map(p=>p.t),[0,4000]);assert.equal(mismatch.points.at(-1).gap,true);
});
test('missing selected regulators remain unknown and percentage results are not rescaled by their display unit',()=>{
 const s=snapshot();assert.equal(M.read({metric:'loadVoltage',device:'removed'},s,null,null,()=>null,s.now).value,null);
 const tile=D.tile('formula');tile.formula={expression:'=A/B*100',variables:{A:{metric:'rollerPower'},B:{metric:'loadPower'}}};tile.unit='%';
 assert.equal(D.convert(80,'%',tile.metric,s),80);assert.equal(M.read(tile,s,null,null,source=>({value:source.metric==='rollerPower'?96:120,at:s.now}),s.now).value,80);
 assert.equal(D.format(null,D.tile('loadState')),'—');
});
