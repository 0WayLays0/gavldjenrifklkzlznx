"""Drawing helpers for the role play's pictures (original art, drawn here): where they are written, and the few
helpers the drawings use (taken over from Époque RP, where these pictures were first made)."""
import json
import os

import numpy as np

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "arcanes_oublies")


def save(img, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.relpath(path))


def write_json(rel, data):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path))


def to_img(a):
    from PIL import Image
    return Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "RGBA")
