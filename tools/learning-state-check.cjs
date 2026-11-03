/* Compare the migration through the unchanged Watt’n’Pit 1.8.3 display calculation. */
const fs=require('node:fs');
const assert=require('node:assert/strict');
const crypto=require('node:crypto');
const path=require('node:path');
for(const [file,sha] of Object.entries({'solar-core.js':'68e9b1a337f484eace1a505387be852d6ad7df18','presence-core.js':'b73dd872e084e1523ad520060ec91c15279abe85','equipment.js':'0aa61994c073d0e900347d335cfa07f8fcd2f31f'})){
  const data=fs.readFileSync(path.join(__dirname,'reference',file));assert.equal(crypto.createHash('sha1').update('blob '+data.length+'\0').update(data).digest('hex'),sha);
}
console.log('PASS the display reference is byte-identical to the pinned Watt’n’Pit 1.8.3 calculation');
const C=require('./reference/solar-core.js');
const fixture=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const day=C.stamp(fixture.now,'UTC').day;
const usage=(state,mode='home')=>C.consumption(fixture.setup,state.observations,day,fixture.now,mode);
const original=usage(fixture.original),before=usage(fixture.before);
assert.equal(original.usedHours,504);assert.equal(before.usedHours,120);
console.log('PASS unchanged display calculation reproduces 504 old learned hours versus only 120 recent hours after the incomplete migration');
for(const target of ['central','s20','s10']){
  const restored=usage(fixture[target]);
  assert.equal(restored.usedHours,original.usedHours);
  assert.ok(Math.abs(restored.wh-original.wh)<1e-8);
  assert.deepEqual(restored.slotStats.map(x=>[x.hours,x.days]),original.slotStats.map(x=>[x.hours,x.days]));
  console.log('PASS '+target+' restores the same learned hours, daily demand and seasonal slot evidence as the original display');
  assert.equal(usage(fixture[target],'away').usedHours,24);
  console.log('PASS '+target+' retains the separate learned absence profile');
  const calibration=state=>C.calibration(state.observations,state.forecasts,fixture.solarKey,fixture.systemKey,fixture.now);
  assert.deepEqual(calibration(fixture[target]),calibration(fixture.original));
  console.log('PASS '+target+' retains the original yield-calibration evidence and forecast correction');
}
