/* Display size is local to this screen. Changes require explicit confirmation. */
(function(){
 'use strict';const key='wattpit-display-scale-v1',native=window.WattPit;
 const normalize=x=>Number.isFinite(Number(x))?Math.round(Math.max(50,Math.min(150,Number(x)))):100;
 let saved=100;try{saved=normalize(native?.displayScale?native.displayScale():localStorage.getItem(key)??100);}catch(_){}
 function apply(value){value=normalize(value);document.documentElement.style.zoom=String(value/100);document.documentElement.style.setProperty('--app-scale',String(value/100));window.dispatchEvent(new Event('resize'));return value;}
 window.WattDisplay={normalize,apply,get value(){return normalize(parseFloat(document.documentElement.style.zoom)*100);}};apply(saved);
 document.addEventListener('DOMContentLoaded',()=>{
  const input=document.getElementById('displayScale'),reset=document.getElementById('resetDisplayScale');if(!input)return;
  const row=input.closest('.setting-row')||input.parentElement;input.hidden=true;if(reset)reset.hidden=true;const old=document.getElementById('displayScaleValue');if(old)old.hidden=true;
  const open=document.createElement('button');open.className='quiet';open.textContent='Größe einstellen · '+saved+' %';row.append(open);
  open.onclick=()=>{const a=window.AutarPieApp;if(!a)return;a.openModal('Darstellungsgröße',`<p>Vorschau anpassen und anschließend speichern.</p><label class="form-field">Größe · <output id="apScaleValue">${saved} %</output><input id="apSize" type="range" min="50" max="150" value="${saved}"></label><div class="form-actions"><button id="apSizeCancel" class="quiet">Abbrechen</button><button id="apSizeSave" class="primary">Speichern</button></div>`);
   const slider=document.getElementById('apSize');slider.oninput=()=>{document.getElementById('apScaleValue').textContent=normalize(slider.value)+' %';apply(slider.value);};
   const close=()=>apply(saved);const closer=document.getElementById('closeModal'),previous=closer.onclick;closer.onclick=()=>{close();closer.onclick=previous;previous?.();};
   document.getElementById('apSizeCancel').onclick=()=>{close();closer.onclick=previous;a.closeModal();};
   document.getElementById('apSizeSave').onclick=()=>{const next=normalize(slider.value);let e='';try{if(native?.saveDisplayScale)e=native.saveDisplayScale(next);else localStorage.setItem(key,String(next));}catch(_){e='Größe konnte nicht gespeichert werden.';}if(e){close();a.toast(e);return;}saved=next;open.textContent='Größe einstellen · '+saved+' %';apply(saved);closer.onclick=previous;a.closeModal();};
  };
 });
})();
