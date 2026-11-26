// Ajusta o projeto Android gerado: permissoes, servicos, icone e plugins nativos
const fs=require('fs'),path=require('path');
const main=path.join('android','app','src','main');
const res=path.join(main,'res'),man=path.join(main,'AndroidManifest.xml');
let m=fs.readFileSync(man,'utf8');

// permissoes
for(const p of['RECORD_AUDIO','MODIFY_AUDIO_SETTINGS','WAKE_LOCK','BLUETOOTH','BLUETOOTH_CONNECT',
  'FOREGROUND_SERVICE','FOREGROUND_SERVICE_MICROPHONE','POST_NOTIFICATIONS','SYSTEM_ALERT_WINDOW']){
  if(!m.includes('android.permission.'+p+'"'))m=m.replace('</manifest>','    <uses-permission android:name="android.permission.'+p+'" />\n</manifest>');
}
// servicos (segundo plano + bolinha flutuante)
if(!m.includes('.MicForegroundService')){
  m=m.replace('</application>',
    '    <service android:name=".MicForegroundService" android:exported="false" android:foregroundServiceType="microphone" />\n'+
    '    <service android:name=".BubbleService" android:exported="false" />\n</application>');
}
// icones
if(!/android:roundIcon=/.test(m))m=m.replace('<application','<application android:roundIcon="@mipmap/ic_launcher_round"');
fs.writeFileSync(man,m);

// icone adaptativo (caveira centralizada, sem virar so um quadrado)
fs.cpSync(path.join('native','res'),res,{recursive:true,force:true});

// codigo nativo
const jd=path.join(main,'java','com','caveirax','micbooster');
fs.mkdirSync(jd,{recursive:true});
for(const f of fs.readdirSync('native').filter(x=>x.endsWith('.java')))fs.copyFileSync(path.join('native',f),path.join(jd,f));
console.log('Projeto Android preparado.');
