// Synthetic places and callbacks; no GPS or geocoder service is contacted.
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
const D=require('../app/src/main/assets/dashboard-core.js');
const waitForPage=require('./browser-wait.cjs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 try{
  const page=await browser.newPage({viewport:{width:390,height:844},locale:'de-DE'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(board=>{
   window.gpsStarts=0;window.board=JSON.parse(localStorage.getItem('gps-board')||JSON.stringify(board));
   const place=JSON.parse(localStorage.getItem('gps-place')||'{"name":"GPS-Standort","latitude":52.52,"longitude":13.41}');
   window.fixture={running:false,devices:[],readings:[],found:[],stats:{count:0},historyRevision:1,equipment:{version:1,groups:[]},settings:{theme:'dark',palette:'classic',capacity:100,sampleSeconds:10,miniRange:'today',weatherPlace:place,solarSetup:{}},weather:{timezone:'Europe/Berlin',days:[]}};
   window.WattPit={appVersion:()=> '1.8.2',snapshot:()=>JSON.stringify(fixture),dashboard:()=>JSON.stringify(window.board),saveDashboard:json=>{window.board=JSON.parse(json);localStorage.setItem('gps-board',json);return '';},settings:json=>{Object.assign(fixture.settings,JSON.parse(json));return '';},history:()=>'{"points":[]}',dashboardHistory:()=>'{"points":[],"summary":{}}',energyHistoryWindow:()=>'{"error":true}',solarSetup:()=>JSON.stringify(fixture.settings.solarSetup),saveSolarSite:(setup,place)=>{fixture.settings.solarSetup=JSON.parse(setup);fixture.settings.weatherPlace=JSON.parse(place);localStorage.setItem('gps-place',place);return '';},locateSolarSite:()=>gpsStarts++,cancelSolarLocation:()=>{},searchSolarPlaces:query=>setTimeout(()=>onSolarPlaces({query,places:[{name:'Manueller Testort',latitude:50,longitude:9}]}),0)};
  },D.defaults());
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));
  const heading=page.locator('#weatherPlaceName'),detail=heading.locator('.weather-place-detail');
  assert.equal(await heading.textContent(),'GPS · 52.520°, 13.410°');assert.equal(await page.evaluate(()=>gpsStarts),0,'opening the app must not request a GPS fix');
  const layouts=await page.evaluate(()=>JSON.stringify(dashboard.config.tiles.map(t=>t.layouts)));
  await page.evaluate(()=>{fixture.settings.weatherPlace.name='GPS · Testort';poll();});assert.equal(await heading.textContent(),'GPS · Testort');assert.equal(await detail.textContent(),' · Testort');
  assert.equal(await detail.evaluate(e=>getComputedStyle(e).fontWeight),'400');assert.equal(await heading.evaluate(e=>e.firstChild.textContent),'GPS');
  for(const width of [320,390,412,844])for(const theme of ['dark','light']){
   await page.setViewportSize({width,height:844});await page.evaluate(theme=>{fixture.settings.theme=theme;poll();dashboard.render();},theme);
   assert.equal(await heading.textContent(),'GPS · Testort');assert.equal(await detail.evaluate(e=>getComputedStyle(e).fontWeight),'400');assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);
   assert.equal(await page.evaluate(()=>JSON.stringify(dashboard.config.tiles.map(t=>t.layouts))),layouts);
  }
  await page.evaluate(()=>{fixture.settings.weatherPlace.name='GPS · '+('Langer Testort ').repeat(10).trim();poll();});assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);assert.ok((await heading.getAttribute('aria-label')).includes('Langer Testort'));
  await page.evaluate(()=>{fixture.settings.weatherPlace.name='GPS · Testort';poll();});
  // A returned name is a draft until the site is explicitly saved.
  await page.locator('#solarSetupButton').click();await page.locator('#solarLocate').click();
  await page.evaluate(()=>onSolarLocation({name:'GPS · Anderer Testort',latitude:52.52,longitude:13.41,accuracy:25,time:Date.now()}));
  assert.equal(await page.locator('#solarSelectedPlace').textContent(),'GPS · Anderer Testort');assert.equal(await heading.textContent(),'GPS · Testort');await page.locator('#solarSetupCancel').click();assert.equal(await heading.textContent(),'GPS · Testort');
  await page.locator('#solarSetupButton').click();await page.locator('#solarLocate').click();
  await page.evaluate(()=>onSolarLocation({name:'GPS · Anderer Testort',latitude:52.52,longitude:13.41,accuracy:25,time:Date.now()}));await page.locator('#solarSetupSave').click();
  assert.equal(await heading.textContent(),'GPS · Anderer Testort');assert.deepEqual(await page.evaluate(()=>fixture.settings.weatherPlace),{name:'GPS · Anderer Testort',latitude:52.52,longitude:13.41});
  const backup=await page.evaluate(()=>WattSettingsCore.normalize(WattSettingsCore.create(fixture,dashboard.config,fixture.equipment)));assert.equal(backup.weatherPlace.name,'GPS · Anderer Testort');
  await page.reload();await waitForPage(page,()=>document.getElementById('weatherPlaceName')?.textContent==='GPS · Anderer Testort');assert.equal(await heading.textContent(),'GPS · Anderer Testort');assert.equal(await page.evaluate(()=>gpsStarts),0);
  await page.evaluate(()=>{const t=dashboard.config.tiles.find(t=>t.metric==='weather');t.title='<Mein Wetter>';dashboard.commit();});assert.equal(await heading.textContent(),'<Mein Wetter>');assert.equal(await heading.getAttribute('aria-label'),'<Mein Wetter> · GPS · Anderer Testort');assert.equal(await heading.locator('img,b').count(),0);
  await page.evaluate(()=>{dashboard.config.tiles.find(t=>t.metric==='weather').title='';dashboard.commit();});
  // Manually searched places keep their names and are not mislabeled as GPS.
  await page.locator('#solarSetupButton').click();await page.locator('#solarPlaceQuery').fill('Manuell');await page.locator('#solarPlaceSearch').click();await page.locator('#solarPlaceResults button').click();await page.locator('#solarSetupSave').click();assert.equal(await heading.textContent(),'Manueller Testort');assert.equal(await detail.count(),0);
  assert.deepEqual(errors,[]);console.log('PASS GPS place header: thin locality, existing-coordinate fallback, no unsolicited GPS, draft/cancel/save, restart/backup, long names, custom captions, manual search, 320–844px and both themes.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
