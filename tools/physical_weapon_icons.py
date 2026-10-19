#!/usr/bin/env python3
"""Physical Prowess spell icons for the weapon techniques: evade, the third scythe, sword and dagger moves, and the
hammer, axe, spear, longsword, bow, crossbow and throwing-knife moves.

Called by physical_assets.main. All art is original pixel art over the school's leather background, outlined so it
reads at 16 px. Nothing here glows: steel, wood, leather, rope, dust and motion lines.
"""
import gen_assets as g
import physical_assets as pa

LEGEND = {**pa.PIXELS,
          "W": pa.WOOD, "L": (164, 116, 66),           # wood: hafts, shafts, stocks, shields
          "i": (150, 156, 170), "I": (96, 100, 114),   # iron heads
          "t": (230, 226, 210),                        # bow string, target rings
          "f": (190, 40, 40),                          # red fletching, target rings
          "R": (196, 160, 96),                         # rope
          "g": (200, 160, 70),                         # sword guards and pommels, as in pa._sword
          "y": (255, 214, 110)}                        # a steel spark


def _icon(rows):
    img = g.spell_background(8, 8, pa.LEATHER, pa.DARK)
    img.alpha_composite(g.from_grid(rows, LEGEND))
    return img


def evade_icon():
    """Pixel art: an arrow arcing backward, right to left, from a puff of dust."""
    return _icon([
        "................",
        "................",
        "......ooooo.....",
        ".....owwwwwo....",
        "....owooooowo...",
        "...owo.....owo..",
        "...owo.....owo..",
        "..oowoo....owo..",
        ".owwwwwo...owo..",
        "..owwwo....owo..",
        "...owo.....owo..",
        "....o......owo..",
        "..........ddodd.",
        ".........dddddd.",
        "..........dd.dd.",
        "................",
    ])


def scythe_hook_icon():
    """Pixel art: a scythe, an arrow inside its hook pulling toward the handle."""
    pull = [
        "................",
        "................",
        "................",
        "........o.......",
        ".......owo......",
        ".....ooowwo.....",
        "....owwwwwwo....",
        "...owooowwo.....",
        "..owo..owo......",
        "..owo...o.......",
        ".owo............",
        "..o.............",
        "................",
        "................",
        "................",
        "................",
    ]
    return _icon(pa._overlay(pull, pa.SCYTHE))


def sword_disarm_icon():
    """Pixel art: a sword rising from the bottom left, the enemy's sword tumbling away upside down."""
    return _icon([
        "...........ogo..",
        "...........oho..",
        ".........aogHgo.",
        "........aogggggo",
        "........a.oomMo.",
        "........o..omMo.",
        ".......omo.omMoa",
        "......omMo.omMoa",
        "..o..omMo..omoa.",
        ".ogoomMo....o...",
        "..ogmMo.........",
        "..oHgo..........",
        ".oHhogo.........",
        "ogho.o..........",
        "ogo.............",
        ".o..............",
    ])


def dagger_finisher_icon():
    """Pixel art: a dagger driven point down, motion lines above it, blood under the point."""
    return _icon([
        "................",
        "......oooo......",
        "..w...oggo...w..",
        "..w...ohHo...w..",
        "..a...ohHo...a..",
        "....oooooooo....",
        "....oggggggo....",
        "....oooooooo....",
        "......omMo......",
        "......omMo......",
        "......omMo......",
        "......omMo......",
        ".......oro......",
        "........r.......",
        ".......rr.......",
        ".......rr.......",
    ])


def hammer_concussion_icon():
    """Pixel art: a war hammer swinging in, a dizzy spiral and a star where it lands."""
    return _icon([
        "..w.............",
        ".waw......oo....",
        "..w.www..oimo...",
        "...w...woiiimo..",
        "..w.ww.wIiiiimo.",
        "..w.w..woIiiiiIo",
        "...ww.w.oLIiiIo.",
        "......woLWoIIo..",
        "......oLWo.oo...",
        ".....oLWo.......",
        "....oLWo........",
        "...oLWo.........",
        "..oLWo..........",
        ".oLWo...........",
        "..oo............",
        "................",
    ])


