#!/bin/bash
# Render lab: draws a spell's model the way the game would, without the game.
#   tools/renderlab/lab.sh <scene> <age> <eye x,y,z> <at x,y,z> <out.png> [raster.py options]
# Needs a JDK and python3 with numpy and pillow; fetches JOML once into ~/.cache/renderlab.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
CACHE=$HOME/.cache/renderlab
JOML=$CACHE/joml.jar
mkdir -p "$CACHE"
if ! unzip -tq "$JOML" >/dev/null 2>&1; then
    curl -sSL -o "$JOML" https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar
fi
SRC=$ROOT/src/main/java/com/waylays/arcanes/client/render
BUILD=$CACHE/build
rm -rf "$BUILD" && mkdir -p "$BUILD"
FILES=$(find "$HERE/stubs" "$HERE/lab" -name '*.java'; echo "$SRC/RenderHelper.java"; find "$SRC/water" "$SRC/fire" "$SRC/ritual" -name "*.java" 2>/dev/null || true)
javac -nowarn -d "$BUILD" -cp "$JOML" $FILES
SCENE=$1 AGE=$2 EYE=$3 AT=$4 OUT=$5
shift 5
java -cp "$BUILD:$JOML" renderlab.Lab "$SCENE" "$AGE" "$EYE" "$AT" "$CACHE/scene.txt"
python3 "$HERE/raster.py" "$CACHE/scene.txt" "$OUT" --eye="$EYE" --at="$AT" "$@"
