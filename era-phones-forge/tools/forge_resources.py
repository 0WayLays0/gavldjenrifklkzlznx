"""The Forge 1.20.1 build's resources, made from the NeoForge 1.21.1 build's (era-phones/src/main/resources, written by
its generator): the same models, textures, sounds and languages, with what 1.20.1 reads differently changed:
- data folders in the plural (loot_tables, recipes, structures, tags/items, tags/blocks);
- a recipe's result {"item": ...} (1.21: {"id": ...});
- the loader's own keys: forge_data, forge:separate_transforms (1.21: neoforge_...).
Run after era-phones/tools/gen_assets.py; it rewrites era-phones-forge/src/main/resources/{assets,data}."""
import json
import os
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "era-phones", "src", "main", "resources")
DST = os.path.join(HERE, "..", "src", "main", "resources")
FOLDERS = {"loot_table": "loot_tables", "recipe": "recipes", "structure": "structures", "advancement": "advancements",
           "predicate": "predicates", "item_modifier": "item_modifiers", "function": "functions"}
TAGS = {"item": "items", "block": "blocks", "entity_type": "entity_types", "fluid": "fluids", "game_event": "game_events"}


def data_path(rel):
    parts = rel.split(os.sep)
    # data/<ns>/<folder>/...
    if len(parts) > 2 and parts[0] == "data":
        if parts[2] in FOLDERS:
            parts[2] = FOLDERS[parts[2]]
        elif parts[2] == "tags" and len(parts) > 3 and parts[3] in TAGS:
            parts[3] = TAGS[parts[3]]
    return os.sep.join(parts)


def fix_json(rel, data):
    if rel.startswith("data") and isinstance(data, dict) and isinstance(data.get("result"), dict) and "id" in data["result"]:
        data["result"]["item"] = data["result"].pop("id")
    text = json.dumps(data, ensure_ascii=False, indent=2)
    return text.replace('"neoforge_data"', '"forge_data"').replace('"neoforge:', '"forge:')


def main():
    for top in ("assets", "data"):
        out = os.path.join(DST, top)
        if os.path.isdir(out):
            shutil.rmtree(out)
    count = 0
    for top in ("assets", "data"):
        for root, _, files in os.walk(os.path.join(SRC, top)):
            for f in files:
                src = os.path.join(root, f)
                rel = os.path.relpath(src, SRC)
                dst = os.path.join(DST, data_path(rel))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                if f.endswith(".json"):
                    with open(src, encoding="utf-8") as fh:
                        data = json.load(fh)
                    with open(dst, "w", encoding="utf-8") as fh:
                        fh.write(fix_json(rel, data) + "\n")
                else:
                    shutil.copyfile(src, dst)
                count += 1
    print(f"{count} resources written for Forge 1.20.1")


if __name__ == "__main__":
    main()
