const test=require('node:test'),assert=require('node:assert/strict'),C=require('../app/src/main/assets/supplies-core.js');
const now=Date.parse('2026-11-04T12:00:00Z'),D=C.DAY;
const event=(id,kind,type,at,amount,extra={})=>({id:'supply-'+id,kind,type,at,amount,empty:true,site:{key:'site-1'},...extra});
test('reach uses days and hours, singulars, and never decimal days',()=>{assert.equal(C.duration(41),'1 Tag 17 Stunden');assert.equal(C.duration(24),'1 Tag');assert.equal(C.duration(49),'2 Tage 1 Stunde');assert.equal(C.duration(.5),'Weniger als 1 Stunde');assert.equal(C.duration(null),'—');assert.equal(C.duration(0),'0 Stunden');assert.equal(C.duration(47.99),'1 Tag 23 Stunden');});
test('water purchases learn quantities after the first boundary, without inventing stock',()=>{const p=C.build({events:[event('0001','water','supply',now-10*D,40),event('0002','water','supply',now-5*D,20),event('0003','water','supply',now,30)]},now).water;assert.equal(p.daily,5);assert.equal(p.stock,null);assert.equal(p.remainingHours,null);assert.equal(p.needs[3],15);assert.equal(p.intervalHours,120);});
test('water procurement adds to an anchored stock instead of resetting it',()=>{const p=C.build({events:[event('0000','water','supply',now-8*D,20),event('0001','water','stock',now-3*D,12),event('0002','water','supply',now-2*D,12)]},now).water;assert.equal(p.daily,2);assert.equal(p.stock,18);assert.equal(p.remainingHours,216);});
test('gas interval consumes the previous bottle size, not the replacement size',()=>{const p=C.build({events:[event('0001','gas','swap',now-4*D,11),event('0002','gas','swap',now-2*D,5)]},now).gas;assert.equal(p.daily,5.5);assert.equal(p.stock,0);assert.equal(p.remainingHours,0);});
test('early bottle changes and intervening stock corrections do not train full-bottle consumption',()=>{const events=[event('0001','gas','swap',now-8*D,11),event('0002','gas','swap',now-6*D,11,{empty:false}),event('0003','gas','stock',now-5*D,8),event('0004','gas','swap',now-4*D,11),event('0005','gas','swap',now-2*D,11)];const p=C.build({events},now).gas;assert.equal(p.intervals,1);assert.equal(p.daily,5.5);});
test('one entry provides no made-up usage rate',()=>{for(const kind of ['gas','water']){const p=C.build({events:[event('0001',kind,kind==='gas'?'swap':'supply',now-2*D,11)]},now)[kind];assert.equal(p.daily,null);assert.equal(p.remainingHours,null);assert.equal(p.needs[3],undefined);}});
test('explicit zero stock is zero, missing stock is unknown',()=>{const events=[event('0001','water','supply',now-4*D,20),event('0002','water','supply',now-2*D,20),event('0003','water','stock',now,0)];assert.equal(C.build({events},now).water.stock,0);assert.equal(C.build({events},now).water.remainingHours,0);});
test('missing weather preserves the interval baseline and does not pretend weather was learned',()=>{const p=C.build({events:[event('0001','gas','swap',now-8*D,11),event('0002','gas','swap',now-4*D,11)]},now).gas;assert.equal(p.daily,2.75);assert.equal(p.model,null);assert.equal(p.needs[14],38.5);});
test('issued forecasts cannot enter historical weather training',()=>{const weather={history:[{site:'x',from:0,to:D,heat:10,windHeat:5,source:'forecast'}]};assert.deepEqual(C.historyRows(weather),[]);assert.equal(C.weatherFeatures([],0,D,'x'),null);});
test('archive reconstructions supersede recent model history without double counting',()=>{const base={site:'x',from:0,to:D,windHeat:5};const rows=C.historyRows({history:[{...base,source:'recent',heat:10},{...base,source:'archive',heat:20},{...base,source:'recent',heat:30}]});assert.equal(rows.length,1);assert.equal(C.weatherFeatures(rows,0,D,'x').heat,20);});
test('incomplete weather coverage is not silently filled',()=>{assert.equal(C.weatherFeatures([{site:'x',from:0,to:D/2,heat:10,windHeat:2}],0,D,'x'),null);});
test('location changes prevent training gas/weather relationships across the change',()=>{const events=[event('0001','gas','swap',now-4*D,11),event('0002','water','supply',now-3*D,20,{site:{key:'other'}}),event('0003','gas','swap',now-2*D,11)];assert.equal(C.intervals(events,'gas',now,{}).rows[0].features,null);});
test('temperature dependence requires enough varied, cross-validated cycles',()=>{
 const rows=Array.from({length:9},(_,i)=>{const heat=[2,12,7,17,4,14,9,19,6][i];return {days:3,amount:(.2+.08*heat)*3,rate:.2+.08*heat,features:{heat,windHeat:heat*.5}};});
 assert.equal(C.weatherModel(rows.slice(0,5)),null);const model=C.weatherModel(rows);assert.ok(model);assert.ok(Math.abs(model.coefficients[0]-.2)<.03);assert.ok(Math.abs(model.coefficients[1]-.08)<.005);
});
test('constant weather cannot identify a temperature model',()=>{const rows=Array.from({length:10},()=>({days:3,amount:3,rate:1,features:{heat:10,windHeat:5}}));assert.equal(C.weatherModel(rows),null);});
test('future and duplicate journal entries are handled explicitly',()=>{assert.throws(()=>C.normalize({events:[event('0001','water','supply',now,20),event('0001','water','supply',now,20)]}));assert.equal(C.build({events:[event('0001','gas','swap',now+D,11)]},now).gas.lastAt,null);});
test('old intervals age out without deleting the source journal',()=>{const source={events:[event('0001','water','supply',now-405*D,20),event('0002','water','supply',now-402*D,20)]};assert.equal(C.build(source,now).water.daily,null);assert.equal(source.events.length,2);});
test('new dashboard metrics work without Bluetooth readings and preserve existing layouts',()=>{const dashboard=require('../app/src/main/assets/dashboard-core.js'),tile=dashboard.tile('waterRange'),snapshot={settings:{capacity:100},suppliesAnalysis:{water:{remainingHours:41}}};assert.equal(dashboard.displayValue(tile,null,null,null,snapshot),41);assert.equal(dashboard.format(41,tile),'1 Tag 17 Stunden');assert.equal(dashboard.normalize({version:1,tiles:[{...tile,layouts:{compact:{x:2,y:5,w:4,h:4},wide:{x:8,y:1,w:3,h:3}}}],blockOrder:['tiles','history','record']}).tiles[0].layouts.compact.x,2);});
test('same-minute stock and procurement retain their recorded order',()=>{const events=[event('0001','water','supply',now-4*D,20),event('0002','water','supply',now-2*D,20),event('0003','water','stock',now,5),event('0004','water','supply',now,20)];assert.equal(C.build({events},now).water.stock,25);});
test('a fresh multi-day forecast influences the whole gas reach, including after the first 30 hours',()=>{
 const events=[],history=[],forecast=[],heats=[2,12,7,17,4,14,9,19,6],site={key:'site-1'};
 for(let i=0;i<9;i++){const from=now-(9-i)*2*D;events.push(event('weather-'+i,'gas','swap',from,(.2+.08*heats[i])*2,{site}));for(let d=0;d<2;d++)history.push({from:from+d*D,to:from+(d+1)*D,site:site.key,source:'archive',heat:heats[i],windHeat:heats[i]*.5});}
 events.push(event('weather-last','gas','swap',now,8,{site}));for(let d=0;d<15;d++)forecast.push({from:now+d*D,to:now+(d+1)*D,site:site.key,source:'forecast',heat:20,windHeat:10});
 const p=C.build({events,weather:{updated:now,siteKey:site.key,history,forecast}},now).gas;assert.ok(p.model);assert.ok(Math.abs(p.remainingHours-8/1.8*24)<2);assert.ok(Math.abs(p.needs[14]-1.8*14)<.3);
});
test('a configured location change and return inside one bottle cycle prevents false weather training',()=>{const events=[event('0001','gas','swap',now-4*D,11),event('0002','gas','swap',now-2*D,11)],sites=[{at:now-3.5*D,site:{key:'elsewhere'}},{at:now-2.5*D,site:{key:'site-1'}}];assert.equal(C.intervals(events,'gas',now,{},sites).rows[0].features,null);});

