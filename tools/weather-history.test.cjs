const {test}=require('node:test'),assert=require('node:assert/strict'),H=require('../app/src/main/assets/weather-history.js');
const time=s=>Date.parse(s),now=time('2026-11-05T12:00:00Z'),key='example-site';
const row=(day,source='archive')=>({site:key,from:time(day+'T00:00:00Z'),to:time(day+'T00:00:00Z')+86400000,temperature:8,wind:14,heat:10,windHeat:4,source});
const fixture=()=>({events:[{at:time('2026-11-02T12:00:00Z'),site:{key,name:'Beispielort'}}],weather:{timezone:'UTC',updated:now,siteKey:key,place:{name:'Beispielort'},history:[row('2026-11-02'),row('2026-11-03','recent'),row('2026-11-04')]}});
test('historical coverage counts actual stored days and archive/recent duplicates only once',()=>{
 const f=fixture();f.weather.history.push(row('2026-11-02','recent'));const result=H.coverage(f,now);
 assert.equal(result.state,'complete');assert.equal(result.available,3);assert.equal(result.required,3);assert.equal(result.stored,3);assert.equal(result.sites[0].from,'2026-11-02');
});
test('a current retrieval timestamp, forecasts and records for another place never hide a missing day',()=>{
 const f=fixture();f.weather.history[1].source='forecast';f.weather.history.push({...row('2026-11-03'),site:'wrong-place'});f.weather.forecast=[row('2026-11-03')];
 const result=H.coverage(f,now);assert.equal(result.state,'incomplete');assert.equal(result.available,2);assert.deepEqual(result.sites[0].missing,['2026-11-03']);
});
test('invalid/incomplete records and today do not count; errors and pending stay visible',()=>{
 const f=fixture();delete f.weather.history[0].wind;f.weather.history.push(row('2026-11-05'));f.weather.error='Abruf fehlgeschlagen';f.weather.pending=true;
 const result=H.coverage(f,now);assert.equal(result.stored,2);assert.equal(result.missing,1);assert.equal(result.pending,true);assert.equal(result.error,'Abruf fehlgeschlagen');
});
test('missing timezone, missing event location and no journal cannot report a completed backfill',()=>{
 const f=fixture();delete f.weather.timezone;assert.equal(H.coverage(f,now).state,'unverified');
 f.weather.timezone='UTC';delete f.events[0].site;assert.equal(H.coverage(f,now).state,'unassigned');
 f.events=[];assert.equal(H.coverage(f,now).state,'idle');
});
test('DST days count as one local day instead of fixed 24-hour buckets',()=>{
 const f={events:[{at:time('2026-10-25T00:00:00Z'),site:{key}}],weather:{timezone:'Europe/Berlin',history:[{...row('2026-10-25'),from:time('2026-10-24T22:00:00Z'),to:time('2026-10-25T23:00:00Z')}]}};
 const result=H.coverage(f,time('2026-10-26T12:00:00Z'));assert.equal(result.state,'complete');assert.equal(result.required,1);assert.equal(result.stored,1);
});
