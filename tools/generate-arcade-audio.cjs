// Original synthesis; leaves the established sumo samples untouched.
const fs=require('fs'),path=require('path');const rate=22050;
function wav(name,duration,f,loop=false){let n=Math.floor(duration*rate),b=Buffer.alloc(44+n*2);b.write('RIFF');b.writeUInt32LE(b.length-8,4);b.write('WAVEfmt ',8);b.writeUInt32LE(16,16);b.writeUInt16LE(1,20);b.writeUInt16LE(1,22);b.writeUInt32LE(rate,24);b.writeUInt32LE(rate*2,28);b.writeUInt16LE(2,32);b.writeUInt16LE(16,34);b.write('data',36);b.writeUInt32LE(n*2,40);for(let i=0;i<n;i++){let t=i/rate,fade=loop?1:Math.min(1,t/.002,(duration-t)/.015);b.writeInt16LE(Math.round(Math.tanh(f(t))*fade*26000),44+i*2);}fs.writeFileSync(path.join(__dirname,'../app/src/main/assets/audio',name+'.wav'),b);}
const sin=(hz,t)=>Math.sin(2*Math.PI*hz*t);
for(const [name,hz,decay] of [['rebound',370,35],['cue',1350,48],['chalk',950,65],['puck',630,30],['flipper',155,22],['checkpoint',740,12],['stone',1850,55],['cannon',65,18]])wav(name,.24,t=>(sin(hz,t)*.65+sin(hz*2.31,t)*.22)*Math.exp(-decay*t));
let seed=41,low=0;const noise=()=>{seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed/2147483648-1;};
wav('chalk',.13,t=>(.6*noise()+.12*sin(950,t))*Math.exp(-32*t));
wav('cannon',.32,t=>{low=.82*low+.18*noise();return (.85*sin(55+80*Math.exp(-30*t),t)+1.3*low)*Math.exp(-15*t);});
wav('rebound',.15,t=>(sin(510-900*t,t)*.7+.15*sin(1020,t))*Math.exp(-25*t));
wav('checkpoint',.29,t=>sin(t<.13?660:990,t)*.55*Math.exp(-5*t));
wav('clash',.25,t=>(sin(2100,t)+.4*sin(3190,t))*Math.exp(-25*t));
wav('engine',1,t=>(.40*sin(60,t)+.20*sin(120,t)+.14*sin(240,t)+.07*sin(360,t)+.03*sin(480,t))*(.85+.15*Math.cos(2*Math.PI*13*t)),true);

let gravel=0;wav('rubble',.3,t=>{gravel=.55*gravel+.45*noise();return (.55*sin(95,t)*Math.exp(-24*t)+.65*gravel*Math.exp(-16*t));});
