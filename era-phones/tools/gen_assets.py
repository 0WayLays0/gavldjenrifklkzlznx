#!/usr/bin/env python3
"""Draws every picture and item model of the Era Phones mod into src/main/resources/assets/eraphones. Run from
anywhere: python3 era-phones/tools/gen_assets.py (needs numpy and pillow)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import horror_assets  # noqa: E402
import lang  # noqa: E402
import models  # noqa: E402
import retro_assets  # noqa: E402
import smart_assets  # noqa: E402

if __name__ == "__main__":
    smart_assets.main()
    retro_assets.main()
    horror_assets.main()
    models.main()
    lang.main()
