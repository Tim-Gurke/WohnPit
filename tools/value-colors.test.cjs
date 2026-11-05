const {test}=require('node:test'),assert=require('node:assert/strict');
const C=require('../app/src/main/assets/dashboard-core.js'),B=require('../app/src/main/assets/settings-backup.js');
test('central metric colors reach inherited gauges and numbers while custom tiles survive',()=>{
 const board=C.defaults(),before=structuredClone(board.tiles.map(t=>t.layouts));
 board.tiles.push({...C.tile('waterStock','custom-water'),colorSource:'custom',color:'#112233'});
 for(const key of Object.keys(C.CATALOG))if(C.CATALOG[key].resource==='water')board.valueColors[key]='#314159';
 const saved=C.normalize(board);assert.ok(saved.tiles.filter(t=>t.metric.startsWith('water')&&t.id!=='custom-water').every(t=>t.color==='#314159'));
 assert.equal(saved.tiles.at(-1).color,'#112233');assert.deepEqual(saved.tiles.slice(0,-1).map(t=>t.layouts),before);
 assert.equal(C.metricColor(saved,'waterDaily'),'#314159');assert.equal(C.metricColor(saved,'gasRange'),'#c2c200');
 assert.deepEqual(C.normalize(JSON.parse(JSON.stringify(saved))),saved);
});
test('legacy explicit colors are retained and invalid registry entries cannot enter CSS',()=>{
 const old={version:1,energyFlowVersion:1,lanTileVersion:1,dischargeSignVersion:1,tiles:[{...C.tile('solar'),colorSource:undefined,color:'#123456'}]};
 old.valueColors={solar:'url(https://example.invalid/)',gasStock:'#ABCDEF',unknown:'#123456'};
 const board=C.normalize(old);assert.equal(board.tiles[0].colorSource,'custom');assert.equal(board.tiles[0].color,'#123456');
 assert.equal(board.valueColors.solar,'#e8bb57');assert.equal(board.valueColors.gasStock,'#abcdef');assert.equal(board.valueColors.unknown,undefined);
});
test('the approved preset contains both uploaded layouts, scales and the exact supply colors',()=>{
 const board=C.defaults();assert.equal(board.tiles.length,20);
 const w=board.tiles.find(t=>t.metric==='waterStock'),g=board.tiles.find(t=>t.metric==='gasStock');
 assert.deepEqual(w.layouts,{compact:{x:0,y:31.5,w:4,h:6},wide:{x:0,y:35,w:4,h:4}});assert.equal(w.max,40);
 assert.deepEqual(g.layouts,{compact:{x:0,y:37.5,w:4,h:6},wide:{x:0,y:51,w:4,h:4}});assert.equal(g.max,11);
 for(const t of board.tiles.filter(t=>C.CATALOG[t.metric].resource))assert.equal(t.color,C.CATALOG[t.metric].resource==='water'?'#4c65e0':'#c2c200');
 assert.equal(board.tiles.find(t=>t.metric==='solar').color,'#e8bb57');assert.equal(board.tiles.find(t=>t.metric==='soc').color,'#65cbb0');assert.equal(board.tiles.find(t=>t.metric==='energyBalance').color,'#81baf4');
 const snapshot={settings:{theme:'dark',palette:'classic',capacity:100,sampleSeconds:10,miniRange:'today'}};
 board.valueColors.waterStock='#214365';const backup=B.create(snapshot,C.normalize(board),{version:1,groups:[]});assert.deepEqual(B.normalize(JSON.parse(JSON.stringify(backup))).dashboard,backup.dashboard);
});
