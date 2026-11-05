(function(){
  'use strict';
  const $=id=>document.getElementById(id),esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const parse=s=>/^#?[0-9a-f]{6}$/i.test(String(s).trim())?'#'+String(s).trim().replace(/^#/,'').toLowerCase():null;
  const H=window.WattColorInput={
    field(id,color,label='Farbcode'){
      color=parse(color)||'#81baf4';
      return `<span class="color-code-field" data-color-input="${id}"><input id="${id}" type="color" value="${color}" aria-label="${esc(label)} auswählen"><input id="${id}Hex" class="color-code" type="text" value="${color.toUpperCase()}" maxlength="7" spellcheck="false" autocapitalize="characters" autocomplete="off" aria-label="${esc(label)} als Hexcode" placeholder="#RRGGBB"><button type="button" id="${id}Copy" class="quiet color-copy" aria-label="${esc(label)} kopieren">Kopieren</button></span>`;
    },
    set(id,color){const value=parse(color);if(value){$(id).value=value;$(id+'Hex').value=value.toUpperCase();$(id+'Hex').setAttribute('aria-invalid','false');}},
    disabled(id,value){document.querySelectorAll(`[data-color-input="${id}"] input`).forEach(el=>el.disabled=value);},
    read(id){const value=parse($(id+'Hex').value);if(!value)throw Error('Bitte einen gültigen Farbcode mit sechs Zeichen eingeben, zum Beispiel #4C65E0.');return value;},
    bind(id,a,changed=()=>{}){
      const picker=$(id),hex=$(id+'Hex');
      picker.oninput=()=>{H.set(id,picker.value);changed(picker.value);};
      hex.oninput=()=>{const value=parse(hex.value);hex.setAttribute('aria-invalid',value?'false':'true');if(value){picker.value=value;changed(value);}};
      hex.onblur=()=>{const value=parse(hex.value);if(value)H.set(id,value);};
      $(id+'Copy').onclick=async()=>{
        try{
          const value=H.read(id).toUpperCase();
          if(typeof a.native?.copyColorCode==='function'){const error=await a.native.copyColorCode(value);if(error)throw Error(error);}
          else if(navigator.clipboard?.writeText)await navigator.clipboard.writeText(value);
          else{const old=document.activeElement;hex.focus();hex.select();if(!document.execCommand('copy'))throw Error('Bitte den Farbcode markieren und kopieren.');old?.focus();}
          a.toast('Farbcode '+value+' kopiert.');
        }catch(e){a.toast(e.message);}
      };
    }
  };
  window.WattValueColors=class{
    constructor(a){this.a=a;$('valueColorsOpen').onclick=()=>this.open();}
    open(){
      const C=window.WattDashboardCore,board=this.a.dashboard.config,draft=C.valueColors(board.valueColors),rows=[];
      for(const kind of ['water','gas'])rows.push({id:kind,label:kind==='water'?'Wasser':'Gas',metrics:Object.keys(C.CATALOG).filter(key=>C.CATALOG[key].resource===kind),group:'Gas und Wasser'});
      for(const [key,m] of Object.entries(C.CATALOG))if(!m.resource)rows.push({id:key,label:m.label,metrics:[key],group:m.group});
      const groups=[...new Set(rows.map(r=>r.group))],row=r=>`<div class="value-color-row"><strong>${esc(r.label)}</strong>${H.field('valueColor-'+r.id,draft[r.metrics[0]],r.label)}</div>`;
      this.a.openModal('Messwertfarben',`<p class="muted">Gilt auf diesem Handy für Kachelauswahl, Zahlen, Rundinstrumente und Diagramme. Wasser und Gas erhalten jeweils eine gemeinsame Farbe. Kacheln mit bewusst gewählter eigener Farbe behalten diese.</p><div class="value-color-groups">${groups.map((group,i)=>`<details ${i===0?'open':''}><summary>${esc(group)}</summary>${rows.filter(r=>r.group===group).map(row).join('')}</details>`).join('')}</div><p id="valueColorsError" class="form-error" role="alert"></p><div class="form-actions"><button id="valueColorsCancel" class="quiet">Verwerfen</button><button id="valueColorsSave" class="primary">Speichern</button></div>`);
      rows.forEach(r=>H.bind('valueColor-'+r.id,this.a,value=>r.metrics.forEach(key=>draft[key]=value)));
      $('valueColorsCancel').onclick=()=>this.a.closeModal();
      $('valueColorsSave').onclick=()=>{
        try{
          rows.forEach(r=>{const value=H.read('valueColor-'+r.id);r.metrics.forEach(key=>draft[key]=value);});
          this.a.dashboard.config.valueColors=draft;
          if(this.a.dashboard.commit()){this.a.closeModal();this.a.repaint();this.a.toast('Messwertfarben gespeichert.');}
        }catch(e){$('valueColorsError').textContent=e.message;}
      };
    }
  };
})();
