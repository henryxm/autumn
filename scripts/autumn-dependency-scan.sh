#!/usr/bin/env bash
# 只读扫描：在「依赖 autumn 的业务工程」根目录运行，检查版本统一性、配置线索、高风险代码模式。
# 不修改任何文件。

set -euo pipefail

ROOT="${AUTUMN_SCAN_ROOT:-$(pwd)}"
if [[ ! -d "$ROOT" ]]; then
  echo "AUTUMN_SCAN_ROOT is not a directory: $ROOT" >&2
  exit 1
fi

echo "=== autumn-dependency-scan (read-only) ==="
echo "ROOT: $ROOT"
echo ""

warn() { echo "[WARN] $*"; }
info() { echo "[INFO] $*"; }

have_rg() { command -v rg >/dev/null 2>&1; }

# 仅扫描 <dependency>...</dependency> 内 groupId=cn.org.autumn 的版本（排除项目坐标、parent 等）
extract_autumn_dep_versions() {
  awk '
    /<dependency>/ { dep=1; gid=""; ver=""; next }
    /<\/dependency>/ {
      if (dep && gid == "cn.org.autumn" && ver != "" && ver !~ /^\$\{/) print ver
      dep=0
      next
    }
    dep && /<groupId>/ {
      gsub(/^[[:space:]]*<groupId>|<\/groupId>.*$/, "", $0)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $0)
      gid=$0
      next
    }
    dep && gid == "cn.org.autumn" && /<version>/ {
      gsub(/^[[:space:]]*<version>|<\/version>.*$/, "", $0)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $0)
      ver=$0
      next
    }
  ' "$1"
}

TMPV=$(mktemp)
trap 'rm -f "$TMPV"' EXIT

while IFS= read -r -d '' f; do
  [[ -f "$f" ]] || continue
  extract_autumn_dep_versions "$f" >> "$TMPV" || true
done < <(find "$ROOT" -name pom.xml ! -path '*/target/*' ! -path '*/.git/*' -print0 2>/dev/null)

if [[ ! -s "$TMPV" ]]; then
  warn "未在 <dependency> 中解析到 cn.org.autumn 版本（多模块父工程 / Gradle / BOM 请手工核对）。"
else
  sort -u "$TMPV" -o "$TMPV".u
  mv "$TMPV".u "$TMPV"
  N=$(wc -l < "$TMPV" | tr -d ' ')
  if [[ "$N" -gt 1 ]]; then
    warn "发现多个不同的 cn.org.autumn 依赖版本（请统一）："
    sed 's/^/  - /' "$TMPV"
  else
    info "检测到 cn.org.autumn 依赖版本: $(cat "$TMPV")"
  fi
fi

# --- Lombok (heuristic) ---
LOMBOK_OK=0
while IFS= read -r -d '' pf; do
  if grep -qE 'annotationProcessorPaths|lombok-maven-plugin' "$pf" 2>/dev/null; then
    LOMBOK_OK=1
    break
  fi
done < <(find "$ROOT" -name pom.xml ! -path '*/target/*' ! -path '*/.git/*' -print0 2>/dev/null)

if [[ "$LOMBOK_OK" -eq 1 ]]; then
  info "部分 pom 含 lombok / annotationProcessorPaths（JDK9+ 编译问题见 AI_POSTGRESQL.md）。"
elif find "$ROOT" -name '*.java' ! -path '*/target/*' ! -path '*/.git/*' -print -quit 2>/dev/null | grep -q .; then
  warn "未在 pom.xml 中明显看到 lombok 注解处理器配置；JDK9+ 编译时请核对工程化说明。"
fi

# YAML/Properties：autumn.database 或 autumn: 下的 database:
autumn_db_configured() {
  local c="$1"
  grep -q 'autumn\.database' "$c" 2>/dev/null && return 0
  if grep -q '^autumn:' "$c" 2>/dev/null; then
    grep -A40 '^autumn:' "$c" | grep -qE '^[[:space:]]*database:' && return 0
  fi
  return 1
}

FOUND_CFG=0
while IFS= read -r -d '' c; do
  FOUND_CFG=1
  if autumn_db_configured "$c"; then
    info "已配置 autumn 库类型（autumn.database / YAML）: $c"
  fi
  if grep -qi 'postgresql' "$c" 2>/dev/null && ! autumn_db_configured "$c"; then
    warn "文件提及 postgresql 但未发现 autumn 库类型配置: $c（参见 AI_POSTGRESQL.md）"
  fi
