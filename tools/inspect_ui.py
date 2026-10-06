from pathlib import Path
import xml.etree.ElementTree as E
from collections import defaultdict

root = Path(__file__).resolve().parents[1] / 'app/src/main/res'
a = '{http://schemas.android.com/apk/res/android}'
colors = defaultdict(list)
for p in (root/'layout').glob('*.xml'):
    for n in E.parse(p).iter():
        for k, v in n.attrib.items():
            if v.startswith('#'): colors[v].append(p.stem + ':' + k.split('}')[-1])
for c, files in sorted(colors.items()): print(c, ', '.join(sorted(set(files))))
print('\nPROFILE TREE')
def walk(n, d=0):
    print(' '*d + n.tag.split('.')[-1], n.get(a+'id',''), n.get(a+'text',''), n.get(a+'layout_height',''), n.get(a+'background',''))
    for ch in n: walk(ch, d+1)
walk(E.parse(root/'layout/fragment_profile.xml').getroot())
