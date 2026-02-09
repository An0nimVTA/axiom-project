#!/bin/bash

# Define paths
SOURCE_BUILD="axiom-launcher-kotlin/build"
TARGET_DIR="build_portable/AxiomClient"

echo "=== Assembling NATIVE Portable Axiom Client ==="

# 1. Clean and Create Target Directory
rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"

# 2. Copy Executable and Libs
echo "Copying launcher binaries..."
cp "$SOURCE_BUILD/launch4j/AxiomLauncher.exe" "$TARGET_DIR/"
cp -r "$SOURCE_BUILD/launch4j/lib" "$TARGET_DIR/"

# 2.1 Copy launcher jar for Linux start.sh
LAUNCHER_JAR=$(ls -t "$SOURCE_BUILD/libs"/axiom-launcher-*.jar 2>/dev/null | grep -vE '(-sources|-javadoc)\.jar$' | head -1)
if [ -n "$LAUNCHER_JAR" ]; then
    cp "$LAUNCHER_JAR" "$TARGET_DIR/lib/"
else
    echo "⚠️  Launcher JAR не найден в $SOURCE_BUILD/libs"
fi

# Note: The 'runtime' folder MUST be copied here manually by you (or from a source if available)
# for the .exe to work. The .exe now expects ./runtime/bin/java.exe

# 3. Create Linux Start Script (Only for Linux users, as .exe is Windows only)
echo "Creating start.sh (for Linux only)..."
cat > "$TARGET_DIR/start.sh" << 'EOF'
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Try bundled runtime first
if [ -f "$DIR/runtime/bin/java" ]; then
    "$DIR/runtime/bin/java" -cp "$DIR/lib/*" com.axiom.launcher.MainKt "$@"
else
    java -cp "$DIR/lib/*" com.axiom.launcher.MainKt "$@"
fi
EOF
chmod +x "$TARGET_DIR/start.sh"

echo "=== Assembly Complete! ==="
echo "Files are located in: $TARGET_DIR"
echo "Structure:"
ls -F "$TARGET_DIR"
