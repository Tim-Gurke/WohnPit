const autarpieControls=require('./autarpie-test-controls.cjs');
/* Shipped UI, synthetic data and the public sanitized layout. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const waitForPage=require('./browser-wait.cjs'),preset=require('../app/src/main/assets/dashboard-preset.json');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})});
 try{
 const page=await browser.newPage({viewport:{width:430,height:940},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.clock.setFixedTime(new Date('2026-11-05T12:00:00Z'));
 await page.addInitScript(preset=>{
  const now=Date.now(),DAY=86400000,site={key:'example-site',name:'Beispielort'},event=(kind,type,at,amount,id)=>({id,kind,type,at,amount,site,empty:true});
  window.testTheme='dark';window.testBoard=preset;window.savedColors=0;
  window.suppliesFixture={version:1,protocol:1,revision:1,defaults:{water:20,gas:11},events:[event('water','supply',now-3*DAY,20,'water-example-01'),event('water','supply',now-DAY,20,'water-example-02'),event('gas','swap',now-3*DAY,11,'gas-example-01'),event('gas','swap',now-DAY,11,'gas-example-02'),event('water','stock',now,30,'water-example-stock')],weather:{updated:now,timezone:'UTC',siteKey:site.key,place:site,history:[2,3,4].map(day=>({site:site.key,source:day===4?'recent':'archive',from:Date.parse('2026-11-0'+day+'T00:00:00Z'),to:Date.parse('2026-11-0'+(day+1)+'T00:00:00Z'),temperature:7+day,wind:14+day,heat:10,windHeat:4})),forecast:[]}};
  window.WattPit={appVersion:()=>'2.0.0',dashboard:()=>JSON.stringify(window.testBoard),saveDashboard:json=>{window.testBoard=JSON.parse(json);window.savedColors++;return '';},copyColorCode:code=>{window.copiedColor=code;return '';},settings:()=>'',solarSetup:()=>'{}',rollerSnapshot:()=>'{}',suppliesSnapshot:()=>JSON.stringify(window.suppliesFixture),syncState:()=>JSON.stringify({busy:false,runId:0,cockpitNoticeAccepted:true,theme:window.testTheme,palette:'classic'}),snapshot:()=>JSON.stringify({running:false,status:'Beispieldaten',devices:[],readings:[],found:[],stats:{count:0},settings:{theme:window.testTheme,capacity:100,sampleSeconds:10,weatherPlace:site}})};
 },preset);
 await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await autarpieControls.prepare(page);await waitForPage(page,()=>!!dashboard?.config?.tiles.length);
 const layouts=await page.evaluate(()=>dashboard.config.tiles.map(t=>t.layouts));assert.deepEqual(layouts,preset.tiles.map(t=>t.layouts));
 const output=process.env.WATTPIT_COLOR_SCREENSHOTS||null;if(output)fs.mkdirSync(output,{recursive:true});
 await autarpieControls.openSettings(page);await page.locator('#valueColorsOpen').click();
 assert.equal(await page.locator('#valueColor-waterHex').inputValue(),'#4C65E0');assert.equal(await page.locator('#valueColor-gasHex').inputValue(),'#C2C200');
 await page.evaluate(()=>document.getElementById('toast').hidden=true);if(output)await page.screenshot({animations:'disabled',path:path.join(output,'value-colors-2.0.0-dark.png')});
 await page.locator('#valueColor-waterHex').fill('#zzzzzz');await page.locator('#valueColorsSave').click();assert.match(await page.locator('#valueColorsError').textContent(),/gültigen Farbcode/);assert.equal(await page.evaluate(()=>window.savedColors),0);
 await page.locator('#valueColor-waterHex').fill('3A6FD2');await page.locator('#valueColor-waterCopy').click();assert.equal(await page.evaluate(()=>window.copiedColor),'#3A6FD2');
 await page.locator('#valueColorsSave').click();await waitForPage(page,()=>document.getElementById('modal').hidden);
 assert.equal(await page.evaluate(()=>dashboard.config.tiles.filter(t=>t.metric.startsWith('water')).every(t=>t.color==='#3a6fd2')),true);
 await page.locator('[data-page="overview"]').click();await autarpieControls.addTile(page);assert.equal(await page.locator('[data-metric="waterNeed3"] i').evaluate(el=>el.style.backgroundColor),'rgb(58, 111, 210)');
 await page.locator('[data-metric="waterNeed3"]').click();if(await page.locator('#saveTile').count())await page.locator('#saveTile').click();
 const tileId=await page.evaluate(()=>dashboard.config.tiles.find(t=>t.metric==='waterStock').id);await page.evaluate(id=>dashboard.editor(id),tileId);
 await page.getByText('Farben & Schrift',{exact:true}).click();await page.locator('#tileColorSource').selectOption('custom');await page.locator('#tileColorHex').fill('#123456');await page.locator('#saveTile').click();
 assert.equal(await page.evaluate(id=>dashboard.config.tiles.find(t=>t.id===id).color,tileId),'#123456');
 await autarpieControls.openSettings(page);await page.locator('#valueColorsOpen').click();await page.locator('#valueColor-waterHex').fill('#4C65E0');await page.locator('#valueColorsSave').click();
 assert.equal(await page.evaluate(id=>dashboard.config.tiles.find(t=>t.id===id).color,tileId),'#123456');
 await page.evaluate(id=>dashboard.editor(id),tileId);await page.getByText('Farben & Schrift',{exact:true}).click();await page.locator('#tileColorSource').selectOption('value');await page.locator('#tileColorCopy').click();assert.equal(await page.evaluate(()=>window.copiedColor),'#4C65E0');await page.locator('#saveTile').click();
 await page.locator('[data-page="supplies"]').click();assert.match(await page.locator('#suppliesWeatherHistory').textContent(),/Rückblick vollständig/);assert.match(await page.locator('#suppliesWeatherHistory').textContent(),/3 von 3/);
 await page.locator('#suppliesWeatherHistory summary').click();await page.evaluate(()=>document.getElementById('toast').hidden=true);if(output)await page.screenshot({animations:'disabled',path:path.join(output,'weather-history-2.0.0-dark.png')});
 await page.evaluate(()=>{window.suppliesFixture.weather.history[1].source='forecast';supplies.renderKey='';supplies.render();});assert.match(await page.locator('#suppliesWeatherHistory').textContent(),/Wettertage fehlen/);assert.match(await page.locator('#suppliesWeatherHistory').textContent(),/2 von 3/);
 await page.evaluate(()=>{window.suppliesFixture.weather.history[1].source='archive';window.testTheme='light';poll();});
 await page.evaluate(()=>document.getElementById('toast').hidden=true);if(output)await page.screenshot({animations:'disabled',path:path.join(output,'weather-history-2.0.0-light.png')});
 await autarpieControls.openSettings(page);await page.locator('#valueColorsOpen').click();await page.evaluate(()=>document.getElementById('toast').hidden=true);if(output)await page.screenshot({animations:'disabled',path:path.join(output,'value-colors-2.0.0-light.png')});
 await page.locator('#valueColor-gasHex').fill('#FFFFFF');await page.locator('#valueColorsCancel').click();assert.equal(await page.evaluate(()=>dashboard.config.valueColors.gasStock),'#c2c200');
 assert.equal(await page.evaluate(()=>document.body.scrollWidth>innerWidth),false);assert.deepEqual(errors,[]);
 console.log('PASS shared metric colors, exact hex input/copy, catalogue, custom/inherited tiles, cancel, full layout, historical coverage and both themes.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
