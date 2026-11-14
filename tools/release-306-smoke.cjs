/* Actual navigation and charge-learning settings; synthetic inputs, no hardware claim. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs'),wait=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})}),page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'UTC'}),errors=[];
 const out=path.resolve(process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/autarpie-ui');fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const preset=require('../app/src/main/assets/ui-preset.json');
 try{
  await page.addInitScript({content:'('+require('./autarpie-fixture.cjs').toString()+')();('+function(p){
   apFixture.board=p.dashboard;apFixture.workspace=p.workspace;apFixture.appearance={...p.appearance,accent:'#22cc88'};apFixture.scale=p.viewSettings.displayScale;
   apFixture.workspace.pages.push({id:'custom-learning',name:'Eigene Ansicht',board:JSON.parse(JSON.stringify(p.dashboard)),design:{enabled:true,accent:'#ff2255'}});apFixture.workspace.navigation=['overview','solar','history','custom-learning','storage','model','supplies','devices','settings'];
   const original=WattPit.rollerSnapshot;WattPit.appVersion=()=>'3.0.6';WattPit.rollerSnapshot=()=>{const r=JSON.parse(original());r.profile.planCharge=true;r.fresh=false;r.lastKnown={...r.live,at:Date.now()-86400000};r.live={};return JSON.stringify(r);};
   const mac=JSON.parse(original()).profile.mac,H=3600000;apFixture.special={version:1,profiles:[{id:'roller',kind:'roller',name:'Beispiel-Roller',enabled:true,mode:'learn',wh:0,km:0,minutes:30,hour:12,chargesPerWeek:3.5}],events:[]};apFixture.learning={version:1,mac,timezone:'UTC',pathDays:{},charges:[{id:'first',mac,from:Date.now()-2*86400000,to:Date.now()-2*86400000+H/2,inputWh:300,coverage:1}]};
   WattPit.specialUses=()=>JSON.stringify({...apFixture.special,rollerLearning:apFixture.learning});WattPit.saveSpecialUses=json=>{apFixture.special=JSON.parse(json);return '';};
  }.toString()+')('+JSON.stringify(preset)+');'});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieUI&&!!window.AutarPieApp?.solar);
  async function selection(id,color){const selected=page.locator('.bottom-nav [aria-current="page"]');assert.equal(await selected.count(),1);assert.equal(await selected.getAttribute('data-page'),id);assert.equal(await selected.locator('svg').evaluate(e=>getComputedStyle(e).stroke),color);}
  for(const id of ['overview','solar','history','storage','model','supplies','devices','settings','custom-learning']){await page.evaluate(id=>AutarPieApp.showPage(id),id);await selection(id,'rgb(34, 204, 136)');}
  await page.evaluate(()=>{AutarPieUI.workspace.navigation.reverse();AutarPieUI.navigation();});await selection('custom-learning','rgb(34, 204, 136)');
  await page.evaluate(()=>{AutarPieUI.appearance.accent='#d87733';apFixture.appearance={...AutarPieUI.appearance};AutarPieUI.applyAppearance();});await selection('custom-learning','rgb(216, 119, 51)');
  for(const theme of ['light','dark']){await page.evaluate(theme=>{AutarPieUI.appearance.theme=theme;apFixture.appearance={...AutarPieUI.appearance};AutarPieUI.applyAppearance();},theme);await selection('custom-learning','rgb(216, 119, 51)');}
  await page.evaluate(()=>AutarPieApp.solar.special.open());assert.match(await page.locator('.special-learning').textContent(),/1 auswertbare Ladung.*300 Wh/);assert.match(await page.locator('.special-learning').textContent(),/Planungsannahme.*3,5/);
  await page.locator('[data-special-edit="roller"]').click();await page.locator('#specialChargesPerWeek').fill('2.5');await page.locator('#specialSave').click();assert.equal(await page.evaluate(()=>apFixture.special.profiles[0].chargesPerWeek),2.5);assert.equal(await page.evaluate(()=>apFixture.special.rollerLearning),undefined);
  await page.screenshot({path:path.join(out,'autarpie-3.0.6-settings.png'),animations:'disabled'});await page.locator('#closeModal').click();
  await page.evaluate(()=>{AutarPieApp.showPage('storage');document.getElementById('toast').hidden=true;});
  await page.locator('.bottom-nav [data-page="storage"]').scrollIntoViewIfNeeded();await selection('storage','rgb(216, 119, 51)');await page.screenshot({path:path.join(out,'autarpie-3.0.6-dark.png'),animations:'disabled'});
  assert.deepEqual(errors,[]);console.log('PASS all built-in/custom shortcut accents, page-specific design, reordered shortcuts, live shared color/theme changes, and offline short-charge learning with an explicit weekly assumption.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