def hammer_quake_icon():
    """Pixel art: a hammer head smashed into the ground, cracks spreading and rocks flying."""
    return _icon([
        "................",
        "......oooo......",
        "..oo..oLWo...oo.",
        ".odo..oLWo..odo.",
        "..oo..oLWo...oo.",
        "......oLWo......",
        "...oooooooooo...",
        "...omiiiiiiIo...",
        "w..oiiiiiiiIo..w",
        ".w.oIIIIIIIIo.w.",
        "...oooooooooo...",
        "dddddddooddddddd",
        "ddddddoddoddddd.",
        ".dddoodddoddod..",
        "..ddddodddoddd..",
        "....dddddddd....",
    ])


def hammer_ram_icon():
    """Pixel art: a war hammer driven head first to the right, impact lines off its face."""
    return _icon([
        "................",
        "................",
        "........oooooo..",
        ".www....omiiIo..",
        "........omiiIo.w",
        "........omiiIo..",
        "ooooooooomiiIo.w",
        "LLLLLLLLomiiIo.w",
        "WWWWWWWWomiiIo.w",
        "ooooooooomiiIo.w",
        "........omiiIo..",
        "........omiiIo.w",
        ".www....oIIIIo..",
        "........oooooo..",
        "................",
        "................",
    ])


def axe_cleave_icon():
    """Pixel art: an axe chopped down into a round shield, cracks running from the cut."""
    return _icon([
        "...........a....",
        ".......ooo..w...",
        "oooooooIIIo..w..",
        "LLLLLLLIIILo.w..",
        "WWWWWWWIIIWo..w.",
        "ooooooooiio...a.",
        "......oiiiio....",
        ".....oiiiiiio...",
        "....oImmmmmmo...",
        "...oILLoLLoLIo..",
        "...oILoWiiWoIo..",
        "...oILoWimWLIo..",
        "...oILLWLLWLIo..",
        "....oILWLLWIo...",
        ".....oIIIIIo....",
        "......ooooo.....",
    ])


def axe_throw_icon():
    """Pixel art: an axe spinning through the air, the curve of its spin trailing behind."""
    return _icon([
        "................",
        "................",
        ".............o..",
        "............oLo.",
        "...........oLWo.",
        ".........ooLWoo.",
        "........oIIWooio",
        ".......oIIIiiimo",
        ".......oLIiiimo.",
        "a.....oLWoiimo..",
        "a....oLWoiimo...",
        "w...oLWooimo....",
        ".a.oLWo.omo.....",
        "..aoWo...o......",
        "...waaww........",
        "................",
    ])


def axe_sunder_icon():
    """Pixel art: a chestplate split by a diagonal cut."""
    return _icon([
        "..............w.",
        ".oooo......oowa.",
        ".omiio....owIo..",
        ".omiiiooooaiIo..",
        ".omiiiiiiwiiIo..",
        ".ooomiiiwoiIooo.",
        "...omiiwoiiIo...",
        "...omiwoiiiIo...",
        "...omwoiiiiIo...",
        "...owoiiiiiIo...",
        "...wIIIIIIIIo...",
        "..woooooooooo...",
        ".w..............",
        "w...............",
        "................",
        "................",
    ])


def spear_thrust_icon():
    """Pixel art: a long spear thrust to the right, long thrust lines along it."""
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "..wwwwwwww......",
        "...........oo...",
        "..........oimoo.",
        "oooooooooooimmmo",
        "LLLLLLLLLWoiIIIo",
        "oooooooooooiIoo.",
        "..........oIo...",
        "...........o....",
        "..wwwwwwww......",
        "................",
        "................",
        "................",
    ])


def spear_charge_icon():
    """Pixel art: a spear lowered for a running charge, speed lines and a cloud of dust behind."""
    return _icon([
        "................",
        "...wwww.........",
        ".www............",
        "ooo.............",
        "LLLooo..........",
        "WWWLLLooo.......",
        "oooWWWLLLo.oo...",
        "...oooWWWLoiioo.",
        "www...oooWiiiimo",
        ".ww......oiIIoo.",
        "..........ooo...",
        "..dd............",
        ".dddd.d.........",
        "dddddddd........",
        ".dd.ddd.........",
        "................",
    ])


