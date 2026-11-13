#!/usr/bin/env python3
"""Draws every picture and item model of the Era Phones mod into src/main/resources/assets/eraphones. Run from
anywhere: python3 era-phones/tools/gen_assets.py (needs numpy and pillow)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import emote_assets  # noqa: E402
import gear_assets  # noqa: E402
import walkman_assets  # noqa: E402
import lock_assets  # noqa: E402
import held_fix  # noqa: E402
import horror_assets  # noqa: E402
import lang  # noqa: E402
import models  # noqa: E402
import retro_assets  # noqa: E402
import smart_assets  # noqa: E402
import writing_assets  # noqa: E402

if __name__ == "__main__":
    smart_assets.main()
    retro_assets.main()
    horror_assets.main()
    gear_assets.main()
    walkman_assets.main()
    lock_assets.main()
    held_fix.main()
    emote_assets.main()
    writing_assets.main()
    models.main()
    lang.main()
