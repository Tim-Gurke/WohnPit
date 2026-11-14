/* Actual shipped UI with its sanitized defaults and neutral measurements. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})}),page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[];
 const out=path.resolve(process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/autarpie-ui');fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const preset=require('../app/src/main/assets/ui-preset.json');
 try{
  await page.addInitScript({content:`(${require('./autarpie-fixture.cjs').toString()})();\n(${function(p){
   apFixture.board=p.dashboard;apFixture.workspace=p.workspace;apFixture.appearance=p.appearance;apFixture.scale=p.viewSettings.displayScale;
   const original=WattPit.snapshot,roller=WattPit.rollerSnapshot,history=WattPit.history;apFixture.offline=true;
   WattPit.appVersion=()=>'3.0.4';WattPit.uiDefaults=()=>JSON.stringify(p);
   WattPit.snapshot=()=>{const s=JSON.parse(original()),noon=new Date();noon.setHours(12,0,0,0);s.settings.weatherPlace={name:'Beispielort',latitude:0,longitude:0};s.weather={timezone:'Europe/Berlin',updated:Date.now(),place:s.settings.weatherPlace,days:Array.from({length:19},(_,i)=>{const date=new Date(noon);date.setDate(date.getDate()+i-3);return {day:date.toLocaleDateString('sv-SE'),hours:4,code:0,sunrise:+date-5*3600000,sunset:+date+4*3600000};})};s.solarDaily=s.weather.days.slice(0,3).map((d,i)=>({day:d.day,'Yield(Wh)':300+i*50}));s.energy=JSON.parse(WattPit.energySnapshot());if(apFixture.noVictron)s.readings=[];if(apFixture.partial)s.readings=s.readings.filter(r=>r.kind==='shunt');return JSON.stringify(s);};
   WattPit.rollerSnapshot=()=>{const s=JSON.parse(roller());if(apFixture.offline){s.lastKnown={...s.live,at:Date.now()-3*3600000};s.live={};s.fresh=false;s.status='Nicht erreichbar';}return JSON.stringify(s);};
   WattPit.history=(from,to,metric)=>metric==='soc'?JSON.stringify({points:[{t:(from+to)/2,v:52,origin:'live',kind:'shunt',device:'synthetic-shunt',period:10}]}):history(from,to,metric);
   WattPit.saveLoadConfiguration=json=>{apFixture.output=JSON.parse(json);return '';};
  }.toString()})(${JSON.stringify(preset)});`});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieMetricUI&&!!window.AutarPieOutbox);assert.deepEqual(errors,[]);
  assert.equal(await page.locator('[data-tile]').count(),21);assert.equal(await page.locator('.bottom-nav .nav-label').count(),0);
  for(const name of ['E-Scooter','Modellbau'])assert.equal(await page.getByRole('button',{name,exact:true}).count(),1);
  await page.evaluate(()=>{apFixture.noVictron=true;AutarPieApp.poll();});assert.doesNotMatch(await page.locator('#liveStatus').textContent(),/Live/);
  await page.evaluate(()=>{apFixture.noVictron=false;apFixture.partial=true;AutarPieApp.poll();});assert.match(await page.locator('#liveStatus').textContent(),/Teilweise live/);
  await page.evaluate(()=>{apFixture.partial=false;AutarPieApp.poll();});assert.equal((await page.locator('#liveStatus').textContent()).trim(),'Live');
  await page.evaluate(()=>{if(!AutarPieApp.dashboard.config.tiles.some(t=>t.metric==='rollerSoc'))AutarPieApp.dashboard.add('rollerSoc');AutarPieApp.poll();});
  const cached=page.locator('.cached-reading');assert.ok(await cached.count()>0);assert.match(await cached.first().textContent(),/Letzter Datenstand.*Roller nicht erreichbar/);
  await page.getByRole('button',{name:'E-Scooter',exact:true}).click();await page.evaluate(()=>AutarPieEnergy.openRules());assert.equal(await page.locator('#apPowerPath').inputValue(),'direct');
  await page.locator('#apPowerPath').selectOption('relay');await page.locator('#apRuleCancel').click();assert.equal(await page.evaluate(()=>apFixture.output===undefined),true,'cancel never activates relay mode');
  await page.evaluate(()=>AutarPieEnergy.openRules());assert.equal(await page.locator('#apPowerPath').inputValue(),'direct');await page.locator('#apPowerPath').selectOption('relay');await page.locator('#apRuleSave').click();assert.equal(await page.evaluate(()=>apFixture.output.powerPath),'relay');assert.equal(await page.evaluate(()=>apFixture.output.rules.enabled),false);
  await page.evaluate(()=>{AutarPieApp.showPage('overview');AutarPieApp.solar.renderKey='';AutarPieApp.solar.render();});await wait(page,()=>document.querySelectorAll('[data-weather-history]').length===3);
  await page.locator('[data-weather-history]').first().evaluate(el=>el.click());assert.match(await page.locator('#modalBody').textContent(),/Solarertrag.*300 Wh/);assert.match(await page.locator('#modalBody').textContent(),/52 %/);await page.locator('#closeModal').click();
  await page.evaluate(()=>{const solar=AutarPieApp.solar;solar.openDay(0);document.getElementById('solarLearningCoverage').open=true;const now=Date.now(),day=new Date(now-86400000).toLocaleDateString('sv-SE');onSolarPlanData({source:'',updated:now,observations:[{day,slot:0,from:now-86400000,to:now-86400000+3600000,learningVersion:2,usage:{wh:10,duration:3600000,estimatedDuration:0,latest:now-86400000+3600000}}],forecasts:[]});});
  assert.equal(await page.locator('#solarLearningCoverage').evaluate(el=>el.open),true);assert.match(await page.locator('#solarLearningCoverage summary').textContent(),/1.*ausgewertete Stunden/);await page.locator('#closeModal').click();
  await page.reload();await wait(page,()=>!!window.AutarPieOutbox);await page.evaluate(()=>document.getElementById('toast').hidden=true);await page.screenshot({path:path.join(out,'autarpie-3.0.4-dark.png'),animations:'disabled'});
  await page.evaluate(()=>{AutarPieUI.appearance.theme='light';AutarPieUI.applyAppearance();});await page.screenshot({path:path.join(out,'autarpie-3.0.4-light.png'),animations:'disabled'});
  await page.evaluate(()=>{AutarPieUI.appearance.theme='dark';AutarPieUI.applyAppearance();AutarPieApp.showPage('settings');document.getElementById('apSettingsBack').click();});await page.screenshot({path:path.join(out,'autarpie-3.0.4-settings.png'),animations:'disabled'});
  assert.equal(await page.evaluate(()=>document.body.scrollWidth*(parseFloat(document.documentElement.style.zoom)||1)<=innerWidth+1),true);assert.deepEqual(errors,[]);
  console.log('PASS 3.0.4: shipped defaults, accessible symbol shortcuts, honest live status, cached offline BMS, explicit relay save/cancel, measured weather retrospect, refreshing learning details.');
 }catch(e){await page.screenshot({path:path.join(out,'release-304-failure.png'),fullPage:true}).catch(()=>{});console.error(errors);throw e;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
