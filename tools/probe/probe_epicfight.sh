#!/usr/bin/env bash
# Third pass: Epic Fight's own documentation of the mob patch datapacks, and its source code that reads them
# (Epic Fight is GPL-3.0, its source is public). Run by .github/workflows/probe.yml (CI only).
set -u
mkdir -p probe && cd probe
UA='User-Agent: arcanes-probe'
echo "=== docs page"
curl -sSL -H "$UA" https://epicfight-docs.readthedocs.io/Guides/Entities/page1/ -o page1.html
python3 - <<'PY'
import re,html
t=open('page1.html',encoding='utf-8',errors='replace').read()
t=re.sub(r'<(script|style)[^>]*>.*?</\1>','',t,flags=re.S)
t=re.sub(r'<br\s*/?>','\n',t); t=re.sub(r'</(p|div|h\d|li|pre|tr)>','\n',t)
t=html.unescape(re.sub(r'<[^>]+>','',t))
t=re.sub(r'\n\s*\n+','\n',t)
i=t.find('Custom entity'); print(t[max(0,i-200):i+20000])
PY
echo "=== source branches"
curl -sSL -H "$UA" "https://api.github.com/repos/Epic-Fight/epicfight/branches?per_page=100" | python3 -c "import json,sys;print([b['name'] for b in json.load(sys.stdin)])"
for br in 1.21.1 1.21.1-neoforge 1.21.1-dev 1.21 master main; do
  path=$(curl -sSL -H "$UA" "https://api.github.com/repos/Epic-Fight/epicfight/git/trees/$br?recursive=1" | python3 -c "
import json,sys
try:
  d=json.load(sys.stdin); print('\n'.join(x['path'] for x in d.get('tree',[]) if x['path'].endswith('MobPatchReloadListener.java')))
except Exception: pass")
  if [ -n "$path" ]; then echo "=== branch $br: $path"; for p in $path; do echo "--- $p"; curl -sSL -H "$UA" "https://raw.githubusercontent.com/Epic-Fight/epicfight/$br/$p"; done; break; fi
done
echo "=== done"