done < <(find "$ROOT" \( -name 'application*.yml' -o -name 'application*.yaml' -o -name 'application*.properties' \) ! -path '*/target/*' ! -path '*/.git/*' -print0 2>/dev/null)

if [[ "$FOUND_CFG" -eq 0 ]]; then
  warn "未发现 application*.yml|yaml|properties（可能在子模块或配置中心）。"
fi

scan_pattern() {
  local pat="$1"
  local msg="$2"
  local hits=0
  local d
  while IFS= read -r -d '' d; do
    if have_rg; then
      n=$( (rg -l "$pat" "$d" 2>/dev/null || true) | wc -l | tr -d ' ')
    else
      n=$( (grep -Erl "$pat" --include='*.java' "$d" 2>/dev/null || true) | wc -l | tr -d ' ')
    fi
    hits=$((hits + n))
  done < <(find "$ROOT" -type d \( -path '*/src/main/java' -o -path '*/src/test/java' \) ! -path '*/target/*' ! -path '*/.git/*' -print0 2>/dev/null)
  if [[ "$hits" -gt 0 ]]; then
    warn "$msg (约 $hits 个文件命中，请人工复核)"
  fi
}

# 固定子串（避免 grep BRE 对括号解析问题）
scan_fixed() {
  local pat="$1"
  local msg="$2"
  local hits=0
  local d
  while IFS= read -r -d '' d; do
    if have_rg; then
      n=$( (rg -l -F "$pat" "$d" 2>/dev/null || true) | wc -l | tr -d ' ')
    else
      n=$( (grep -rlF "$pat" --include='*.java' "$d" 2>/dev/null || true) | wc -l | tr -d ' ')
    fi
    hits=$((hits + n))
  done < <(find "$ROOT" -type d \( -path '*/src/main/java' -o -path '*/src/test/java' \) ! -path '*/target/*' ! -path '*/.git/*' -print0 2>/dev/null)
  if [[ "$hits" -gt 0 ]]; then
    warn "$msg (约 $hits 个文件命中，请人工复核)"
  fi
}

if find "$ROOT" -path '*/src/main/java' ! -path '*/target/*' 2>/dev/null | head -1 | grep -q .; then
  scan_pattern 'sun\.misc\.Launcher' '发现 sun.misc.Launcher 引用（JDK9+ 模块问题）'
  scan_pattern 'FIND_IN_SET' '发现 FIND_IN_SET（MySQL 专有，PG 需改造）'
  scan_fixed "concat('%'" '发现 concat 三参模糊写法（宜改用 likeContainsAny；注释/文档亦会命中）'
  scan_pattern 'count\(\*\).*limitOne' '发现 COUNT 与 limitOne 同句（聚合勿追加 limitOne）'
  scan_pattern 'LIMIT[[:space:]]+[0-9]' '发现可能手写 LIMIT（多库时注意方言）'
  # 老旧 Dao：MyBatis 注解内联 SQL（建议改为 @*Provider + RuntimeSql，见 AI_DATABASE.md §8）
  scan_pattern '@(Select|Update|Insert|Delete)\(\s*"' '发现 Dao/Mapper 注解内联 SQL 字符串（建议 Provider + RuntimeSql）'
  scan_pattern '@(Select|Update|Insert|Delete)\(\s*\{' '发现 Dao/Mapper 注解多行 SQL 块 { ... }（建议 Provider + RuntimeSql）'
  # Wrapper / 链式调用中塞自定义 SQL 片段（需人工判断是否含方言）
  scan_pattern '\.(apply|last|having)\(' '发现 Wrapper.apply/last/having（请审是否含反引号/单库函数；见 AI_DATABASE.md §8.3）'
  # 常见 MySQL 专有函数（非 MySQL 族需改造）
  scan_pattern 'IFNULL\(|DATE_FORMAT\(|GROUP_CONCAT\(|LAST_INSERT_ID\(|STR_TO_DATE\(' '发现疑似 MySQL 专有函数（多库请改写或单库声明）'
else
  info "未发现 src/main/java，跳过源码模式扫描。"
fi

echo ""
echo "=== 扫描结束（只读，未修改任何文件）==="
