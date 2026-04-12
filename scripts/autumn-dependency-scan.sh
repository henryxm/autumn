#!/usr/bin/env bash
# 只读体检：依赖方升级 autumn / 多库 SQL 债排查（见 docs/AI_UPGRADE.md §4）。
# 用法：在业务工程根目录执行，或 AUTUMN_SCAN_ROOT=/path/to/repo bash scripts/autumn-dependency-scan.sh
set -euo pipefail

ROOT="${AUTUMN_SCAN_ROOT:-.}"
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: AUTUMN_SCAN_ROOT 不是目录: $ROOT" >&2
  exit 1
fi

echo "[INFO] 扫描根目录: $(cd "$ROOT" && pwd)"
echo "----"

echo "[SCAN] pom.xml 中的 cn.org.autumn 坐标（节选）"
find "$ROOT" -name pom.xml 2>/dev/null | grep -v '/target/' | while read -r f; do grep -Hn 'cn.org.autumn' "$f" 2>/dev/null || true; done | head -200
echo "----"

echo "[SCAN] application*.yml|yaml|properties 中的 autumn.database / jdbc 线索（节选）"
find "$ROOT" \( -path '*/target/*' -o -path '*/.git/*' \) -prune -o \
  \( -name 'application*.yml' -o -name 'application*.yaml' -o -name 'application*.properties' \) -print 2>/dev/null \
  | while read -r f; do grep -HnE 'autumn\.database|^spring\.datasource\.|jdbc:' "$f" 2>/dev/null || true; done | head -200
echo "----"

echo "[SCAN] Java：FIND_IN_SET"
find "$ROOT" \( -path '*/target/*' -o -path '*/.git/*' \) -prune -o -name '*.java' -print 2>/dev/null \
  | while read -r f; do grep -Hn 'FIND_IN_SET' "$f" 2>/dev/null || true; done | head -200
echo "----"

echo "[SCAN] Java：Dao 注解内联 SQL 线索 (@Select|@Update|@Insert|@Delete)"
find "$ROOT" \( -path '*/target/*' -o -path '*/.git/*' \) -prune -o -name '*.java' -print 2>/dev/null \
  | while read -r f; do grep -HnE '@Select\(|@Update\(|@Insert\(|@Delete\(' "$f" 2>/dev/null || true; done | head -200
echo "----"

echo "[INFO] 扫描结束。命中项需结合 docs/AI_DATABASE.md §8、docs/AI_UPGRADE.md 人工判断。"
