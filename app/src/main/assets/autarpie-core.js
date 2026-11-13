/* AutarPie configuration, inheritance and charge decisions; independent of DOM/Bluetooth. */
(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.AutarPieCore=api;})(typeof globalThis==='object'?globalThis:this,function(){
  'use strict';
  const clone=x=>JSON.parse(JSON.stringify(x)),hex=(v,f)=>/^#[0-9a-f]{6}$/i.test(v||'')?v.toLowerCase():f;
  const finite=v=>v!==null&&v!==''&&Number.isFinite(Number(v));
  const limit=(v,f,a,b)=>finite(v)?Math.max(a,Math.min(b,Number(v))):f;
  const CATEGORIES={gas:{name:'Gas',color:'#d3aa32'},water:{name:'Wasser',color:'#529ddd'},solar:{name:'Solar',color:'#eea34d'},calculated:{name:'Berechnete Werte',color:'#64bd8d'},model:{name:'Modellbau',color:'#b080d8'},scooter:{name:'E-Scooter',color:'#56bdb5'},storage:{name:'Batterie / Speicher',color:'#949edb'},system:{name:'Geräte / System',color:'#99a9bb'}};
  const PAGES=[['overview','Übersicht'],['storage','E-Scooter'],['solar','Solar & Speicher'],['supplies','Gas / Wasser'],['model','Modellbau'],['history','Verlauf'],['devices','Geräte'],['settings','Einstellungen']];
  const key=id=>typeof id==='string'&&/^[a-zA-Z0-9_-]{1,80}$/.test(id);
  function surface(x={},fallback={mode:'solid',first:'#142236',second:'#263a50',angle:135}){return {mode:x.mode==='gradient'?'gradient':x.mode==='solid'?'solid':fallback.mode,first:hex(x.first,fallback.first),second:hex(x.second,fallback.second),angle:limit(x.angle,fallback.angle,0,360)};}
  function surfaceCss(s){s=surface(s);return s.mode==='gradient'?`linear-gradient(${s.angle}deg,${s.first},${s.second})`:s.first;}
  function surfaceBase(s){s=surface(s);return s.mode==='gradient'?mix(s.first,s.second,.5):s.first;}
  function textOn(s){const c=surfaceBase(s),l=[1,3,5].map(i=>parseInt(c.slice(i,i+2),16)/255).reduce((sum,v,i)=>sum+v*[.2126,.7152,.0722][i],0);return l>.58?'#142236':'#edf2f8';}
  function appearance(x={}){
    const colors={};for(const [id,d]of Object.entries(CATEGORIES))colors[id]=hex(x.colors?.[id],d.color);
    return {version:1,theme:['system','light','dark'].includes(x.theme)?x.theme:'system',accent:hex(x.accent,'#80b7ed'),colors,metricColors:Object.fromEntries(Object.entries(x.metricColors||{}).filter(([k,v])=>key(k)&&hex(v,null)).slice(0,128)),
      app:surface(x.app,{mode:'solid',first:'#0b1523',second:'#253445',angle:135}),tile:surface(x.tile),
      customSurfaces:x.customSurfaces===true,scales:{count:x.scales?.count===2?2:3,low:limit(x.scales?.low,20,1,98),high:limit(x.scales?.high,40,2,99),width:limit(x.scales?.width,20,1,100),colors:[hex(x.scales?.colors?.[0],'#df6068'),hex(x.scales?.colors?.[1],'#e0bc50'),hex(x.scales?.colors?.[2],'#67bc89')]}};
  }
  function category(metric,m={}){if(metric==='solarEnergy'||metric==='yieldWh')return 'solar';if(m.resourceCategory)return m.resourceCategory;if(m.resource)return m.resource;if(/^roller|^scooter/.test(metric))return 'scooter';if(/^isdt|^charger/.test(metric))return 'model';if(m.calculated||m.flow||m.estimated||m.modelPower||/Energy$|chargeTime/.test(metric))return 'calculated';if(m.kind==='solar'||m.group==='Solar')return 'solar';if(m.kind==='shunt'||m.group==='Akku')return 'storage';return 'system';}
  function metricColor(a,metric,m){return a.metricColors?.[metric]||a.colors[category(metric,m)]||a.colors.system;}
  function availableSurplus(batteryPower,solarPower,loadPower=0,externalPower=0){if(![batteryPower,solarPower,loadPower,externalPower].every(finite))return null;return Math.min(Math.max(0,Number(solarPower)),Number(batteryPower)+Math.max(0,Number(loadPower))-Math.max(0,Number(externalPower)));}
  function mix(a,b,t){return '#'+[1,3,5].map(i=>Math.round(parseInt(a.slice(i,i+2),16)+(parseInt(b.slice(i,i+2),16)-parseInt(a.slice(i,i+2),16))*t).toString(16).padStart(2,'0')).join('');}
  function scaleColor(s,p){s=appearance({scales:s}).scales;p=limit(p,0,0,100);const low=s.low,high=Math.max(low+.01,s.high),half=Math.min(s.width/2,low,100-high,s.count===2?100:(high-low)/2),c=s.colors;
    if(p>=low-half&&p<=low+half)return mix(c[0],s.count===2?c[2]:c[1],(p-low+half)/(2*half));
    if(s.count===3&&p>=high-half&&p<=high+half)return mix(c[1],c[2],(p-high+half)/(2*half));
    return p<low?c[0]:s.count===3&&p<high?c[1]:c[2];
  }
  function dashboardDesign(x={}){const d={enabled:x.enabled===true};for(const k of ['app','tile'])if(x[k])d[k]=surface(x[k]);for(const k of ['accent','text','value'])if(hex(x[k],null))d[k]=hex(x[k],null);return d;}
  function pageAppearance(global,page){const a=appearance(global),d=dashboardDesign(page?.design);if(!d.enabled)return a;for(const k of ['app','tile','accent'])if(d[k])a[k]=d[k];if(d.app||d.tile)a.customSurfaces=true;if(d.value){a.colors=Object.fromEntries(Object.keys(a.colors).map(k=>[k,d.value]));a.metricColors={};}return a;}
  function normalize(x={},legacy={}){
    const pages=PAGES.map(([id,name])=>({id,name,builtin:true,board:x.pages?.find(p=>p.id===id)?.board|| (id==='overview'?legacy:null),design:dashboardDesign(x.pages?.find(p=>p.id===id)?.design)}));
    for(const p of x.pages||[])if(!PAGES.some(([id])=>id===p.id)&&key(p.id)&&typeof p.name==='string'&&p.name.trim()&&!pages.some(q=>q.id===p.id)&&pages.length<40)pages.push({id:p.id,name:p.name.trim().slice(0,60),builtin:false,board:p.board||null,design:dashboardDesign(p.design)});
    const ids=new Set(pages.map(p=>p.id)),navigation=Array.isArray(x.navigation)?[...new Set(x.navigation.filter(id=>ids.has(id)))]:['overview','storage','supplies','history'];
    return {version:1,pages,navigation};
  }
  function createPage(workspace,name,board,id){if(!key(id)||workspace.pages.some(p=>p.id===id)||!String(name).trim()||workspace.pages.length>=40)throw Error('Bitte einen eindeutigen Namen für das Dashboard wählen.');return normalize({...workspace,pages:[...workspace.pages,{id,name:String(name).trim().slice(0,60),board:clone(board)}]});}
  function deletePage(w,id){if(w.pages.find(p=>p.id===id)?.builtin)throw Error('Dieser Bereich bleibt erhalten. Du kannst seine Kurzwahl entfernen.');return normalize({...w,pages:w.pages.filter(p=>p.id!==id),navigation:w.navigation.filter(p=>p!==id)});}
  function chargeSettings(x={}){return {enabled:x.enabled===true,mode:['live','forecast','both'].includes(x.mode)?x.mode:'both',target:limit(x.target,85,1,100),reserve:limit(x.reserve,35,0,100),houseTarget:limit(x.houseTarget,100,0,100),forecastReserveWh:limit(x.forecastReserveWh,300,0,100000),startW:limit(x.startW,150,0,10000),stopW:limit(x.stopW,100,0,10000),delaySeconds:limit(x.delaySeconds,120,0,3600),powerW:limit(x.powerW,140,1,5000),efficiency:limit(x.efficiency,.9,.1,1)};}
  function chargeDecision(settings,input={},state={},now=Date.now()){
    const s=chargeSettings(settings),stop=reason=>({allow:false,reason,since:null});
    if(!s.enabled)return stop('Automatik aus');if(input.assignment!=='scooter')return stop('Lastausgang nicht dem E-Scooter zugeordnet');
    if(state.manualOff)return stop('Manuell ausgeschaltet');if(!finite(input.at)||now-input.at>45000||now<input.at)return stop('Aktuelle Messwerte fehlen');
    if(!finite(input.scooterSoc)||!finite(input.houseSoc))return stop('Akkudaten fehlen');if(input.scooterSoc>=s.target)return stop('Ladeziel erreicht');if(input.houseSoc<=s.reserve)return stop('Wohnwagenreserve schützen');
    if(s.stopW>=s.startW&&s.mode!=='forecast')return stop('Stoppschwelle muss unter der Startschwelle liegen');
    if(s.mode!=='live'&&(!finite(input.forecastWh)||input.forecastWh<s.forecastReserveWh+s.powerW/ s.efficiency/12))return stop('Prognosereserve schützen');
    if(s.mode!=='forecast'&&(!finite(input.surplusW)||input.surplusW<(state.charging?s.stopW:s.startW)))return stop('Wartet auf Solarüberschuss');
    const since=finite(state.since)&&state.since<=now?state.since:now;if(!state.charging&&now-since<s.delaySeconds*1000)return {allow:false,reason:'Überschuss bestätigen',since};
    return {allow:true,reason:'Ladung freigegeben',since,expires:now+60000,target:s.target,reserve:s.reserve};
  }
  function phases(points,from,to){
    const rows=points.filter(p=>finite(p.t)&&finite(p.v)&&Number.isInteger(Number(p.v))&&p.t>=from&&p.t<to).sort((a,b)=>a.t-b.t),segments=[];
    let cursor=from;const push=(a,b,state)=>{if(b<=a)return;const last=segments.at(-1);if(last&&last.state===state&&last.to===a)last.to=b;else segments.push({from:a,to:b,state});};
    for(let i=0;i<rows.length;i++){const p=rows[i],end=Math.min(to,p.t+Math.max(45000,(p.period||0)*1500),rows[i+1]?.t??to);if(p.t>cursor)push(cursor,p.t,'unknown');if(end>cursor){push(Math.max(cursor,p.t),end,p.v);cursor=end;}}
    if(cursor<to)push(cursor,to,'unknown');return segments;
  }
  return {dashboardDesign,pageAppearance,availableSurplus,surfaceBase,textOn,phases,CATEGORIES,PAGES,clone,appearance,surface,surfaceCss,category,metricColor,scaleColor,normalize,createPage,deletePage,chargeSettings,chargeDecision};
});
