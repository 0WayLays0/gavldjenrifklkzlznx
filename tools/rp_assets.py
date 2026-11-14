#!/usr/bin/env python3
"""Draws the role play's pictures and sounds (moved here from Époque RP with the commands): the red balloon and the
blood of /horreur, the letters written on walls by /ecrire, the heartbeat of fear. Run from anywhere:
    python3 tools/rp_assets.py   (needs numpy, pillow and ffmpeg)"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "rp"))

import heartbeat  # noqa: E402
import horror_assets  # noqa: E402
import writing_assets  # noqa: E402

if __name__ == "__main__":
    horror_assets.main()
    writing_assets.main()
    heartbeat.main()
