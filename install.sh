#!/bin/bash

JAR_URL="https://github.com/strangecodder/CLIModeller/releases/download/release/CLIModeller-1.0-SNAPSHOT.jar"

APP_NAME="cfsm-modeller"

LIB_DIR="/usr/local/lib/$APP_NAME"

BIN_DIR="/usr/local/bin"

set -e

echo "🚀 Начинаю установку $APP_NAME..."

if [ "$EUID" -ne 0 ]; then
    echo "⚠️  Пожалуйста, запустите установку с sudo (например: curl ... | sudo bash)"
    exit 1
fi

mkdir -p "$LIB_DIR"
mkdir -p "$BIN_DIR"

echo "⬇️  Скачиваю JAR файл из $JAR_URL ..."
curl -fsSL "$JAR_URL" -o "$LIB_DIR/$APP_NAME.jar"

echo "Создаю скрипт запуска в $BIN_DIR/$APP_NAME ..."
cat > "$BIN_DIR/$APP_NAME" <<EOF
#!/bin/bash
java -jar "$LIB_DIR/$APP_NAME.jar" "\$@"
EOF

chmod +x "$BIN_DIR/$APP_NAME"

echo "Установка завершена!"
echo "Теперь вы можете использовать команду: $APP_NAME"