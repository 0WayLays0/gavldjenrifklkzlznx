#!/usr/bin/env bash
# Fourth pass: the rest of Epic Fight's mob patch guide (presets, examples), and the source that reads the files.
# Run by .github/workflows/probe.yml (CI only).
set -u
mkdir -p probe && cd probe
UA='User-Agent: arcanes-probe'
AUTH="Authorization: Bearer ${GH_TOKEN:-}"
curl -sSL -H "$UA" https://epicfight-docs.readthedocs.io/Guides/Entities/page1/ -o page1.html
python3 - <<'PY'
import re,html
t=open('page1.html',encoding='utf-8',errors='replace').read()
# keep code blocks readable
t=re.sub(r'<(script|style)[^>]*>.*?</\1>','',t,flags=re.S)
t=re.sub(r'<span class="linenos">.*?</span>','',t,flags=re.S)
t=re.sub(r'<td class="linenos">.*?</td>','',t,flags=re.S)
t=re.sub(r'<br\s*/?>','\n',t); t=re.sub(r'</(p|div|h\d|li|pre|tr)>','\n',t)
t=html.unescape(re.sub(r'<[^>]+>','',t))
t=re.sub(r'\n\s*\n+','\n',t)
i=t.find('Easy Setup'); i=t.find('Easy Setup',i+1); i=t.find('Easy Setup',i+1)
print("=== guide from 'Easy Setup'"); print(t[i-100:i+40000])
PY
echo "=== source"
br=$(curl -sSL -H "$UA" -H "$AUTH" "https://api.github.com/repos/Epic-Fight/epicfight" | python3 -c "import json,sys;print(json.load(sys.stdin).get('default_branch',''))")
echo "default branch: $br"
curl -sSL -H "$UA" -H "$AUTH" "https://api.github.com/repos/Epic-Fight/epicfight/branches?per_page=100" | python3 -c "import json,sys;d=json.load(sys.stdin);print([b['name'] for b in d] if isinstance(d,list) else d)"
for b in 1.21.1 1.21.1-neoforge "$br"; do
  paths=$(curl -sSL -H "$UA" -H "$AUTH" "https://api.github.com/repos/Epic-Fight/epicfight/git/trees/$b?recursive=1" | python3 -c "
import json,sys
d=json.load(sys.stdin)
for x in d.get('tree',[]) if isinstance(d,dict) else []:
  p=x['path']
  if p.endswith(('MobPatchReloadListener.java','HasCustomTag.java')): print(p)")
  if [ -n "$paths" ]; then
    echo "=== branch $b"
    for p in $paths; do echo "--- $p"; curl -sSL -H "$UA" "https://raw.githubusercontent.com/Epic-Fight/epicfight/$b/$p"; echo; done
    break
  fi
done
echo "=== done"
