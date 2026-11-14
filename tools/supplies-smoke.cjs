const autarpieControls=require('./autarpie-test-controls.cjs');
/* Actual shipped UI with a delayed, acknowledged data-host fixture; fictional data only. */
const waitForPage=require('./browser-wait.cjs');
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})});
 try{
  const page=await browser.newPage({viewport:{width:950,height:1050},deviceScaleFactor:1.5,timezoneId:'Europe/Berlin',locale:'de-DE'}),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.clock.setFixedTime(new Date('2026-11-04T12:00:00Z'));
  await page.addInitScript(()=>{
   const now=Date.now(),D=86400000,site={name:'Beispielort',latitude:50,longitude:10,key:'50000:10000'},event=(id,kind,type,at,amount)=>({id:'example-'+id,kind,type,at,amount,empty:true,site});
   window.suppliesFixture={version:1,protocol:1,containerSizeSettings:true,revision:1,defaults:{water:20,gas:11},events:[event('water-01','water','supply',now-12*D,20),event('water-02','water','supply',now-8*D,20),event('water-03','water','supply',now-4*D,20),event('gas-01','gas','swap',now-9*D,11),event('gas-02','gas','swap',now-5*D,11),event('gas-03','gas','swap',now-D,11),event('stock-water','water','stock',now,12),event('stock-gas','gas','stock',now-7200000,5)],weather:{updated:now,siteKey:site.key,place:site,history:[],forecast:[]}};
   window.plantRevision=4;window.online=true;window.writes=0;window.testBoard={};
   window.WattPit={appVersion:()=>'2.0.2',isLanViewer:()=>true,controlRevision:()=>window.plantRevision,lanState:()=>JSON.stringify({role:'viewer',id:'example-plant',name:'WohnPit · Beispiel',connected:window.online}),dashboard:()=>JSON.stringify(window.testBoard),saveDashboard:json=>{window.testBoard=JSON.parse(json);return '';},settings:()=>'',solarSetup:()=>'{}',rollerSnapshot:()=>'{}',
    syncState:()=>JSON.stringify({busy:false,runId:0,cockpitNoticeAccepted:true,theme:window.testTheme||'dark',palette:'classic'}),
    snapshot:()=>JSON.stringify({lan:{role:'viewer',id:'example-plant',connected:window.online},controlRevision:window.plantRevision,running:false,status:'Beispieldaten',devices:[],readings:[],found:[],stats:{count:0},settings:{theme:window.testTheme||'dark',capacity:100,sampleSeconds:10,weatherPlace:site}}),
    suppliesSnapshot:()=>JSON.stringify(window.suppliesFixture),saveSupplies:()=>{throw Error('Viewer must use acknowledged controls');},refreshSuppliesWeather:()=>{},refreshLan:()=>{},
    requestPlantCommand:json=>{const command=JSON.parse(json);setTimeout(()=>{
      if(command.revision!==window.plantRevision){window.onPlantResult({id:command.id,error:'Daten wurden inzwischen geändert. Bitte neu öffnen.'});return;}
      const config=window.suppliesFixture;if(command.action==='saveSupplies'){const value=JSON.parse(command.args[0]);if(value.op==='defaults')config.defaults={water:value.water,gas:value.gas,waterContainer:value.waterContainer??config.defaults.waterContainer??20};else if(value.op==='delete')config.events=config.events.filter(e=>e.id!==value.id);else{const index=config.events.findIndex(e=>e.id===value.event.id);if(index>=0)config.events[index]=value.event;else config.events.push(value.event);}config.revision++;window.writes++;}window.plantRevision++;window.onPlantResult({id:command.id,ok:true});
     },400);}
   };
  });
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await autarpieControls.prepare(page);await page.locator('[data-page="supplies"]').click();
  assert.match(await page.locator('#supplies-gas .supply-range').textContent(),/1 Tag 17 Stunden/);assert.match(await page.locator('#supplies-water').textContent(),/12 L verbleibend/);
  assert.equal(await page.locator('.bottom-nav [data-page="supplies"] svg path').count(),2);assert.equal(await page.locator('.supply-symbol svg').count(),2);
  assert.match(await page.locator('#supplies-gas .supply-weather').textContent(),/Wetterkorrektur noch nicht aktiv/);
  assert.match(await page.locator('#supplies-gas .supply-weather').textContent(),/0 von 2 abgeschlossenen Abschnitten/);
  assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);
  const out=process.env.WATTPIT_SUPPLIES_SCREENSHOTS||path.resolve(__dirname,'../docs/images');fs.mkdirSync(out,{recursive:true});
  const originalJournal=await page.evaluate(()=>JSON.stringify(window.suppliesFixture.events));
  await page.locator('[data-supply-unit="gas"]').selectOption('Flaschen');await page.locator('[data-supply-unit="water"]').selectOption('Kanister');
  assert.match(await page.locator('[data-supply-needs="gas"]').textContent(),/3,5 Flaschen/);assert.match(await page.locator('[data-supply-needs="water"]').textContent(),/3,5 Kanister/);
  assert.match(await page.locator('#supplies-water .supply-unit-setting').textContent(),/1 Kanister = 20 L/);assert.match(await page.locator('#supplies-gas .supply-unit-setting').textContent(),/1 Flasche = 11 kg/);
  assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('autarpie-workspace-v1')).pages.find(p=>p.id==='supplies').board.supplyUnits),{water:'Kanister',gas:'Flaschen'});assert.equal(await page.evaluate(()=>window.writes),0);assert.equal(await page.evaluate(()=>JSON.stringify(window.suppliesFixture.events)),originalJournal);
  await page.evaluate(()=>document.querySelector('.bottom-nav').hidden=true);await page.locator('#supplies-water').screenshot({path:path.join(out,'supplies-water-2.0.2-dark.png'),animations:'disabled'});await page.evaluate(()=>{window.testTheme='light';poll();});await page.locator('#supplies-water').screenshot({path:path.join(out,'supplies-water-2.0.2-light.png'),animations:'disabled'});await page.evaluate(()=>{window.testTheme='dark';poll();document.querySelector('.bottom-nav').hidden=false;});
  await page.locator('[data-supply-unit="gas"]').selectOption('kg');await page.locator('[data-supply-unit="water"]').selectOption('L');
  await page.screenshot({path:path.join(out,'supplies-2.0.2-dark.png'),animations:'disabled'});
  await page.evaluate(()=>{window.testTheme='light';poll();});await page.screenshot({path:path.join(out,'supplies-2.0.2-light.png'),animations:'disabled'});await page.evaluate(()=>{window.testTheme='dark';poll();});
  await page.setViewportSize({width:390,height:900});await page.locator('#suppliesWater').click();assert.equal(await page.locator('#supplyAmount').inputValue(),'20');
  await page.screenshot({path:path.join(out,'supplies-water-entry-2.0.2.png'),animations:'disabled'});
  const before=await page.locator('.supply-event').count();await page.locator('#supplySave').click();
  assert.equal(await page.evaluate(()=>window.writes),0,'success must wait for the host');await waitForPage(page,()=>window.writes===1);await waitForPage(page,()=>document.getElementById('modal').hidden);assert.equal(await page.locator('.supply-event').count(),before+1);
  await page.locator('#suppliesGas').click();assert.equal(await page.locator('#supplyAmount').inputValue(),'11');await page.locator('#supplyEmpty').uncheck();await page.locator('#supplySave').click();await waitForPage(page,()=>window.writes===2);await waitForPage(page,()=>document.getElementById('modal').hidden);assert.match(await page.locator('#suppliesJournal').textContent(),/noch nicht leer/);
  await page.locator('[data-supply-edit]').first().click();await page.locator('#supplyDelete').click();await page.locator('#supplyCancel').click();assert.equal(await page.evaluate(()=>window.writes),2,'cancel deletion preserves journal');await page.locator('#supplyDelete').click();await page.locator('#supplySave').click();await waitForPage(page,()=>window.writes===3);await waitForPage(page,()=>document.getElementById('modal').hidden);
  await page.locator('#suppliesSettings').click();await page.locator('#supplyWaterDefault').fill('25');await page.locator('#supplySave').click();await waitForPage(page,()=>window.writes===4);await waitForPage(page,()=>document.getElementById('modal').hidden);await page.locator('#suppliesWater').click();assert.equal(await page.locator('#supplyAmount').inputValue(),'25');await page.locator('#supplyCancel').click();
  await page.locator('[data-page="overview"]').click();const layouts=await page.evaluate(()=>dashboard.config.tiles.map(t=>({id:t.id,layouts:t.layouts})));
  await page.evaluate(()=>document.getElementById('dashboardToolbar').hidden=false);await autarpieControls.addTile(page);await page.locator('[data-metric="gasRange"]').click();if(await page.locator('#saveTile').count())await page.locator('#saveTile').click();
  assert.match(await page.locator('[data-tile] .tile-number').last().textContent(),/Tag|Stunde/);assert.deepEqual(await page.evaluate(()=>dashboard.config.tiles.filter(t=>t.metric!=='gasRange').map(t=>({id:t.id,layouts:t.layouts}))),layouts);
  const rangeFits=async()=>{const rects=await page.locator('[data-tile]').last().evaluate(el=>{const number=el.querySelector('.tile-number').getBoundingClientRect(),visual=el.querySelector('.tile-visual').getBoundingClientRect();return {top:number.top,bottom:number.bottom,from:visual.top,to:visual.bottom};});assert.ok(rects.top>=rects.from-1&&rects.bottom<=rects.to+1,'days/hours text must fit entirely inside its tile: '+JSON.stringify(rects));};await rangeFits();
  await page.evaluate(()=>{document.getElementById('toast').hidden=true;document.getElementById('plantActionStatus').hidden=true;});await page.locator('[data-tile]').last().screenshot({path:path.join(out,'supplies-range-2.0.2.png'),animations:'disabled'});await page.setViewportSize({width:832,height:620});await waitForPage(page,()=>dashboard.profile==='wide');await rangeFits();await page.setViewportSize({width:390,height:900});await waitForPage(page,()=>dashboard.profile==='compact');
  await page.locator('[data-page="supplies"]').click();await page.locator('[data-supply-unit="gas"]').selectOption('Flaschen');await page.locator('[data-supply-unit="water"]').selectOption('Kanister');
  const quantityState=()=>({events:window.suppliesFixture.events,gas:{stock:supplies.analysis.gas.stock,daily:supplies.analysis.gas.daily,needs:supplies.analysis.gas.needs},water:{stock:supplies.analysis.water.stock,daily:supplies.analysis.water.daily,needs:supplies.analysis.water.needs}});
  const beforeSizes=await page.evaluate(quantityState);await page.locator('#suppliesSettings').click();
  await page.locator('#supplyWaterContainer').fill('0');await page.locator('#supplySave').click();assert.match(await page.locator('#supplyError').textContent(),/Kanistergröße/);
  await page.locator('#supplyWaterContainer').fill('10');await page.locator('#supplyGasDefault').fill('5');await page.locator('#supplySave').click();await waitForPage(page,()=>document.getElementById('modal').hidden);
  assert.match(await page.locator('[data-supply-needs="gas"]').textContent(),/7,7 Flaschen/);assert.match(await page.locator('[data-supply-needs="water"]').textContent(),/7 Kanister/);assert.deepEqual(await page.evaluate(quantityState),beforeSizes);
  await page.locator('#suppliesGas').click();assert.equal(await page.locator('#supplyAmount').inputValue(),'5');await page.locator('#supplyCancel').click();
  await page.locator('#suppliesSettings').click();assert.equal(await page.locator('#supplyWaterContainer').inputValue(),'10');assert.equal(await page.locator('#supplyWaterDefault').inputValue(),'25');
  await page.locator('#modal .sheet').screenshot({path:path.join(out,'supplies-settings-2.0.2.png'),animations:'disabled'});
  await page.locator('#supplyWaterContainer').fill('20');await page.locator('#supplyGasDefault').fill('11');await page.locator('#supplySave').click();await waitForPage(page,()=>document.getElementById('modal').hidden);
  for(const [metric,unit,sizeHint,value] of [['gasNeed14','Flaschen','1 Flasche = 11 kg','3,5'],['waterNeed14','Kanister','1 Kanister = 20 L','3,5']]){
   await page.locator('[data-page="overview"]').click();await page.evaluate(()=>document.getElementById('dashboardToolbar').hidden=false);await autarpieControls.addTile(page);await page.locator('[data-metric="'+metric+'"]').click();
   await page.locator('[data-tile]').last().click();await page.locator('#tileActionSettings').click();
   await page.locator('#tileUnit').selectOption(unit);assert.equal(await page.locator('#tileSupplyUnitHint').textContent(),sizeHint);
   await page.locator('#tileType').selectOption('gauge');await page.getByText('Skala',{exact:true}).click();await page.locator('#tileMax').fill('5');await page.locator('#saveTile').click();
   const tileId=await page.evaluate(metric=>dashboard.config.tiles.find(t=>t.metric===metric).id,metric),tile=page.locator('[data-tile="'+tileId+'"]');
   assert.equal(await tile.locator('.tile-number').textContent(),value);assert.equal(await tile.locator('.gauge-max').textContent(),'5');assert.match(await tile.locator('.tile-hint').textContent(),new RegExp(sizeHint));
   await page.evaluate(()=>document.querySelector('.bottom-nav').hidden=true);await tile.screenshot({path:path.join(out,metric.startsWith('gas')?'supplies-gas-demand-2.0.2.png':'supplies-water-demand-2.0.2.png'),animations:'disabled'});await page.evaluate(()=>document.querySelector('.bottom-nav').hidden=false);
   await tile.click();await page.locator('#tileActionDetail').click();assert.equal(await page.locator('#detailReading .tile-number').textContent(),value);assert.equal(await page.locator('.detail-container-size').textContent(),sizeHint);
   await page.locator('#detailSettings summary').click();await page.locator('#detailUnit').selectOption(metric.startsWith('gas')?'kg':'L');
   const base=await page.evaluate(()=>dashboard.detail.value);assert.ok(base>Number(value.replace(',','.')));await page.locator('#detailClose').click();
   await page.evaluate(()=>{delete snapshot.suppliesAnalysis;dashboard.update();});assert.equal(await tile.locator('.tile-number').textContent(),'—');assert.equal(await tile.locator('.gauge-max').textContent(),'—');
   await page.evaluate(()=>{supplies.render();dashboard.update();});assert.equal(await tile.locator('.tile-number').textContent(),value);
  }
  await page.locator('[data-page="supplies"]').click();
  const previousFixture=await page.evaluate(()=>JSON.parse(JSON.stringify(window.suppliesFixture)));
  await page.evaluate(()=>{
   const now=Date.now(),D=86400000,site=window.suppliesFixture.weather.place,heats=[2,12,7,17,4,14],events=[],history=[],forecast=[];
   for(let i=0;i<6;i++){
    const from=now-(6-i)*2*D,heat=heats[i];events.push({id:'weather-example-'+i,kind:'gas',type:'swap',at:from,amount:(.2+.08*heat)*2,empty:true,site});
    for(let d=0;d<2;d++)history.push({site:site.key,source:'archive',from:from+d*D,to:from+(d+1)*D,heat,windHeat:heat*.5,temperature:18-heat,wind:10});
   }
   events.push({id:'weather-example-last',kind:'gas',type:'swap',at:now,amount:8,empty:true,site});
   for(let d=0;d<15;d++)forecast.push({site:site.key,source:'forecast',from:now+d*D,to:now+(d+1)*D,heat:20,windHeat:10});
   Object.assign(window.suppliesFixture,{events,weather:{updated:now,timezone:'UTC',siteKey:site.key,place:site,history,forecast}});poll();
  });
  assert.equal(await page.locator('.supply-weather').getAttribute('data-state'),'active');
  assert.match(await page.locator('.supply-weather strong').textContent(),/aktiv · Temperatur/);
  assert.match(await page.locator('.supply-weather').textContent(),/6 von 6 abgeschlossenen/);
  await page.locator('[data-supply-unit="gas"]').selectOption('Flaschen');assert.match(await page.locator('[data-supply-needs="gas"]').textContent(),/2,3 Flaschen/);
  await page.setViewportSize({width:950,height:1050});await page.evaluate(()=>{document.getElementById('toast').hidden=true;});
  await page.evaluate(()=>document.querySelector('.bottom-nav').hidden=true);
  await page.locator('#supplies-gas').screenshot({path:path.join(out,'supplies-weather-active-2.0.2-dark.png'),animations:'disabled'});
  await page.evaluate(()=>{window.testTheme='light';poll();});await page.locator('#supplies-gas').screenshot({path:path.join(out,'supplies-weather-active-2.0.2-light.png'),animations:'disabled'});await page.evaluate(()=>document.querySelector('.bottom-nav').hidden=false);
  await page.evaluate(()=>{window.suppliesFixture.weather.enabled=false;poll();});
  assert.equal(await page.locator('.supply-weather').getAttribute('data-state'),'disabled','changing only weather availability must refresh the calculation');
  await page.evaluate(()=>{window.suppliesFixture.weather.enabled=true;window.suppliesFixture.weather.forecast=[];poll();});
  assert.equal(await page.locator('.supply-weather').getAttribute('data-state'),'missing-forecast','changing only forecast rows must refresh the calculation');
  await page.evaluate(fixture=>{window.suppliesFixture=fixture;window.testTheme='dark';poll();},previousFixture);
  await page.setViewportSize({width:390,height:900});await waitForPage(page,()=>!dashboard.resizePending&&document.body.scrollWidth<=innerWidth).catch(async error=>{console.error(await page.evaluate(()=>({width:innerWidth,overflow:[...document.querySelectorAll('body *')].filter(e=>e.getClientRects().length&&e.getBoundingClientRect().right>innerWidth+1).slice(0,10).map(e=>({id:e.id,cls:e.className,right:e.getBoundingClientRect().right}))})));throw error;});assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);
  await page.evaluate(()=>{delete window.suppliesFixture.containerSizeSettings;poll();});await page.locator('#suppliesSettings').click();assert.equal(await page.locator('#supplyWaterContainer').isDisabled(),true);assert.match(await page.locator('#modalBody').textContent(),/auch die Zentrale/);await page.locator('#supplyCancel').click();
  await page.evaluate(()=>{window.suppliesFixture.containerSizeSettings=true;poll();});
  await page.evaluate(()=>{window.online=false;poll();});assert.equal(await page.locator('#suppliesWater').isDisabled(),false,'offline entries remain available for durable queuing');assert.match(await page.locator('#suppliesStatus').textContent(),/Letzter Stand/);
  await page.evaluate(()=>{window.online=true;delete window.suppliesFixture.protocol;poll();});assert.equal(await page.locator('#suppliesGas').isDisabled(),true);assert.match(await page.locator('#suppliesStatus').textContent(),/AutarPie 3\.0\.0/);
  await page.evaluate(()=>{suppliesFixture.protocol=1;window.online=false;poll();});const writesBeforeOffline=await page.evaluate(()=>window.writes);await page.locator('#suppliesWater').click();await page.locator('#supplyAmount').fill('17');await page.locator('#supplySave').click();assert.equal(await page.evaluate(()=>window.writes),writesBeforeOffline);const queued=await page.evaluate(()=>JSON.parse(localStorage.getItem('autarpie-supply-outbox-v1')));assert.equal(queued.length,1);assert.equal(queued[0].state,'pending');assert.match(await page.locator('#apSupplyPending').textContent(),/Übertragung ausstehend/);
  await page.reload();await waitForPage(page,()=>JSON.parse(localStorage.getItem('autarpie-supply-outbox-v1')||'[]').length===0);assert.equal(await page.evaluate(()=>window.writes),1);const transferred=await page.evaluate(id=>suppliesFixture.events.filter(e=>e.id===id),queued[0].command.event.id);assert.equal(transferred.length,1);assert.equal(transferred[0].at,queued[0].command.event.at);assert.equal(transferred[0].amount,17);await page.evaluate(()=>{poll();poll();});assert.equal(await page.evaluate(()=>window.writes),1,'acknowledged entries are not sent a second time after reconnect');
  assert.deepEqual(errors,[]);console.log('PASS supplies UI: days/hours, delayed host acknowledgement, additions, early gas changes, delete/cancel, shared defaults, optional tiles, retained layouts, offline/update states and screenshots.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
