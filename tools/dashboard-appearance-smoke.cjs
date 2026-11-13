/* Actual page transitions and color editor with neutral, independently stored designs. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs'),wait=require('./browser-wait.cjs');
const fixture=require('./autarpie-fixture.cjs');
function appearanceFixture(){
 const surface=first=>({mode:'solid',first,second:first,angle:135});
 apFixture.appearance=JSON.parse(localStorage.getItem('test-page-appearance')||'null')||{version:1,theme:'dark',customSurfaces:true,app:surface('#20344d'),tile:surface('#263a50')};
 apFixture.workspace=JSON.parse(localStorage.getItem('test-page-workspace')||'null')||{version:1,pages:[
  {id:'overview',board:JSON.parse(JSON.stringify(apFixture.board)),design:{enabled:true,app:surface('#123456'),tile:surface('#1a3652'),value:'#37c28e'}},
  {id:'solar',design:{enabled:true,app:surface('#111111'),tile:surface('#252525'),value:'#e4b15c'}},
  {id:'history',design:{enabled:false}},
  {id:'custom-demo',name:'Eigene Testseite',board:JSON.parse(JSON.stringify(apFixture.board)),design:{enabled:true,app:surface('#213f25'),tile:surface('#294d30')}}
 ],navigation:['overview','solar','history','custom-demo']};
 const save=WattPit.saveWorkspace;WattPit.saveWorkspace=json=>{if(window.rejectNextPageSave){window.rejectNextPageSave=false;return 'Speichern im Test abgelehnt';}const error=save(json);if(!error)localStorage.setItem('test-page-workspace',json);return error;};
}
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})}),page=await browser.newPage({viewport:{width:412,height:915},locale:'de-DE',timezoneId:'Europe/Berlin'}),errors=[];
 const out=path.resolve(process.env.WATTPIT_SCREENSHOT_DIR||'app/build/reports/dashboard-appearance-ui');fs.mkdirSync(out,{recursive:true});page.on('pageerror',e=>errors.push(e.message));
 const show=async id=>page.evaluate(id=>AutarPieApp.showPage(id),id);
 const background=async(expected,label)=>{const actual=await page.evaluate(()=>({root:getComputedStyle(document.documentElement).getPropertyValue('--ap-app-background').trim(),body:getComputedStyle(document.body).backgroundColor}));assert.equal(actual.root,expected,label);assert.equal(actual.body,'rgb('+[1,3,5].map(i=>parseInt(expected.slice(i,i+2),16)).join(', ')+')',label+' rendered body');};
 const edit=async name=>{await page.locator('#autarpieAreas').click();await page.locator('#apDashboardDesign').click();assert.equal(await page.locator('#modalTitle').textContent(),'Dashboard-Design · '+name,'color editor targets the visible page');};
 const color=async(id,value)=>{await page.locator('#'+id+'Hex').fill(value);await page.locator('#'+id+'Hex').dispatchEvent('input');};
 try{
  await page.addInitScript({content:'('+fixture.toString()+')();('+appearanceFixture.toString()+')();'});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await wait(page,()=>!!window.AutarPieMetricUI&&!!window.AutarPieUI);assert.deepEqual(errors,[]);
  for(const origin of ['overview','solar','custom-demo','devices','settings']){await show(origin);await show('history');await background('#20344d','Verlauf uses the common design after '+origin);await page.evaluate(()=>AutarPieApp.poll());await background('#20344d','poll keeps Verlauf independent of '+origin);}
  console.log('PASS history falls back to shared colors independently of the previous page and polling');
  const before=await page.evaluate(()=>JSON.parse(JSON.stringify(AutarPieUI.workspace)));
  await edit('Verlauf');await page.locator('#apPageEnabled').check();await color('apPageFirst-app','#442244');await color('apPageFirst-tile','#332244');await page.locator('#apPageValueEnabled').check();await color('apPageValue','#9fc4ea');await page.locator('#apPageApplyAll').check();await page.locator('#apPageSave').click();
  await background('#442244','saved history design');
  const after=await page.evaluate(()=>JSON.parse(JSON.stringify(AutarPieUI.workspace)));
  assert.deepEqual(after.pages.filter(p=>p.id!=='history'),before.pages.filter(p=>p.id!=='history'),'saving history colors cannot change another page or its tiles');
  assert.deepEqual(after.pages.find(p=>p.id==='history').board,before.pages.find(p=>p.id==='history').board,'history colors cannot copy the last dashboard tile board');
  assert.equal(await page.evaluate(()=>AutarPieAppearance.colors.solar),'#9fc4ea','history chart colors use its effective design');
  assert.equal(await page.evaluate(()=>AutarPieUI.appearance.colors.solar),'#eea34d','per-page value colors cannot modify shared colors');
  assert.deepEqual(await page.evaluate(()=>apFixture.workspace),after,'native workspace stores the intended page');
  console.log('PASS history color editor changes only history, preserves tile boards and colors its own charts');
  for(const [origin,expected]of [['overview','#123456'],['solar','#111111'],['custom-demo','#213f25'],['devices','#20344d'],['settings','#20344d']]){await show(origin);await background(expected,origin+' keeps its design');await show('history');await background('#442244','history retains its design after '+origin);}
  await edit('Verlauf');await color('apPageFirst-app','#553355');await page.evaluate(()=>window.rejectNextPageSave=true);await page.locator('#apPageSave').click();assert.match(await page.locator('#apPageError').textContent(),/abgelehnt/);await background('#442244','rejected save leaves the displayed design intact');assert.deepEqual(await page.evaluate(()=>apFixture.workspace),after,'rejected save leaves stored pages intact');await page.locator('#apPageSave').click();await background('#553355','retry saves the same visible page');
  await show('solar');await edit('Solar & Speicher');await color('apPageFirst-app','#192b3d');await page.locator('#apPageSave').click();await background('#192b3d','solar change applies to solar');await show('history');await background('#553355','solar change cannot alter history');
  await edit('Verlauf');await color('apPageFirst-app','#775577');await page.locator('#apPageCancel').click();await background('#553355','cancel leaves history unchanged');
  console.log('PASS page designs remain isolated after switching, rejected-save retry and cancellation');
  await page.reload();await wait(page,()=>!!window.AutarPieMetricUI);await show('solar');await background('#192b3d','solar survives restart');await show('history');await background('#553355','history survives restart');
  for(const viewport of [{width:915,height:412},{width:412,height:915}]){await page.setViewportSize(viewport);await show('overview');await show('history');await background('#553355','orientation change preserves the selected design');}
  await edit('Verlauf');await page.locator('#apPageEnabled').uncheck();await page.locator('#apPageSave').click();await background('#20344d','disabled history design restores shared colors');
  await page.evaluate(()=>{apFixture.appearance={...AutarPieUI.appearance,app:{mode:'solid',first:'#304a63',second:'#304a63',angle:135}};localStorage.setItem('test-page-appearance',JSON.stringify(apFixture.appearance));AutarPieApp.poll();});await background('#304a63','disabled history design follows updated shared colors');
  await show('overview');await background('#123456','shared update keeps custom overview');await show('solar');await background('#192b3d','shared update keeps custom solar');await show('history');await background('#304a63','history fallback does not borrow custom solar');
  assert.deepEqual(errors,[]);await page.evaluate(()=>document.getElementById('toast').hidden=true);await page.screenshot({path:path.join(out,'autarpie-3.0.3-dashboard-colors.png'),fullPage:false,animations:'disabled'});
  console.log('PASS page colors survive restart and orientation changes; disabling restores only shared defaults');
 }catch(e){await page.screenshot({path:path.join(out,'dashboard-colors-failure.png'),fullPage:true}).catch(()=>{});console.error('Page errors:',errors);throw e;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
