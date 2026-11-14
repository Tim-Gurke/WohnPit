/* Exercise the shipped learning explanation and viewer export entry points. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})});
 const page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[];
 const out=path.resolve(process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/autarpie-ui');fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const preset=require('../app/src/main/assets/ui-preset.json');
 try{
  await page.addInitScript({content:`(${require('./autarpie-fixture.cjs').toString()})();\n(${function(){
   const original=WattPit.snapshot;apFixture.exports=0;apFixture.rebuilds=0;
   WattPit.appVersion=()=>'3.0.5';WattPit.isLanViewer=()=>true;WattPit.lanState=()=>JSON.stringify({role:'viewer',name:'Neutral test plant',connected:false,receivedAt:Date.now()-3600000});
   WattPit.choosePlanningDiagnostics=()=>apFixture.exports++;WattPit.requestLearningRebuild=()=>{apFixture.rebuilds++;onSolarLearningProgress({message:'Historische Messdaten werden abgeglichen …'});};
   WattPit.snapshot=()=>{const s=JSON.parse(original()),noon=new Date();noon.setHours(12,0,0,0);s.settings.weatherPlace={name:'Beispielort',latitude:0,longitude:0};s.weather={timezone:'Europe/Berlin',updated:Date.now(),place:s.settings.weatherPlace,days:Array.from({length:16},(_,i)=>{const d=new Date(noon);d.setDate(d.getDate()+i);return {day:d.toLocaleDateString('sv-SE'),hours:4,code:0,sunrise:+d-5*3600000,sunset:+d+4*3600000};})};return JSON.stringify(s);};
  }.toString()})();`});
  await page.addInitScript(p=>{apFixture.board=p.dashboard;apFixture.workspace=p.workspace;apFixture.appearance=p.appearance;apFixture.scale=p.viewSettings.displayScale;},preset);
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieUI&&!!window.AutarPieApp);
  await page.evaluate(()=>{AutarPieApp.showPage('settings');AutarPieUI.openSettings('data');});
  assert.equal(await page.locator('#exportPlanningDiagnostics').isEnabled(),true,'a cached viewer can export its own analysis');
  assert.equal(await page.locator('#exportPlanningDiagnostics').isVisible(),true,'diagnosis belongs to Data and backup');
  await page.locator('#exportPlanningDiagnostics').click();assert.equal(await page.evaluate(()=>apFixture.exports),1);
  await page.locator('#rebuildLearning').click();assert.equal(await page.evaluate(()=>apFixture.rebuilds),1);assert.match(await page.locator('#solarHistoryStatus').textContent(),/Historische Messdaten/);
  await page.screenshot({path:path.join(out,'autarpie-3.0.5-settings.png'),animations:'disabled'});
  await page.evaluate(()=>{
   const H=3600000,now=Date.now(),from=now-2*86400000,day=new Date(from).toLocaleDateString('sv-SE'),usage={wh:10,duration:H,estimatedDuration:0,latest:from+H,rejected:{measurementGap:3*H}};
   const row={from,to:from+11*H,day,slot:0,learningVersion:2,presenceRejections:true,usage,usageByPresence:{home:usage,away:{wh:2,duration:2*H,estimatedDuration:0,latest:from+3*H},unknown:{wh:1,duration:H,estimatedDuration:0,latest:from+4*H}}};
   const old={...row,from:from-86400000,to:from-86400000+11*H,day:new Date(from-86400000).toLocaleDateString('sv-SE'),presenceRejections:false,usage:{...usage,rejected:{weather:2*H}}};
   onSolarPlanData({source:'',updated:now,historyRange:{from:from-40*86400000,to:now,count:5000},observations:[row,old],forecasts:[]});AutarPieApp.solar.openDay(0);document.getElementById('solarLearningCoverage').open=true;
  });
  const explanation=await page.locator('#solarLearningCoverage').textContent();assert.match(explanation,/Messgrundlage auf diesem Handy/);assert.match(explanation,/Gemeinsame Messabdeckung fehlt/);assert.match(explanation,/anderen Anwesenheitsprofil/);assert.match(explanation,/unbekannter Anwesenheit/);assert.match(explanation,/Ältere Ausschlüsse.*Anwesenheit nicht zuordenbar/);assert.match(explanation,/Passende historische Wetterdaten fehlen/);
  await page.screenshot({path:path.join(out,'autarpie-3.0.5-learning.png'),animations:'disabled'});await page.locator('#closeModal').click();
  await page.evaluate(()=>{AutarPieApp.showPage('overview');document.getElementById('toast').hidden=true;});await page.screenshot({path:path.join(out,'autarpie-3.0.5-dark.png'),animations:'disabled'});
  await page.evaluate(()=>{AutarPieUI.appearance.theme='light';AutarPieUI.applyAppearance();});await page.screenshot({path:path.join(out,'autarpie-3.0.5-light.png'),animations:'disabled'});
  assert.deepEqual(errors,[]);console.log('PASS historical learning explanation, separate presence profiles, visible legacy rejections, and cached viewer export/rebuild controls.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