def spear_javelin_icon():
    """Pixel art: a javelin coming down at the end of a dotted arc."""
    return _icon([
        "................",
        "................",
        "......o.........",
        ".....oLo........",
        "....woWLo.......",
        "...a..oWLo......",
        "...w...oWLo.....",
        "..a.....oWLoo...",
        "..w......oimio..",
        "..a......oiimo..",
        ".w........ooimo.",
        ".a..........oo..",
        ".w..............",
        ".a..............",
        ".w..............",
        "................",
    ])


def longsword_cut_icon():
    """Pixel art: a two-handed longsword held diagonally, the wide arc of its downward cut."""
    return _icon([
        "................",
        "............o...",
        "...........omo..",
        "..........omMo.w",
        ".........omMo..w",
        "........omMo...w",
        "..o....omMo...ww",
        ".ogo..omMo....w.",
        "..ogoomMo.....w.",
        "...ogooo.....w..",
        "...oogo.....ww..",
        "..oHhogo...ww...",
        ".ogoo.ogo.ww....",
        ".ogo...o.ww.....",
        "..o...www.......",
        "...wwww.........",
    ])


def longsword_sweep_icon():
    """Pixel art: a longsword held level over the half-moon of its sweep."""
    return _icon([
        "................",
        "................",
        "..o.............",
        ".ogoo...........",
        "ohhhgoooooooooo.",
        "oHHHgmmmmmmmmmmo",
        "ohhhgMMMMMMMMMo.",
        ".ogooooooooooo..",
        "..o.............",
        "w..............w",
        "w..............w",
        ".w............w.",
        ".aw..........wa.",
        "..aww......wwa..",
        "....awwwwwwa....",
        "................",
    ])


def longsword_parry_icon():
    """Pixel art: two blades crossed in an X, a steel spark where they meet."""
    return _icon([
        ".oo..........oo.",
        ".omo........omo.",
        "..oMo......oMo..",
        "...oMo....oMo...",
        "....oMo.y.oMo...",
        ".....oMywyMo....",
        "......ywwwy.....",
        ".....yowwwoy....",
        "......oMyMo.....",
        ".....oMo..oMo...",
        "....oMo....oMo..",
        "..oogo......ogoo",
        "..oggo......oggo",
        "..ohoo......ooho",
        ".oho........oho.",
        ".oo..........oo.",
    ])


def bow_guided_icon():
    """Pixel art: an arrow at the end of a snaking dotted path."""
    return _icon([
        "................",
        "............ooo.",
        "...........oiimo",
        "............oiio",
        "...........oLoio",
        "..........oLo.o.",
        "........ooLo....",
        ".......ofLo.....",
        "......ofLfo.....",
        "......oLfo......",
        "......aoo.......",
        "......w.........",
        ".a.wwa..........",
        ".w.a............",
        "a...............",
        "w...............",
    ])


def bow_volley_icon():
    """Pixel art: three arrows fanning out from the bottom left."""
    return _icon([
        "...o............",
        "..omo...........",
        ".oiiio....ooo...",
        "..oLo....oiimo..",
        "..oLo.....oiio..",
        "..oLo...ooLoio..",
        "..oLo..ofLo.o...",
        "..oLo.ofLfo.....",
        ".ofLfooLfo......",
        ".ofLfo.oo.......",
        "..oLo.oo.....o..",
        "...o.offoooooio.",
        "....oLLLLLLLLimo",
        ".....offoooooio.",
        "......oo.....o..",
        "................",
    ])


def bow_rain_icon():
    """Pixel art: arrows raining point down onto the ground."""
    return _icon([
        "...........oLo..",
        "...o......ofLfo.",
        "..oLo.....ofLfo.",
        ".ofLfo.....oLo..",
        ".ofLfo..o..oLo..",
        "..oLo..oLooiiio.",
        "..oLo.ofLfoomo..",
        "..oLo.ofLfo.o...",
        ".oiiio.oLo......",
        "..omo..oLo......",
        "...o...oLo......",
        "......oiiio.....",
        ".......omo......",
        "ddddddddoddddddd",
        "dddddddddddddddd",
        "................",
    ])


def crossbow_pierce_icon():
    """Pixel art: a heavy bolt run through two targets."""
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "...ooo...ooo....",
        "..offfo.offfoo..",
        "ooftttfoftttfioo",
        "LLLLLLLLLLLLLiim",
        "ooftttfoftttfioo",
        "..offfo.offfoo..",
        "...ooo...ooo....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ])


