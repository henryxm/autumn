#!/usr/bin/env bash
# 在 Java 源码中检索「应改用 RedisExpireUtil」的常见可疑写法（grep 静态体检）。
# 用法：在项目根目录执行：
#   bash /path/to/autumn/scripts/redis-expire-forbidden-scan.sh .

set -euo pipefail
ROOT="${1:-.}"

echo "== Scanning under: $ROOT (Java sources only) =="

patterns=(
  'redisTemplate\.expire[[:space:]]*\('
  '\.expire[[:space:]]*\([^)]*TimeUnit'
  'opsForValue[[:space:]]*\([[:space:]]*\)[[:space:]]*\.set[[:space:]]*\([^;]+TimeUnit'
)

for p in "${patterns[@]}"; do
  echo
  echo "-- pattern: $p --"
  matches=$(grep -R -n --include='*.java' -E "$p" "$ROOT" 2>/dev/null | grep -Ev 'RedisExpireUtil\.|incrementAndExpireIfFirst|incrementAndPExpireIfFirst' || true)
  if [[ -n "${matches}" ]]; then
    printf '%s\n' "${matches}"
  else
    echo "(no matches)"
  fi
done

echo
echo "Done. Also run: mvn dependency:tree | grep -E 'spring-data-redis|redisson-spring-data'"
