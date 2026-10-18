// Original procedural effects. No network, samples or dependencies.
const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, '../app/src/main/assets/audio');
fs.mkdirSync(dir, {recursive:true});
const rate = 22050;
let seed = 19;
function noise() { seed = (Math.imul(seed,1664525)+1013904223)>>>0; return seed/2147483648-1; }
function wav(name, duration, fn) {
  const n = Math.floor(duration*rate), b = Buffer.alloc(44+n*2);
  b.write('RIFF'); b.writeUInt32LE(b.length-8,4); b.write('WAVEfmt ',8);
  b.writeUInt32LE(16,16); b.writeUInt16LE(1,20); b.writeUInt16LE(1,22);
  b.writeUInt32LE(rate,24); b.writeUInt32LE(rate*2,28); b.writeUInt16LE(2,32);
  b.writeUInt16LE(16,34); b.write('data',36); b.writeUInt32LE(n*2,40);
  for(let i=0;i<n;i++) {
    const t=i/rate, fade=Math.min(1,t/.003,(duration-t)/.012);
    b.writeInt16LE(Math.round(Math.max(-1,Math.min(1,fn(t)*fade)) * 27000),44+i*2);
  }
  fs.writeFileSync(path.join(dir,name+'.wav'),b);
}
// Low chest-like effort: falling fundamental, restrained harmonics so the low
// pitch is still heard on a phone speaker, plus a soft filtered breath transient.
function effort(base, drop, decay) {
  let breath=0;
  return t => {
    breath=.82*breath+.18*noise();
    const phase=2*Math.PI*(base*t+drop*(1-Math.exp(-24*t))/24);
    const voice=.57*Math.sin(phase)+.23*Math.sin(2*phase)+.09*Math.sin(3*phase);
    return (voice+.25*breath)*(1-Math.exp(-180*t))*Math.exp(-decay*t);
  };
}
// New default: brief clay-floor stomp followed by a softer body contact.
function stomp(base,heavy=false) {
  let dust=0;
  return t => {
    dust=.68*dust+.32*noise();
    const phase=2*Math.PI*(base*t+50*(1-Math.exp(-42*t))/42);
    const body=(.58*Math.sin(phase)+.2*Math.sin(2*phase))*(1-Math.exp(-250*t))*Math.exp(-(heavy?19:27)*t);
    const touch=t>.027 ? dust*.32*Math.exp(-47*(t-.027)) : 0;
    return body+touch;
  };
}
function drum(base,decay) {
  let skin=0;
  return t => {
    skin=.76*skin+.24*noise();
    const phase=2*Math.PI*(base*t+32*(1-Math.exp(-30*t))/30);
    return (.6*Math.sin(phase)+.18*Math.sin(phase*1.96)+.14*skin)*(1-Math.exp(-210*t))*Math.exp(-decay*t);
  };
}
wav('push',.15,stomp(115));wav('impact',.20,stomp(94,true));
wav('drum',.17,drum(88,22));wav('drum-impact',.22,drum(72,18));
wav('grunt',.17,effort(125,36,22));wav('grunt-impact',.22,effort(102,38,18));
wav('tick',.09,t => .5*Math.sin(2*Math.PI*620*t)*Math.exp(-35*t));
wav('win',.48,t => {
  const f=t<.12?523.25:t<.24?659.25:783.99;
  return .45*Math.sin(2*Math.PI*f*t)*Math.exp(-3*(t%.12));
});
console.log('Generated eight original PCM WAV effects (three click styles, countdown, result).');