function weatherFixture(count=9,{wind=false,constantHeat=false,constantRate=false}={}){
 const events=[],history=[],forecast=[],heats=[2,12,7,17,4,14,9,19,6],winds=[12,1,25,4,20,2,28,6,15],site={key:'site-1'};
 for(let i=0;i<count;i++){
  const from=now-(count-i)*2*D,heat=constantHeat?10:heats[i],windHeat=heat*winds[i]/20,rate=constantRate?1:.2+.08*heat+(wind?.2*windHeat:0);
  events.push(event('status-'+i,'gas','swap',from,rate*2,{site}));
  for(let d=0;d<2;d++)history.push({from:from+d*D,to:from+(d+1)*D,site:site.key,source:'archive',heat,windHeat});
 }
 events.push(event('status-last','gas','swap',now,8,{site}));
 for(let d=0;d<15;d++)forecast.push({from:now+d*D,to:now+(d+1)*D,site:site.key,source:'forecast',heat:20,windHeat:10});
 return {events,weather:{updated:now,siteKey:site.key,history,forecast}};
}
test('loaded history alone reports learning progress rather than active weather correction',()=>{
 const p=C.build(weatherFixture(5),now).gas;
 assert.equal(p.weatherStatus.state,'learning');assert.equal(p.weatherStatus.weatherIntervals,5);assert.equal(p.weatherStatus.intervals,5);assert.match(p.weatherStatus.detail,/5 von mindestens 6/);assert.equal(p.weatherCorrected,false);
 const none=C.build({},now).gas;assert.equal(none.weatherStatus.state,'no-intervals');
 const missing=weatherFixture(6);missing.weather.history=[];const incomplete=C.build(missing,now).gas;
 assert.equal(incomplete.weatherStatus.weatherIntervals,0);assert.match(incomplete.weatherStatus.detail,/6 Abschnitte.*Wetterzuordnung/);
});
test('weather readiness explains similar temperatures and rejected weather models',()=>{
 assert.equal(C.build(weatherFixture(9,{constantHeat:true}),now).gas.weatherStatus.state,'similar-weather');
 assert.equal(C.build(weatherFixture(9,{constantRate:true}),now).gas.weatherStatus.state,'not-improved');
});
test('active temperature and wind status follows the model that actually changes the forecasts',()=>{
 const p=C.build(weatherFixture(6),now).gas;assert.equal(p.weatherStatus.state,'active');assert.equal(p.weatherStatus.model,'temperature');assert.equal(p.forecastWeatherCorrected,true);assert.equal(p.rangeWeatherCorrected,true);assert.notEqual(p.needs[3],p.daily*3);
 const wind=C.build(weatherFixture(9,{wind:true}),now).gas;assert.equal(wind.weatherStatus.model,'temperature-wind');assert.match(wind.weatherStatus.title,/Temperatur und Wind/);
});
test('disabled, stale, wrong-place and wrong-source forecasts preserve baseline needs and explain the reason',()=>{
 for(const [state,change] of [
  ['disabled',f=>f.weather.enabled=false],
  ['stale',f=>f.weather.updated=now-31*C.HOUR],
  ['stale',f=>delete f.weather.updated],
  ['missing-forecast',f=>f.weather.forecast=[]],
  ['missing-forecast',f=>f.weather.forecast.forEach(r=>r.site='elsewhere')],
  ['missing-forecast',f=>f.weather.forecast.forEach(r=>r.source='archive')]
 ]){
  const fixture=weatherFixture();change(fixture);const p=C.build(fixture,now).gas;
  assert.ok(p.model);assert.equal(p.weatherStatus.state,state);assert.equal(p.forecastWeatherCorrected,false);assert.equal(p.rangeWeatherCorrected,false);assert.equal(p.needs[3],p.daily*3);
 }
});
test('historical stock correction is distinguished from an unavailable future weather correction',()=>{
 const fixture=weatherFixture();fixture.events=fixture.events.filter(e=>e.at<now);fixture.events.push(event('status-stock','gas','stock',now-D,8));fixture.weather.updated=now-31*C.HOUR;
 const p=C.build(fixture,now).gas;assert.ok(p.model);assert.equal(p.historyWeatherCorrected,true);assert.equal(p.weatherCorrected,true);assert.equal(p.weatherStatus.state,'stale');assert.match(p.weatherStatus.detail,/bisherige Verbrauch.*Wetterrückblick/);assert.equal(p.forecastWeatherCorrected,false);
});
test('gas and water demand can be expressed in fractional containers without changing stock or learning',()=>{
 const source={defaults:{water:40,gas:11},events:[event('water-before','water','supply',now-4*D,20),event('water-after','water','supply',now-2*D,20),event('gas-before','gas','swap',now-4*D,11),event('gas-after','gas','swap',now-2*D,11)]};
 const before=structuredClone(source),analysis=C.build(source,now);
 assert.equal(analysis.water.containerSize,20,'a 40 L purchase default does not change the standard 20 L canister');assert.equal(analysis.gas.containerSize,11);
 assert.equal(C.quantity(30,'water','Kanister',analysis.water.containerSize),1.5);
 assert.ok(Math.abs(C.quantity(25.3,'gas','Flaschen',analysis.gas.containerSize)-2.3)<1e-12);
 assert.equal(C.quantity(25.3,'gas','kg',analysis.gas.containerSize),25.3);
 assert.equal(C.build({...source,defaults:{water:40,gas:5}},now).gas.containerSize,5);
 const smaller=C.build({...source,defaults:{water:40,gas:5,waterContainer:10}},now);
 assert.equal(smaller.water.containerSize,10);assert.equal(C.quantity(30,'water','Kanister',smaller.water.containerSize),3);
 for(const kind of ['gas','water'])for(const field of ['daily','stock','remainingHours'])assert.equal(smaller[kind][field],analysis[kind][field],'changing the container reference preserves '+kind+' '+field);
 for(const size of [0,-10,null,Infinity])assert.throws(()=>C.normalize({defaults:{waterContainer:size}}));
 assert.deepEqual(source,before);assert.deepEqual(C.build(source,now),analysis);
 assert.equal(C.containerHint('water',20),'1 Kanister = 20 L');assert.equal(C.containerHint('gas',11),'1 Flasche = 11 kg');
});
test('unknown quantities and missing container sizes never produce zero or an invented container count',()=>{
 for(const kind of ['gas','water']){
  const unit=C.quantityUnits(kind)[1];for(const missing of [null,undefined,NaN,Infinity])assert.equal(C.quantity(missing,kind,unit,20),null);
  for(const size of [null,undefined,0,-1,Infinity])assert.equal(C.quantity(30,kind,unit,size),null);
  assert.equal(C.quantity(0,kind,unit,20),0);
 }
});
test('fractional container sizes keep their actual reference instead of rounding the label',()=>{
 assert.equal(C.containerHint('gas',2.75),'1 Flasche = 2,75 kg');assert.equal(C.quantity(11,'gas','Flaschen',2.75),4);
 assert.equal(C.containerHint('water',12.5),'1 Kanister = 12,5 L');assert.equal(C.quantity(25,'water','Kanister',12.5),2);
});
test('all demand tiles and instrument scales convert with the current container size',()=>{
 const dashboard=require('../app/src/main/assets/dashboard-core.js'),snapshot={settings:{capacity:100},suppliesAnalysis:{gas:{containerSize:11,needs:{3:25.3,7:25.3,14:25.3}},water:{containerSize:20,needs:{3:30,7:30,14:30}}}};
 for(const kind of ['gas','water'])for(const horizon of [3,7,14]){
  const t=dashboard.tile(kind+'Need'+horizon),unit=kind==='gas'?'Flaschen':'Kanister';t.unit=unit;t.min=0;t.max=kind==='gas'?55:100;
  assert.ok(dashboard.CATALOG[t.metric].units.includes(unit));
  const base=dashboard.displayValue(t,null,null,null,snapshot),value=dashboard.convert(base,unit,t.metric,snapshot);
  assert.equal(dashboard.format(value,t),kind==='gas'?'2,3':'1,5');assert.equal(dashboard.fixedRange(t,snapshot).max,5);
  assert.ok(Math.abs(dashboard.unconvert(value,unit,t.metric,snapshot)-base)<1e-12);
  assert.equal(dashboard.convert(base,unit,t.metric,{}),null);
 }
 snapshot.suppliesAnalysis.gas.containerSize=5;assert.equal(dashboard.convert(25,'Flaschen','gasNeed14',snapshot),5);
 assert.deepEqual(dashboard.CATALOG.gasStock.units,['kg']);assert.deepEqual(dashboard.CATALOG.waterStock.units,['L']);
});