def crossbow_burst_icon():
    """Pixel art: a crossbow, seen from above, three bolts flying ahead of it in a row."""
    return _icon([
        "................",
        "...oo...........",
        "..oLLo..........",
        ".otoLo..........",
        ".otooLo.........",
        "oto.oLo.....o...",
        "oto.oLo.o..oio..",
        "tooooLioioooiooo",
        "WWWLLLimLLimLLim",
        "tooooLioioooiooo",
        "oto.oLooio..o...",
        "oto.oLo.o.......",
        ".otooLo.........",
        ".otoLo..........",
        "..oLLo..........",
        "...oo...........",
    ])


def crossbow_harpoon_icon():
    """Pixel art: a barbed harpoon bolt flying right, a wavy rope trailing back from it."""
    return _icon([
        "................",
        "................",
        "................",
        "..........o.....",
        ".........oIo....",
        "..........oIo...",
        ".oo...ooooiiIoo.",
        "oRRo.oRLLLiiiimo",
        "RooRoRooooiiIoo.",
        "o..oRo....oIo...",
        "....o....oIo....",
        "..........o.....",
        "................",
        "................",
        "................",
        "................",
    ])


def knife_throw_icon():
    """Pixel art: a throwing knife flying right toward a target."""
    return _icon([
        "................",
        "................",
        "................",
        "...........ooo..",
        "..........offfo.",
        "aaa....oooftttfo",
        "..ooooommoftftfo",
        ".oHhHMmmmmftttfo",
        "..oooooMMoofffo.",
        "aaa....oo..ooo..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ])


def knife_fan_icon():
    """Pixel art: five throwing knives fanning out."""
    return _icon([
        "................",
        "................",
        "................",
        "................",
        "o......o......o.",
        "Mo....oMo....oMo",
        "omo...omo...omo.",
        ".omo..omo..omo..",
        "..omo.omo.omo...",
        "...oHooHooHo....",
        "....ohohoho.....",
        "oooooo.o.oooooo.",
        "MmmmHho.ohHmmmMo",
        "oooooo...oooooo.",
        "................",
        "................",
    ])


def knife_pin_icon():
    """Pixel art: a throwing knife driven through a boot into the cracked ground."""
    return _icon([
        "................",
        "................",
        ".oooo...........",
        "oHHHHo..........",
        "ohhhho..o.......",
        "ohhhho.oHo......",
        "ohhhho.oho......",
        "ohhhhhooHo......",
        "ohhhhhhhMoo.....",
        "ohhhhhhhmhhoo...",
        "ohhhhhhMmmhhHo..",
        "oIIIIIIMmmIIIo..",
        ".ooooooomoooo...",
        "dddddddomoddddd.",
        ".dddddodododddd.",
        "...ddoddddddd...",
    ])


ICONS = {"evade": evade_icon, "scythe_hook": scythe_hook_icon, "sword_disarm": sword_disarm_icon,
         "dagger_finisher": dagger_finisher_icon, "hammer_concussion": hammer_concussion_icon,
         "hammer_quake": hammer_quake_icon, "hammer_ram": hammer_ram_icon, "axe_cleave": axe_cleave_icon,
         "axe_throw": axe_throw_icon, "axe_sunder": axe_sunder_icon, "spear_thrust": spear_thrust_icon,
         "spear_charge": spear_charge_icon, "spear_javelin": spear_javelin_icon, "longsword_cut": longsword_cut_icon,
         "longsword_sweep": longsword_sweep_icon, "longsword_parry": longsword_parry_icon,
         "bow_guided": bow_guided_icon, "bow_volley": bow_volley_icon, "bow_rain": bow_rain_icon,
         "crossbow_pierce": crossbow_pierce_icon, "crossbow_burst": crossbow_burst_icon,
         "crossbow_harpoon": crossbow_harpoon_icon, "knife_throw": knife_throw_icon, "knife_fan": knife_fan_icon,
         "knife_pin": knife_pin_icon}


def main():
    for name, fn in ICONS.items():
        g.save(fn(), f"textures/gui/spell_icons/{name}.png")
