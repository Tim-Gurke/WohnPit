(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattSuppliesCore=api;})(typeof globalThis==='object'?globalThis:this,function(){
  'use strict';
  const HOUR=3600000,DAY=24*HOUR,finite=n=>typeof n==='number'&&Number.isFinite(n);
  const quantityUnits=kind=>kind==='gas'?['kg','Flaschen']:['L','Kanister'];
  function quantity(value,kind,unit,containerSize,inverse=false){
    if(!finite(value)||!quantityUnits(kind).includes(unit))return null;
    if(unit===quantityUnits(kind)[0])return value;
    return finite(containerSize)&&containerSize>0?(inverse?value*containerSize:value/containerSize):null;
  }
  function containerHint(kind,size){
    if(!finite(size)||size<=0)return kind==='gas'?'Flaschengröße unbekannt':'Kanistergröße unbekannt';
    return '1 '+(kind==='gas'?'Flasche':'Kanister')+' = '+size.toLocaleString('de-DE',{maximumFractionDigits:6})+' '+quantityUnits(kind)[0];
  }
  function normalize(input={}){
    if(input.version!=null&&input.version!==1)throw Error('Unbekanntes Gas-/Wasserformat.');
    const defaults={water:20,gas:11,waterContainer:20,...input.defaults},events=[...(input.events||[])].map((e,i)=>({...e,order:finite(e.order)?e.order:i}));
    if(events.length>5000||!Array.isArray(input.events||[]))throw Error('Ungültiges Versorgungsjournal.');
    for(const kind of ['water','gas'])if(!finite(defaults[kind])||defaults[kind]<=0||defaults[kind]>(kind==='water'?10000:100))throw Error('Bitte gültige Standardmengen eingeben.');
    if(!finite(defaults.waterContainer)||defaults.waterContainer<=0||defaults.waterContainer>10000)throw Error('Bitte eine gültige Kanistergröße eingeben.');
    const ids=new Set();for(const e of events){if(!/^[A-Za-z0-9_-]{8,80}$/.test(e.id)||ids.has(e.id)||!['water','gas'].includes(e.kind)||!['stock',e.kind==='gas'?'swap':'supply'].includes(e.type)||!finite(e.at)||!finite(e.amount)||e.amount<(e.type==='stock'?0:.01)||e.amount>(e.kind==='water'?10000:100))throw Error('Ungültiger Gas-/Wassereintrag.');ids.add(e.id);}
    return {...input,version:1,defaults,events:events.sort((a,b)=>a.at-b.at||a.order-b.order||a.id.localeCompare(b.id))};
  }
  function duration(hours){
    if(!finite(hours)||hours<0)return '—';if(hours===0)return '0 Stunden';if(hours<1)return 'Weniger als 1 Stunde';
    const total=Math.floor(hours),days=Math.floor(total/24),rest=total%24;
    return [days?days+(days===1?' Tag':' Tage'):'',rest?rest+(rest===1?' Stunde':' Stunden'):''].filter(Boolean).join(' ');
  }
  const mean=values=>values.length?values.reduce((sum,n)=>sum+n,0)/values.length:null;
  function weatherFeatures(rows,from,to,site){
    if(!site||to<=from)return null;let covered=0,heat=0,windHeat=0;
    for(const row of rows){if(row.site!==site||!finite(row.heat)||!finite(row.windHeat)||!finite(row.from)||!finite(row.to)||row.to<=row.from)continue;const span=Math.max(0,Math.min(to,row.to)-Math.max(from,row.from));if(!span)continue;covered+=span;heat+=span*row.heat;windHeat+=span*row.windHeat;}
    return covered/(to-from)>=.9?{heat:heat/covered,windHeat:windHeat/covered,coverage:covered/(to-from)}:null;
  }
  function historyRows(weather={}){
    const unique=new Map();for(const row of weather.history||[]){if(!['archive','recent'].includes(row.source))continue;const key=row.site+'/'+row.from,old=unique.get(key);if(!old||row.source==='archive'||old.source!=='archive')unique.set(key,row);}return [...unique.values()];
  }
  function intervals(events,kind,now,weather,sites=[]){
    const journal=events.filter(e=>e.kind===kind&&e.type===(kind==='gas'?'swap':'supply')&&e.at<=now),out=[];
    for(let i=1;i<journal.length;i++){const previous=journal[i-1],next=journal[i],days=(next.at-previous.at)/DAY;if(days<=0||now-next.at>400*DAY||kind==='gas'&&next.empty===false)continue;
      const amount=kind==='gas'?previous.amount:next.amount;
      // A stock correction inside a gas cycle makes its consumed amount unknown.
      if(kind==='gas'&&events.some(e=>e.kind===kind&&e.type==='stock'&&e.at>previous.at&&e.at<next.at))continue;
      const sameSite=!sites.some(s=>s.at>previous.at&&s.at<next.at&&s.site?.key!==previous.site?.key)&&previous.site?.key&&previous.site.key===next.site?.key&&!events.some(e=>e.site?.key&&e.at>previous.at&&e.at<next.at&&e.site.key!==previous.site.key);
      out.push({from:previous.at,to:next.at,days,amount,rate:amount/days,features:kind==='gas'&&sameSite?weatherFeatures(historyRows(weather),previous.at,next.at,previous.site.key):null});
    }
    return {journal,rows:out.slice(-24)};
  }
  function fit(rows,dimensions){
    const coefficients=Array(dimensions).fill(0),x=rows.map(r=>[1,r.features.heat,r.features.windHeat].slice(0,dimensions));coefficients[0]=rows.reduce((s,r)=>s+r.amount,0)/rows.reduce((s,r)=>s+r.days,0);
    for(let pass=0;pass<180;pass++)for(let j=0;j<dimensions;j++){let numerator=0,denominator=1e-6;for(let i=0;i<rows.length;i++){let other=0;for(let k=0;k<dimensions;k++)if(k!==j)other+=coefficients[k]*x[i][k];numerator+=rows[i].days*x[i][j]*(rows[i].rate-other);denominator+=rows[i].days*x[i][j]*x[i][j];}coefficients[j]=Math.max(0,numerator/denominator);}
    return coefficients;
  }
  const predict=(coefficients,features)=>coefficients.reduce((sum,c,i)=>sum+c*[1,features.heat,features.windHeat][i],0);
  function weatherModel(rows){
    const usable=rows.filter(r=>r.features&&finite(r.days)&&r.days>0&&finite(r.amount)&&r.amount>0);if(!usable.length)return null;
    const days=usable.reduce((sum,r)=>sum+r.days,0),daily=usable.reduce((sum,r)=>sum+r.amount,0)/days,heat=usable.reduce((sum,r)=>sum+r.days*r.features.heat,0)/days;
    // A cautious heating prior, anchored to the observed bottle use. One cycle
    // cannot identify personal heating/cooking coefficients or any wind effect.
    const slope=daily*.5/(heat+5),provisional={coefficients:[daily-slope*heat,slope],intervals:usable.length,wind:false,provisional:true};
    if(usable.length<3||Math.max(...usable.map(r=>r.features.heat))-Math.min(...usable.map(r=>r.features.heat))<3)return provisional;
    let best=null;for(const dimensions of [2,3]){
      if(dimensions===3&&usable.length<9)continue;let baseError=0,modelError=0;
      for(let i=0;i<usable.length;i++){const training=usable.filter((_,j)=>j!==i),baseline=training.reduce((s,r)=>s+r.amount,0)/training.reduce((s,r)=>s+r.days,0),estimated=predict(fit(training,dimensions),usable[i].features);baseError+=usable[i].days*(usable[i].rate-baseline)**2;modelError+=usable[i].days*(usable[i].rate-estimated)**2;}
      if(baseError>1e-8&&modelError<baseError*.85&&(!best||modelError<best.error*.95))best={coefficients:fit(usable,dimensions),error:modelError,intervals:usable.length,wind:dimensions===3};
    }return best||provisional;
  }
  const correctedRate=(profile,features)=>Math.min(profile.daily*(profile.model.provisional?1.6:5),Math.max(profile.daily*(profile.model.provisional?.65:.15),predict(profile.model.coefficients,features)));
  const freshForecast=(weather,now)=>weather.enabled!==false&&finite(weather.updated)&&now-weather.updated<30*HOUR;
  const forecastRows=weather=>(weather.forecast||[]).filter(r=>r.source==='forecast');
  function rateAt(profile,weather,from,to,currentSite,now){
    const features=weatherFeatures(forecastRows(weather),from,to,currentSite);
    if(profile.model&&features&&freshForecast(weather,now))return {rate:correctedRate(profile,features),weather:true};
    return {rate:profile.daily,weather:false};
  }
  function consumption(profile,weather,from,to,site,future=false){
    if(!finite(profile.daily)||profile.daily<=0||to<from)return null;let result=0,cursor=from;
    const rows=future?forecastRows(weather):historyRows(weather);let corrected=0;
    while(cursor<to){const end=Math.min(to,cursor+DAY),features=weatherFeatures(rows,cursor,end,site);let rate=profile.daily;
      if(profile.model&&features&&(!future||freshForecast(weather,from))){rate=correctedRate(profile,features);corrected+=end-cursor;}
      result+=rate*(end-cursor)/DAY;cursor=end;
    }return {amount:result,weather:corrected>0,coverage:(to>from?corrected/(to-from):0)};
  }
  function weatherStatus(profile,rows,weather,site,now){
    const usable=rows.filter(r=>r.features),count=usable.length,model=profile.model;
    const status={intervals:rows.length,weatherIntervals:count,temperatureMinimum:1,windMinimum:9,provisional:!!model?.provisional,model:model?(model.provisional?'temperature-provisional':model.wind?'temperature-wind':'temperature'):null};
    const baseline='Der übliche Tagesbedarf bleibt der Durchschnitt deiner abgeschlossenen Flaschenabschnitte.';
    let state,title,detail;
    if(!model){
      title='Wetterkorrektur noch nicht aktiv';
      if(!rows.length){state='no-intervals';detail='Noch kein vollständiger Flaschenabschnitt. Zwei Wechsel mit leerer vorheriger Flasche liefern den ersten Abschnitt.';}
      else if(!count){state='learning';detail='Für die erste vorläufige Temperaturkorrektur braucht ein vollständiger Flaschenabschnitt passende historische Wetterdaten; Wind wird später gesondert geprüft.';if(count<rows.length)detail+=' Für '+(rows.length-count)+' Abschnitte fehlt eine ausreichende Wetterzuordnung zum selben Ort.';}
      else if(Math.max(...usable.map(r=>r.features.heat))-Math.min(...usable.map(r=>r.features.heat))<3){state='similar-weather';detail='Genügend Abschnitte, aber noch zu ähnliche Temperaturen für ein verlässliches Wettermodell.';}
      else{state='not-improved';detail='Das Wettermodell erklärt deine bisherigen Verbräuche noch nicht zuverlässig besser als der Durchschnitt.';}
      detail+=' Bis dahin wird aus den Flaschenwechseln geschätzt.';
    }else if(profile.forecastWeatherCorrected||profile.rangeWeatherCorrected){
      state='active';title=model.provisional?'Wetterkorrektur vorläufig aktiv · Temperatur':'Wetterkorrektur aktiv · Temperatur'+(model.wind?' und Wind':'');detail=model.provisional?'Vorsichtige Modellannahme anhand von '+count+' vollständigen Flaschenabschnitten mit Wetterdaten. Noch kein persönlich gelernter Temperatureinfluss: die Korrektur bleibt auf −35 % bis +60 % des üblichen Tagesbedarfs begrenzt. Weitere unterschiedliche Wetterabschnitte verbessern das persönliche Modell.':'Bedarf und Reichweite verwenden das gelernte Modell, soweit passende Vorhersagetage vorliegen. Danach gilt der übliche Tagesbedarf.';
    }else{
      title=(model.provisional?'Vorläufiges Wettermodell':'Wettermodell gelernt')+' · Prognose ohne Wetterkorrektur';
      if(weather.enabled===false){state='disabled';detail='Wettervorhersagen sind ausgeschaltet.';}
      else if(!site){state='missing-site';detail='Der aktuelle Wetterort fehlt.';}
      else if(!freshForecast(weather,now)){state='stale';detail='Eine aktuelle Wettervorhersage fehlt. Bitte Wetter ergänzen.';}
      else{state='missing-forecast';detail='Für den aktuellen Ort und Prognosezeitraum fehlen ausreichend abgedeckte Vorhersagetage.';}
      detail+=' Bedarf und Reichweite verwenden den üblichen Tagesbedarf.';
    }
    if(profile.historyWeatherCorrected)detail+=' Der bisherige Verbrauch seit dem letzten bekannten Vorrat wurde mit Wetterrückblick korrigiert.';
    return {...status,state,title,detail,baseline};
  }
  function build(input={},now=Date.now()){
    const config=normalize(input),weather=config.weather||{},result={};
    for(const kind of ['water','gas']){
      const {journal,rows}=intervals(config.events,kind,now,weather,config.sites||[]),days=rows.reduce((s,r)=>s+r.days,0),amount=rows.reduce((s,r)=>s+r.amount,0),daily=days>0?amount/days:null,model=kind==='gas'?weatherModel(rows):null;
      const profile={kind,daily,model,containerSize:kind==='gas'?config.defaults.gas:config.defaults.waterContainer,intervals:rows.length,intervalHours:mean(rows.map(r=>r.days*24)),lastAt:journal.at(-1)?.at??null,stock:null,remainingHours:null,rangeHours:null,nextSupplyAt:null,needs:{},weatherCorrected:false,historyWeatherCorrected:false,forecastWeatherCorrected:false,rangeWeatherCorrected:false};
      if(profile.lastAt!==null&&finite(profile.intervalHours))profile.nextSupplyAt=profile.lastAt+profile.intervalHours*HOUR;
      const anchors=config.events.filter(e=>e.kind===kind&&e.at<=now&&(e.type==='stock'||kind==='gas'&&e.type==='swap')),anchor=anchors.at(-1),site=weather.siteKey||anchor?.site?.key;
      if(anchor){let stock=anchor.amount;if(finite(daily)&&daily>0){const moved=(config.sites||[]).some(s=>s.at>anchor.at&&s.at<=now&&s.site?.key!==anchor.site?.key);const used=consumption(moved?{...profile,model:null}:profile,weather,anchor.at,now,anchor.site?.key);stock-=used.amount;profile.historyWeatherCorrected=used.weather;}else if(now-anchor.at>HOUR)stock=null;
        if(stock!==null){if(kind==='water')for(const e of journal)if(e.at>anchor.at||e.at===anchor.at&&e.order>anchor.order)stock+=e.amount;profile.stock=Math.max(0,stock);profile.stockEstimated=now>anchor.at;}}
      if(finite(daily)&&daily>0){
        for(const horizon of [3,7,14]){const need=consumption(profile,weather,now,now+horizon*DAY,site,true);profile.needs[horizon]=need.amount;profile.forecastWeatherCorrected=profile.forecastWeatherCorrected||need.weather;}
        if(profile.stock!==null){let remaining=profile.stock,hours=0;while(remaining>0&&hours<24*1096){const rate=rateAt(profile,weather,now+hours*HOUR,now+(hours+1)*HOUR,site,now);if(rate.rate<=0)break;profile.rangeWeatherCorrected=profile.rangeWeatherCorrected||rate.weather;const used=rate.rate/24;if(remaining<=used){hours+=remaining/used;remaining=0;break;}remaining-=used;hours++;}if(remaining<=0){profile.remainingHours=Math.round(hours*1e6)/1e6;profile.emptyAt=now+hours*HOUR;}
          const deviation=Math.max((rows.length<3||model?.provisional)?.5:.25,Math.min(.8,Math.sqrt(mean(rows.map(r=>(r.rate-daily)**2))||0)/daily));if(finite(profile.remainingHours))profile.rangeHours=[profile.remainingHours/(1+deviation),profile.remainingHours/Math.max(.2,1-deviation)];
        }
      }
      profile.weatherCorrected=profile.historyWeatherCorrected||profile.forecastWeatherCorrected||profile.rangeWeatherCorrected;
      if(kind==='gas')profile.weatherStatus=weatherStatus(profile,rows,weather,site,now);
      profile.basis=rows.length===0?'Noch kein vollständiger Nutzungsabschnitt':rows.length<3?'Vorläufig · '+rows.length+' Abschnitt'+(rows.length===1?'':'e'):rows.length+' Nutzungsabschnitte';
      if(kind==='water'&&rows.length)profile.basis+=' · aus Beschaffungen geschätzt';
      if(kind==='gas'&&model)profile.basis+=model.provisional?' · Temperatur vorläufig berücksichtigt':' · Temperatur'+(model.wind?' und Wind':'')+' gelernt';
      profile.hint=profile.basis+(profile.stock===null?' · Vorrat unbekannt':'');result[kind]=profile;
    }return result;
  }
  return {HOUR,DAY,normalize,duration,quantityUnits,quantity,containerHint,weatherFeatures,historyRows,intervals,weatherModel,consumption,build};
});
