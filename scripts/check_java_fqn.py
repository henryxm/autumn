#!/usr/bin/env python3
"""
check_java_fqn — 检测 Java 源码中不必要的全限定类名（FQN）。

规范：docs/AI_CODE_STYLE.md §7
  - 默认 import + 短类名
  - 仅类名冲突时允许 FQN

用法:
  python3 scripts/check_java_fqn.py [ROOT]
  AUTUMN_FQN_FAIL_ON_HIT=1 python3 scripts/check_java_fqn.py   # CI：有命中 exit 1

环境变量:
  AUTUMN_FQN_SKIP_GEN=1     排除 **/controller/gen/**、**/modules/gen/**
  AUTUMN_FQN_JAVA_GLOB      默认 *.java
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
SKIP_GEN = os.environ.get("AUTUMN_FQN_SKIP_GEN", "1") == "1"
FAIL_ON_HIT = os.environ.get("AUTUMN_FQN_FAIL_ON_HIT", "0") == "1"
SCRIPT_DIR = Path(__file__).resolve().parent
ALLOWLIST = SCRIPT_DIR / "fqn-allowlist.txt"

FQN = re.compile(r"\b([a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+\.[A-Z][A-Za-z0-9_]*)\b")

SKIP_PREFIX = (
    "import ",
    "import static ",
    "package ",
)
SKIP_LINE_START = SKIP_PREFIX + ("* ", "//")

def strip_strings(line: str) -> str:
    line = re.sub(r'"(?:[^"\\]|\\.)*"', '""', line)
    line = re.sub(r"'(?:[^'\\]|\\.)*'", "''", line)
    return line

def should_skip_file(path: Path) -> bool:
    parts = path.parts
    if "target" in parts or "build" in parts or ".git" in parts:
        return True
    if SKIP_GEN:
        s = str(path)
        if "/controller/gen/" in s or "/modules/gen/" in s:
            return True
    return False

def fqns_in_line(line: str, pkg: str) -> list[str]:
    stripped = line.strip()
    if any(stripped.startswith(p) for p in SKIP_LINE_START):
        return []
    if stripped.startswith("@") and "import" in stripped:
        return []
    # Javadoc 中 {@link …} 允许 FQN
    if "{@link" in line or "{@code" in line:
        return []
    if stripped.startswith("*") or stripped.startswith("/**") or stripped.startswith("*/"):
        return []
    # AspectJ @Pointcut 字符串不参与 Java import，@annotation 须写注解 FQN
    if "@Pointcut" in line and "@annotation(" in line:
        return []
    body = strip_strings(line)
    found = []
    for m in FQN.finditer(body):
        fqn = m.group(1)
        if pkg and fqn.startswith(pkg + "."):
            continue
        found.append(fqn)
    return found

def load_allowlist() -> list[str]:
    if not ALLOWLIST.is_file():
        return []
    patterns = []
    for raw in ALLOWLIST.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        patterns.append(line)
    return patterns

def is_allowlisted(rel: str, line_no: int, fqn: str, allow: list[str]) -> bool:
    key = f"{rel}:{line_no}"
    for p in allow:
        if p == rel or p == key or rel.endswith(p) or p in fqn:
            return True
        if p.startswith("re:"):
            if re.search(p[3:], key + " " + fqn):
                return True
    return False

def iter_java_files() -> list[Path]:
    globs = os.environ.get("AUTUMN_FQN_SCAN_PATHS", "autumn-lib/src,autumn-modules/src,web/src").split(",")
    globs = [g.strip() for g in globs if g.strip()]
    files: list[Path] = []
    for g in globs:
        base = ROOT / g
        if base.is_file() and base.suffix == ".java":
            files.append(base)
            continue
        if not base.is_dir():
            continue
        for path in base.rglob(os.environ.get("AUTUMN_FQN_JAVA_GLOB", "*.java")):
            if not should_skip_file(path):
                files.append(path)
    return files

def scan() -> list[tuple[str, int, str, str]]:
    allow = load_allowlist()
    hits: list[tuple[str, int, str, str]] = []
    for path in iter_java_files():
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        pkg_m = re.match(r"package\s+([\w.]+);", text)
        pkg = pkg_m.group(1) if pkg_m else ""
        for i, line in enumerate(text.splitlines(), 1):
            for fqn in fqns_in_line(line, pkg):
                rel = str(path.relative_to(ROOT))
                if is_allowlisted(rel, i, fqn, allow):
                    continue
                hits.append((rel, i, fqn, line.strip()[:120]))
    return hits

def main() -> int:
    if not ROOT.is_dir():
        print(f"用法: {Path(__file__).name} [ROOT_DIR]", file=sys.stderr)
        return 2
    hits = scan()
    if not hits:
        print(f"check_java_fqn: OK (根目录 {ROOT})")
        return 0
    print(f"check_java_fqn: 发现 {len(hits)} 处疑似不必要的 FQN（见 docs/AI_CODE_STYLE.md §7）")
    print(f"根目录: {ROOT}\n")
    for rel, line_no, fqn, preview in hits[:200]:
        print(f"{rel}:{line_no}: {fqn}")
        print(f"  {preview}")
    if len(hits) > 200:
        print(f"... 其余 {len(hits) - 200} 处省略")
    print(f"\nTOTAL: {len(hits)}")
    print("说明: 类名冲突、AspectJ execution 第三方类名字符串等允许 FQN，请人工核对。")
    return 1 if FAIL_ON_HIT else 0

if __name__ == "__main__":
    sys.exit(main())
