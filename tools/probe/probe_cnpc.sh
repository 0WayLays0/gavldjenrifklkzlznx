#!/usr/bin/env bash
# Decompiles Custom NPCs (NeoForge 1.21.1): its NPC edit screens, to add an Epic Fight switch. CI only.
set -u
mkdir -p probe && cd probe
curl -sSL -H 'User-Agent: arcanes-probe' "https://api.modrinth.com/v2/project/customnpcs-unofficial/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D" > cn.json
url=$(python3 -c "import json;v=[v for v in json.load(open('cn.json')) if v['version_number']=='NeoForge-1.21.1.20241226'] or json.load(open('cn.json'));print(v[0]['files'][0]['url'])")
curl -sSL -o cn.jar "$url"
curl -sSL -o vf.jar https://repo1.maven.org/maven2/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar
mkdir -p src && java -jar vf.jar -dgs=1 -rsy=1 cn.jar src > /dev/null 2>&1
mkdir -p s && (cd s && unzip -q -o ../src/cn.jar 2>/dev/null || cp -r ../src/* .)
cd s
show() { echo "=================== $1 (lines ${2:-all})"; if [ -f "$1" ]; then if [ -n "${2:-}" ]; then sed -n "$2" "$1"; else cat "$1"; fi; else echo MISSING; fi; }
echo "=== mainmenu guis"; ls noppes/npcs/client/gui/mainmenu/ | head -40
show noppes/npcs/client/gui/mainmenu/GuiNpcAdvanced.java 1,150p
F=$(grep -rl "class GuiNPCInterface2 " noppes | head -1); show "$F" 1,140p
F=$(grep -rl "class GuiNPCInterface " noppes | head -1); echo "--- $F"; grep -nE "class |public .*npc|guiLeft|guiTop|imageWidth|xSize|ySize|addButton|void init|EntityNPCInterface|getButton|buttonEvent" "$F" | head -50
F=$(grep -rl "class GuiNpcButton " noppes | head -1); echo "--- $F"; grep -nE "class |public GuiNpcButton\(" "$F" | head
echo "=== main menu tabs"; F=$(grep -rl "class GuiNpcMenu" noppes | head -1); show "$F" 1,120p
echo "=== done"
