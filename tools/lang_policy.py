#!/usr/bin/env python3
"""The addon's language rule: every screen, menu, message and command is in French, whatever the game's language;
what belongs to the magic itself (spells, schools, rituals, creatures, items, effects, attributes, death messages,
the spells' stat lines) is in English, in both languages.

Applied to src/main/resources/assets/arcanes_oublies/lang/{en_us,fr_fr}.json: the French text of the interface keys
is copied into en_us, the English text of the content keys into fr_fr. Run from the repository root:
    python3 tools/lang_policy.py
"""
import collections
import json
import os

LANG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "arcanes_oublies", "lang")

# Interface: French everywhere
INTERFACE = ("metier.", "gui.", "itemGroup.", "key.", "ui.arcanes_oublies.editor.", "ui.arcanes_oublies.music.", "ui.arcanes_oublies.altar.",
             # The role play and the game masters' tools (from Époque RP): all French
             "rp.", "arms.", "event.", "carry.", "body.", "size.", "entity.arcanes_oublies.ecriture")
# Interface keys whose French text needs a tweak once the content around them is English
OVERRIDES = {"gui.arcanes_oublies.creative.heat": "Starfire %s (%s)"}


def is_interface(key):
    return key.startswith(INTERFACE)


def main():
    load = lambda n: json.load(open(os.path.join(LANG, n), encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
    en, fr = load("en_us.json"), load("fr_fr.json")
    for key in en:
        if key not in fr:
            continue
        if is_interface(key):
            en[key] = fr[key]
        else:
            fr[key] = en[key]
    for key, text in OVERRIDES.items():
        en[key] = fr[key] = text
    for name, data in (("en_us.json", en), ("fr_fr.json", fr)):
        with open(os.path.join(LANG, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("interface in French:", sum(1 for k in en if is_interface(k)), "keys; content in English:", sum(1 for k in en if not is_interface(k)))


if __name__ == "__main__":
    main()
