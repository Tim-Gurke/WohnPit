/* Portable view operations and bounded arithmetic. No commands or dynamic code execution. */
(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.AutarPieUpdate=api;})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const copy=x=>JSON.parse(JSON.stringify(x)),finite=x=>x!==null&&x!==''&&Number.isFinite(Number(x));
 const COLOR_KEYS=['color','colorSource','colorMid','colorEnd','adaptiveColor','multicolor','invert','background','backgroundSurface','opacity','scaleSource'];
 const VIEW_PARTS={colors:'Farben & Design',selection:'Kachelauswahl',layout:'Kachelgrößen & Anordnung',dashboards:'Dashboards & Ansichtseinstellungen'};
 function formula(source,values={},validateOnly=false){
  source=String(source||'').trim().replace(/^=/,'');if(!source||source.length>1000)throw Error('Die Formel muss zwischen 1 und 1000 Zeichen enthalten.');
  const tokens=[];let pos=0;
  while(pos<source.length){if(/\s/.test(source[pos])){pos++;continue;}const rest=source.slice(pos),m=rest.match(/^(?:\d+(?:[.,]\d*)?|[.,]\d+)(?:e[+-]?\d+)?/i)||rest.match(/^[A-Za-z_][A-Za-z_0-9]*/)||rest.match(/^(?:<=|>=|<>|!=|==|[+\-*/^();<>=])/);if(!m)throw Error('Ungültiges Zeichen an Stelle '+(pos+1)+'.');tokens.push(m[0]);pos+=m[0].length;if(tokens.length>256)throw Error('Die Formel ist zu lang.');}
  let i=0,depth=0;const prec={'=':1,'==':1,'!=':1,'<>':1,'<':1,'>':1,'<=':1,'>=':1,'+':2,'-':2,'*':3,'/':3,'^':4};
  function primary(){if(++depth>32)throw Error('Zu viele verschachtelte Klammern.');let t=tokens[i++],node;
   if(t==='+'||t==='-')node={op:'sign',sign:t,arg:expr(4)};
   else if(t==='('){node=expr(0);if(tokens[i++]!==')')throw Error('Schließende Klammer fehlt.');}
   else if(t&&/^(?:\d|[.,]\d)/.test(t))node={number:Number(t.replace(',','.'))};
   else if(t&&/^[A-Za-z_]/.test(t)){t=t.toUpperCase();if(tokens[i]==='('){i++;const args=[];if(tokens[i]!==')'){do{args.push(expr(0));if(tokens[i]!==';')break;i++;}while(true);}if(tokens[i++]!==')')throw Error('Funktionsargumente mit Semikolon trennen.');if(!['MIN','MAX','ABS','BETRAG','ROUND','RUNDEN','IF','WENN'].includes(t))throw Error('Unbekannte Funktion: '+t);node={fn:t,args};}else node={variable:t};}
   else throw Error('Wert oder Klammer an Stelle '+i+' erwartet.');depth--;return node;
  }
  function expr(min){let left=primary();while(prec[tokens[i]]>=min){const op=tokens[i++],right=expr(prec[op]+(op==='^'?0:1));left={op,left,right};}return left;}
  const tree=expr(0);if(i!==tokens.length)throw Error('Unerwarteter Ausdruck an Stelle '+(i+1)+'.');
  function run(n){if('number'in n)return n.number;if(n.variable){const v=values[n.variable];if(!finite(v))throw Error('Messwert '+n.variable+' fehlt oder ist veraltet.');return Number(v);}if(n.fn){const a=n.args;if(['WENN','IF'].includes(n.fn)){if(a.length!==3)throw Error('WENN benötigt drei Argumente.');return run(a[0])?run(a[1]):run(a[2]);}const v=a.map(run);if(!v.length)throw Error('Funktionsargument fehlt.');switch(n.fn){case'MIN':return Math.min(...v);case'MAX':return Math.max(...v);case'ABS':case'BETRAG':if(v.length!==1)throw Error('BETRAG benötigt einen Wert.');return Math.abs(v[0]);case'ROUND':case'RUNDEN':if(v.length>2||!Number.isInteger(v[1]??0)||Math.abs(v[1]??0)>12)throw Error('RUNDEN: maximal zwölf Nachkommastellen.');return Math.round(v[0]*10**(v[1]??0))/10**(v[1]??0);}}
   if(n.op==='sign')return (n.sign==='-'?-1:1)*run(n.arg);const a=run(n.left),b=run(n.right);switch(n.op){case'+':return a+b;case'-':return a-b;case'*':return a*b;case'/':if(b===0)throw Error('Division durch null.');return a/b;case'^':return a**b;case'=':case'==':return +(a===b);case'!=':case'<>':return +(a!==b);case'<':return +(a<b);case'>':return +(a>b);case'<=':return +(a<=b);case'>=':return +(a>=b);}}
  if(validateOnly)return true;
  const result=run(tree);if(!Number.isFinite(result))throw Error('Das Ergebnis ist nicht berechenbar.');return result;
 }
 function calculation(x={}){const vars={};for(const [name,v]of Object.entries(x.variables||{}).slice(0,12)){if(!/^[A-L]$/.test(name)||!v||typeof v.metric!=='string'||v.metric==='formula')continue;vars[name]={metric:v.metric.slice(0,40),device:typeof v.device==='string'?v.device.slice(0,80):'',unit:typeof v.unit==='string'?v.unit.slice(0,20):'',cell:Math.max(1,Math.min(64,Number(v.cell)||1))};}return {expression:String(x.expression||'=A/B*100').slice(0,1000),variables:vars,factor:finite(x.factor)?Number(x.factor):1,offset:finite(x.offset)?Number(x.offset):0,unit:String(x.unit||'').slice(0,20)};}
 function calculate(config,resolve){const c=calculation(config),values={},sources=[];for(const [name,s]of Object.entries(c.variables)){const r=resolve(s);values[name]=r?.value??null;sources.push(r);}const value=formula(c.expression,values)*c.factor+c.offset;if(!Number.isFinite(value))throw Error('Das Ergebnis ist nicht berechenbar.');const times=sources.filter(s=>s?.at).map(s=>s.at);if(times.length>1&&Math.max(...times)-Math.min(...times)>10000)throw Error('Die Messwerte stammen aus unterschiedlichen Zeitpunkten.');return {value,estimated:sources.some(s=>s?.estimated),at:times.length?Math.min(...times):null};}
 function mergeView(current,saved,parts){
  const out=copy(current),take=new Set(parts),all=Object.keys(VIEW_PARTS).every(k=>take.has(k));
  if(all)return copy(saved);
  if(take.has('colors')&&saved.appearance)out.appearance=copy(saved.appearance);
  if(take.has('dashboards')&&saved.viewSettings)out.viewSettings=copy(saved.viewSettings);
  if(take.has('colors')&&saved.ownColors)out.ownColors=copy(saved.ownColors);
  const source=saved.workspace||{pages:[{id:'overview',board:saved.dashboard}],navigation:['overview']};
  if(!out.workspace)out.workspace={version:1,pages:[{id:'overview',board:out.dashboard}],navigation:['overview']};
  if(take.has('dashboards')){const existing=new Map(out.workspace.pages.map(p=>[p.id,p]));out.workspace.pages=source.pages.map(p=>({...copy(p),board:existing.get(p.id)?.board||copy(p.board),design:take.has('colors')?p.design:existing.get(p.id)?.design}));out.workspace.navigation=copy(source.navigation);}
  for(const page of out.workspace.pages){const from=source.pages.find(p=>p.id===page.id);if(!from)continue;if(take.has('colors')){if(from.design)page.design=copy(from.design);else delete page.design;}if(!page.board||!from.board)continue;
   const before=copy(page.board),byId=new Map(before.tiles.map(t=>[t.id,t]));
   if(take.has('selection'))page.board.tiles=from.board.tiles.map(t=>{const old=byId.get(t.id);if(!old)return copy(t);const next=copy(t);if(!take.has('layout'))next.layouts=old.layouts;if(!take.has('colors'))for(const k of COLOR_KEYS){if(k in old)next[k]=copy(old[k]);else delete next[k];}return next;});
   for(const tile of page.board.tiles){const t=from.board.tiles.find(t=>t.id===tile.id&&t.metric===tile.metric);if(!t)continue;if(take.has('layout'))tile.layouts=copy(t.layouts);if(take.has('colors'))for(const k of COLOR_KEYS){if(k in t)tile[k]=copy(t[k]);else delete tile[k];}}
   if(take.has('colors'))page.board.valueColors=copy(from.board.valueColors||{});
   if(take.has('layout'))for(const k of ['blockOrder','historyHeight','rollerLayout'])if(from.board[k])page.board[k]=copy(from.board[k]);
   if(take.has('dashboards'))for(const k of ['showMiniHistory','showRecording','supplyUnits'])if(k in from.board)page.board[k]=copy(from.board[k]);
  }
  out.dashboard=copy(out.workspace.pages.find(p=>p.id==='overview')?.board||out.dashboard);return out;
 }
 function formulaSeries(config,streams,convert=(v)=>v){
  const c=calculation(config),names=Object.keys(c.variables),times=[...new Set(names.flatMap(n=>(streams[n]?.points||[]).map(p=>p.t)))].sort((a,b)=>a-b),indices=Object.fromEntries(names.map(n=>[n,0])),points=[];let interrupted=true;
  for(const t of times){const values={};let gap=false,estimated=false,valid=true;
   for(const n of names){const rows=streams[n]?.points||[];let i=indices[n];while(i+1<rows.length&&rows[i+1].t<=t)i++;indices[n]=i;const a=rows[i],b=rows[i+1];let v=null;
    if(a?.t===t){v=a.v;gap=gap||(i>0&&a.gap===true);}
    else if(a&&b&&a.t<t&&b.t>t&&b.gap!==true&&(!a.device||!b.device||a.device===b.device)&&b.t-a.t<=Math.max(45000,(a.period||0)*1500,(b.period||0)*1500))v=a.v+(b.v-a.v)*(t-a.t)/(b.t-a.t);
    if(!finite(v)){valid=false;break;}values[n]=convert(v,c.variables[n]);estimated=estimated||!!streams[n]?.estimated;
   }
   if(!valid){interrupted=true;continue;}try{const v=formula(c.expression,values)*c.factor+c.offset;if(!Number.isFinite(v))throw Error();points.push({t,last:t,v,min:v,max:v,n:1,gap:interrupted||gap,estimated,kind:'formula',device:'formula',origin:'calculated'});interrupted=false;}catch(_){interrupted=true;}
  }
  const values=points.map(p=>p.v),summary=values.length?{last:values.at(-1),min:Math.min(...values),max:Math.max(...values),avg:values.reduce((a,b)=>a+b,0)/values.length,count:values.length}:{};
  return {points,summary};
 }
 return {formula,formulaSeries,calculation,calculate,mergeView,VIEW_PARTS,COLOR_KEYS};
});
