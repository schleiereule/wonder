/*
Copyright (c) 2009 Victor Stanciu - http://www.victorstanciu.ro

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/
Hotkeys={bound:false,queue:[],hotkeys:[],match:false,keycodes:{"backspace":8,"tab":9,"enter":13,"shift":16,"ctrl":17,"alt":18,"pause":19,"caps":20,"esc":27,"pgup":33,"pgdown":34,"end":35,"home":36,"left":37,"up":38,"right":39,"down":40,"ins":45,"del":46,"0":48,"1":49,"2":50,"3":51,"4":52,"5":53,"6":54,"7":55,"8":56,"9":57,"a":65,"b":66,"c":67,"d":68,"e":69,"f":70,"g":71,"h":72,"i":73,"j":74,"k":75,"l":76,"m":77,"n":78,"o":79,"p":80,"q":81,"r":82,"s":83,"t":84,"u":85,"v":86,"w":87,"x":88,"y":89,"z":90,"left-win":91,"right-win":92,"select":93,"num0":96,"num1":97,"num2":98,"num3":99,"num4":100,"num5":101,"num6":102,"num7":103,"num8":104,"num9":105,"multiply":106,"add":107,"subtract":109,"dec":110,"divide":111,"f1":112,"f2":113,"f3":114,"f4":115,"f5":116,"f6":117,"f7":118,"f8":119,"f9":120,"f10":121,"f11":122,"f12":123,"num":144,"scroll":145,"semi-colon":186,"equal":187,"comma":188,"dash":189,"period":190,"slash":191,"grave":192,"open-bracket":219,"backslash":220,"close-braket":221,"single-quote":222},bind:function(a,b){var c=Array.prototype.slice.call(arguments);c.shift();c.shift();var d=new Hotkey(a,b,c);this.hotkeys.push(d)},keydown:function(b){var c=b.element();var d=b.keyCode?b.keyCode:b.charCode;if(d!=27&&d!=13&&d!=16){if(c.tagName=='INPUT'||c.tagName=='TEXTAREA'){return}}if(Hotkeys.queue.indexOf(d)==-1){Hotkeys.queue.push(d);Hotkeys.queue.sort();var e=Hotkeys.hotkeys.find(function(a){return a.keycodes.toString()==Hotkeys.queue.toString()});if(e){Hotkeys.match=true;b.cancelBubble=true;b.returnValue=false;if(b.stopPropagation){b.stopPropagation();b.preventDefault()}b.stop();e.trigger();Hotkeys.release()}}},keyup:function(a){var b=a.keyCode?a.keyCode:a.charCode;Hotkeys.queue.splice(Hotkeys.queue.indexOf(b),1)},keypress:function(a){if(Hotkeys.match){a.stop()}},release:function(){Hotkeys.queue=[];Hotkeys.match=false}}
Hotkey=Class.create(Abstract,{initialize:function(b,c,d){if(!Hotkeys.bound){Hotkeys.bound=true;Event.observe(document,'keydown',Hotkeys.keydown);Event.observe(document,'keyup',Hotkeys.keyup);Event.observe(document,'keypress',Hotkeys.keypress)}this.combo=b.split("+");this.action=c;this.params=d;this.keycodes=[];if(this.combo){this.combo.each((function(a){this.keycodes.push(Hotkeys.keycodes[a])}).bind(this));this.keycodes.sort()}},trigger:function(){if(typeof(this.action)=='function'){this.action.apply(this,this.params)}}});