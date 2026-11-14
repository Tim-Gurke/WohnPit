(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.WattSpecialUseCore=api;})(typeof globalThis==='object'?globalThis:this,function(){
  'use strict';
  const formats=new Map(),H=3600000,DAY=24*H,finite=n=>typeof n==='number'&&Number.isFinite(n);
  const median=values=>{const a=values.filter(finite).sort((a,b)=>a-b),i=Math.floor(a.length/2);return a.length?a.length%2?a[i]:(a[i-1]+a[i])/2:null;};
  const defaults={version:1,profiles:[],events:[]};
  function normalize(input={}){
    if(input.version!=null&&input.version!==1)throw Error('Unbekannte Sondernutzungen.');
    const profiles=(input.profiles||[]).map(p=>({...p,kind:p.kind==='roller'?'roller':'device',enabled:p.enabled!==false,mode:p.mode==='manual'?'manual':'learn',whPerKm:p.whPerKm??19}));
    if(profiles.length>24||new Set(profiles.map(p=>p.id)).size!==profiles.length)throw Error('Ungültige Sondernutzungen.');
    for(const p of profiles){if(!/^[A-Za-z0-9_-]{1,70}$/.test(p.id)||typeof p.name!=='string'||!p.name.trim()||p.name.length>60)throw Error('Bitte eine gültige Bezeichnung eingeben.');for(const [key,max] of [['wh',100000],['watts',100000],['minutes',1440],['hour',23],['whPerKm',100000],['km',2000],['chargesPerWeek',70]])if(p[key]!=null&&(!finite(p[key])||p[key]<0||p[key]>max))throw Error('Bitte gültige Energie- und Zeitwerte eingeben.');}
    return {version:1,profiles,events:[...(input.events||[])],rollerDays:input.rollerDays||{},rollerLearning:input.rollerLearning||{}};
  }
  function profiles(config,roller){
    const all=normalize(config).profiles;
    if(roller?.profile?.mac&&!all.some(p=>p.id==='roller'))all.unshift({id:'roller',kind:'roller',name:roller.profile.name||'Roller',enabled:true,mode:'learn',wh:0,watts:138,minutes:30,hour:12,whPerKm:19,km:0});
    return all;
  }
  function dayAt(t,zone){try{const key='day:'+zone;if(!formats.has(key))formats.set(key,new Intl.DateTimeFormat('en-CA',{timeZone:zone,year:'numeric',month:'2-digit',day:'2-digit'}));return formats.get(key).format(new Date(t));}catch(_){return '';}}
  function hourAt(t,zone){try{const key='hour:'+zone;if(!formats.has(key))formats.set(key,new Intl.DateTimeFormat('en-GB',{timeZone:zone,hour:'2-digit',hourCycle:'h23'}));return Number(formats.get(key).format(new Date(t)));}catch(_){return null;}}
  function splitDays(from,to,zone,visit){let cursor=from;while(cursor<to){const day=dayAt(cursor,zone);let end=to;if(dayAt(to-1,zone)!==day){let low=cursor+1,high=to;while(low<high){const mid=Math.floor((low+high)/2);if(dayAt(mid,zone)===day)low=mid+1;else high=mid;}end=low;}visit(day,cursor,end);cursor=end;}}

  function legacyLearned(profile,config,roller,zone,now){
    const daily=new Map(),add=(key,wh)=>daily.set(key,(daily.get(key)||0)+wh);
    if(profile.kind==='roller'){
      for(const [day,value] of Object.entries(config.rollerDays||{}))if(value&&typeof value==='object'&&value.mac===roller?.profile?.mac&&value.millis>=20*H&&value.unassignedMillis<=60000&&finite(value.wh)&&value.wh>=0&&day<dayAt(now,zone)&&day>=dayAt(now-90*DAY,zone))daily.set(day,value.wh);
      const invalid=new Set(),journal=roller?.journal||[];for(const e of [...journal,...(roller?.session?.active?[roller.session]:[])]){const end=e.active?now:e.ended;if(!finite(e.started)||!finite(end)||end>now||end<=e.started||now-end>90*DAY)continue;if(e.active||!finite(e.observedMillis)||e.observedMillis/(e.observedMillis+(e.gapMillis||0))<.9)splitDays(e.started,end,zone,key=>invalid.add(key));}for(const key of invalid)daily.delete(key);

    }else{
      // Explicitly recorded zero-use days count; unobserved days never become zeroes.
      for(const e of config.events||[])if(e.profileId===profile.id&&finite(e.wh)&&e.wh>=0&&e.to<=now&&now-e.to<=90*DAY)splitDays(e.from,e.to,zone,(day,from,to)=>add(day,e.wh*(to-from)/(e.to-e.from)));
    }
    if(profile.kind==='roller'){const confirmed=new Map();for(const e of config.events||[])if(e.profileId===profile.id&&finite(e.wh)&&e.wh>=0&&e.to<=now&&now-e.to<=90*DAY)splitDays(e.from,e.to,zone,(day,from,to)=>confirmed.set(day,(confirmed.get(day)||0)+e.wh*(to-from)/(e.to-e.from)));for(const [day,wh] of confirmed)daily.set(day,wh);}
    const rows=[...daily.entries()].sort((a,b)=>a[0].localeCompare(b[0])).slice(-28),wh=rows.length>=3?median(rows.map(x=>x[1])):null;
    // A mean across known days preserves frequency, including confirmed non-use days.
    return {wh:wh===null?null:rows.reduce((s,x)=>s+x[1],0)/rows.length,days:rows.length};
  }
  function learned(profile,config,roller,zone,now){
    const previous=legacyLearned(profile,config,roller,zone,now);if(profile.kind!=='roller')return previous;
    const data=config.rollerLearning||{},mac=roller?.profile?.mac,matching=data.version===1&&mac&&data.mac===mac,seen=new Set(),charges=[];let end=0;
    for(const e of (matching?data.charges||[]:[]).slice().sort((a,b)=>a.from-b.from)){
      if(e.mac!==mac||seen.has(e.id)||!finite(e.from)||!finite(e.to)||e.to<=e.from||e.to>now||now-e.to>90*DAY||!finite(e.inputWh)||e.inputWh<=0||e.inputWh>100000||e.coverage<.98||e.from<end)continue;
      seen.add(e.id);charges.push(e);end=e.to;
    }
    const recent=charges.slice(-28),chargeWh=recent.length?recent.reduce((s,e)=>s+e.inputWh,0)/recent.length:null;
    const days=Object.entries(matching?data.pathDays||{}:{}).filter(([day,d])=>day<dayAt(now,zone)&&day>=dayAt(now-90*DAY,zone)&&d.mac===mac&&d.complete===true&&finite(d.dayMillis)&&d.dayMillis>=22*H&&d.dayMillis<=26*H&&d.millis>=d.dayMillis&&finite(d.wh)&&d.wh>=0).sort((a,b)=>a[0].localeCompare(b[0])).slice(-28);
    const detail={charges:recent.length,chargeWh,latest:recent.at(-1)?.to||0,monitoredDays:days.length,estimatedInput:true,provisional:recent.length<3,frequency:null,source:'unknown',excludedCharges:matching?data.excludedCharges||0:0};
    if(days.length>=3){const wh=days.reduce((s,[,d])=>s+d.wh,0)/days.length,n=days.reduce((s,[,d])=>s+(d.charges||0),0);return {...detail,wh,days:days.length,frequency:n>0||wh===0?n/days.length:null,source:'monitored',provisional:days.length<7};}
    if(previous.wh!==null)return {...detail,...previous,source:'known-days',provisional:previous.days<7};
    if(chargeWh!==null&&finite(profile.chargesPerWeek)&&profile.chargesPerWeek>0)return {...detail,wh:chargeWh*profile.chargesPerWeek/7,days:0,frequency:profile.chargesPerWeek/7,source:'assumed-frequency',provisional:true};
    const r={...roller?.profile},manual=profile.km>0?profile.km*(profile.whPerKm??19)*(1/r.conversionEfficiency+r.idleWatts/r.chargeWatts):profile.wh>0?profile.wh:null;
    if(finite(manual))return {...detail,wh:manual,days:0,source:'manual-fallback',provisional:true};
    return {...detail,wh:null,days:0,reason:chargeWh===null?'Noch keine ausreichend vollständig erfasste Wohnwagenladung.':'Bedarf je Ladung gelernt; die übliche Ladehäufigkeit ist noch unbekannt. Unter Sondernutzungen kann eine vorläufige Häufigkeit angegeben werden.'};
  }
  function learningText(value,zone='UTC'){
    const n=x=>finite(x)?x.toLocaleString('de-DE',{maximumFractionDigits:1}):'—',parts=[];
    if(value?.charges){parts.push(value.charges+' auswertbare Ladung'+(value.charges===1?'':'en')+' · ≈ '+n(value.chargeWh)+' Wh am Wohnwagen je Ladung · geschätzt'+(value.provisional?' · vorläufig':''));if(value.latest)try{parts.push('Letzte Ladung: '+new Date(value.latest).toLocaleDateString('de-DE',{timeZone:zone}));}catch(_){}}
    if(value?.source==='monitored')parts.push(value.monitoredDays+' vollständig überwachte Ladeweg-Tage'+(finite(value.frequency)?' · ≈ '+n(value.frequency*7)+' Ladungen/Woche':''));
    else if(value?.source==='known-days')parts.push(value.days+' bekannte Nutzungstage');
    else if(value?.source==='assumed-frequency')parts.push('Planungsannahme: '+n(value.frequency*7)+' Ladungen/Woche; Häufigkeit noch nicht gelernt');
    else if(value?.source==='manual-fallback')parts.push('Eigene Tages-/Kilometerschätzung, bis die Ladehäufigkeit gelernt ist');
    if(value?.excludedCharges)parts.push(value.excludedCharges+' unvollständige oder nicht zuordenbare Ladeaufzeichnung'+(value.excludedCharges===1?'':'en')+' nicht verwendet');
    return parts.join(' · ');
  }
  function build(days,config,roller,rollerCore,now=Date.now(),zone='UTC'){
    config=normalize(config);roller={...roller,profile:{...rollerCore.defaults,...roller?.profile}};const loads=[],byDay=new Map(),all=profiles(config,roller),learnedCache=new Map();
    for(const day of days){
      const timeline=day.timeline||[],start=timeline[0]?.from,end=timeline[timeline.length-1]?.to,rows=[],anchors=new Map();for(const h of timeline){const hour=hourAt(h.from,zone);if(!anchors.has(hour))anchors.set(hour,h.from);}if(!finite(start)||!finite(end))continue;
      for(const p of all.filter(p=>p.enabled)){
        const events=(config.events||[]).filter(e=>e.profileId===p.id&&e.to>now&&e.from<end&&e.to>start&&finite(e.wh)&&e.wh>=0);
        const charge=p.kind==='roller'&&rollerCore.plan(roller?.profile||{},roller?.fresh?roller.live:{},now),lastCharge=charge?(finite(charge.hours)?Math.max(H,charge.hours*H):(roller?.profile?.capacityAh||26)*(roller?.profile?.nominalVoltage||48.1)/(roller?.profile?.chargeWatts||96)*H):0,concrete=charge&&roller?.fresh&&(roller?.profile?.planCharge||roller?.session?.active)&&(charge.hours===0?day.day===dayAt(now,zone):start<now+lastCharge);
        let wh=null,basis='manual',reason='',count=0,intervals=[],learning=null;
        if(events.length){basis='planned';for(const e of events){const from=Math.max(now,start,e.from),to=Math.min(end,e.to);if(to>from)intervals.push({from,to,watts:e.wh*H/(e.to-e.from)});}wh=intervals.reduce((s,l)=>s+(l.to-l.from)/H*l.watts,0);}
        else if(concrete){basis='charge';if(!roller?.fresh){reason='Pro eingeplante Ladung fehlen aktuelle BMS-Werte.';}else{const r=rollerCore.plan(roller.profile,roller.live,now);if(r.away)wh=0;else if(r.impossible)reason='Das Ladeziel ist mit der hinterlegten Kapazität nicht erreichbar.';else if(r.late)reason='Die Ladung ist vor der geplanten Abfahrt nicht vollständig möglich.';else if(!finite(r.hours))reason='Der Ladebedarf ist noch unbekannt.';else{const from=Math.max(now,start),to=Math.min(end,now+r.hours*H);wh=0;if(to>from){intervals=[{from,to,watts:rollerCore.input(roller.profile.chargeWatts,roller.profile)}];wh=(to-from)/H*intervals[0].watts;}}}}
        else{
          let full;
          if(p.mode==='learn'){if(!learnedCache.has(p.id))learnedCache.set(p.id,learned(p,config,roller,zone,now));const estimate=learnedCache.get(p.id);full=estimate.wh;count=estimate.days;learning=p.kind==='roller'?estimate:null;basis=estimate.source==='assumed-frequency'?'assumed-frequency':estimate.source==='manual-fallback'?'manual-fallback':'learned';if(full===null)reason=(p.kind==='roller'&&!roller.fresh&&(roller.profile.planCharge||roller.session?.active)?'Aktuelle BMS-Werte fehlen. ':'')+(estimate.reason||'Noch nicht genügend beobachtete Nutzungstage (mindestens drei).');}
          else full=p.kind==='roller'&&p.km>0?p.km*(p.whPerKm??19)*(1/roller.profile.conversionEfficiency+roller.profile.idleWatts/roller.profile.chargeWatts):p.wh>0?p.wh:(p.watts||0)*(p.minutes||0)/60;
          if(finite(full)){const hour=Math.min(23,Math.max(0,p.hour??12)),anchor=anchors.get(hour)??start+hour*H,minutes=Math.max(1,p.minutes||30),finish=Math.min(end,anchor+minutes*60000),from=Math.max(now,start,anchor);wh=0;if(finish>from){intervals=[{from,to:finish,watts:full*H/(finish-anchor)}];wh=(finish-from)*full/(finish-anchor);}}
        }
        const row={id:p.id,name:p.name,wh,basis,reason,days:count,learning,learningText:learning?learningText(learning,zone):''};rows.push(row);for(const l of intervals)loads.push({...l,profileId:p.id,name:p.name});
      }
      byDay.set(day.day,{rows,wh:rows.some(r=>r.wh===null)?null:rows.reduce((s,r)=>s+r.wh,0),reason:rows.filter(r=>r.wh===null).map(r=>r.name+': '+r.reason).join(' ')});
    }
    return {profiles:all,loads,byDay};
  }
  return {defaults,normalize,profiles,learned,learningText,build};
});
