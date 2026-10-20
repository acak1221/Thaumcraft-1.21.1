#!/usr/bin/env python3
"""TC4 retained shader assets -> native namespaced GLSL150 warp post chains. Does not change retained originals."""
import json
import re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'thaumcraft_src/assets/minecraft/shaders'
TARGET=ROOT/'common/src/main/resources/assets/thaumcraft/shaders'
NAMES={'bloomColor':'bloom_color','bloom2':'bloom2','bloomVertexBase':'bloom_vertex_base','color_convolve2':'color_convolve2'}
POST={'blurtc':'warp_blurred','desaturatetc':'warp_death_gaze','hunger':'warp_hunger','sunscorned':'warp_sun_scorned'}
def output(relative,text):
 p=TARGET/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
def main():
 for old,new in POST.items():
  data=json.loads((SOURCE/'post'/f'{old}.json').read_text())
  for row in data['passes']:
   row['name']='thaumcraft:'+NAMES[row['name']] if row['name'] in NAMES else 'minecraft:'+('box_blur' if row['name']=='blur' else row['name'])
  output(Path('post')/f'{new}.json',json.dumps(data,indent=2)+'\n')
 for old,new in NAMES.items():
  for extension in ['json','fsh','vsh']:
   source=SOURCE/'program'/f'{old}.{extension}'
   if not source.exists():continue
   text=source.read_text()
   if extension=='json':
    data=json.loads(text)
    for key in ['vertex','fragment']:
     value=data[key];data[key]='thaumcraft:'+NAMES[value] if value in NAMES else 'minecraft:'+value
    text=json.dumps(data,indent=2)+'\n'
   else:
    text=text.replace('#version 120','#version 150').replace('texture2D(','texture(')
    text=text.replace('attribute ','in ').replace('varying ','out ' if extension=='vsh' else 'in ')
    if extension=='fsh':
     text=text.replace('gl_FragColor','fragColor')
     text=text.replace('#version 150','#version 150\n\nout vec4 fragColor;',1)
     # Legacy drivers accepted uniform texture reads in a global initializer. Core GLSL requires these inside main.
     initializer=re.search(r'vec4 averageColor=\(.*?\)/81\.0;',text,re.S)
     if initializer:
      code=initializer.group();text=text[:initializer.start()]+text[initializer.end():]
      text=text.replace('void main() {','void main() {\n    '+code,1)
   output(Path('program')/f'{new}.{extension}',text)
if __name__=='__main__':main()
