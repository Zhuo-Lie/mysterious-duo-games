const fs=require('fs');const assert=require('assert');const path=require('path');
for(const name of ['push','impact','drum','drum-impact','grunt','grunt-impact','tick','win','rebound','cue','chalk','puck','flipper','checkpoint','stone','cannon','clash','engine','rubble']){
  const b=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/audio',name+'.wav'));
  assert.equal(b.toString('ascii',0,4),'RIFF');assert.equal(b.toString('ascii',8,12),'WAVE');
  assert.equal(b.readUInt32LE(4)+8,b.length);assert.equal(b.readUInt16LE(20),1);
  assert.equal(b.readUInt16LE(22),1);assert.equal(b.readUInt32LE(24),22050);assert.equal(b.readUInt16LE(34),16);
  assert.equal(b.readUInt32LE(40)+44,b.length);
  let peak=0;for(let i=44;i<b.length;i+=2)peak=Math.max(peak,Math.abs(b.readInt16LE(i)));
  assert(peak>1000&&peak<32000,'audible sample without digital clipping');
  assert.equal(b.readInt16LE(44),0);assert(Math.abs(b.readInt16LE(b.length-2))<(name==='engine'?1000:200),'faded or continuous loop endpoint');
}
console.log('19 PCM audio assets passed format, level, clipping and endpoint checks.');
