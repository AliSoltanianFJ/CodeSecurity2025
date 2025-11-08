# @author: Ali Soltanian Fard Jahromi
# Custom code scanning script for Go files to detect path traversal and weak hashing patterns.
import os
import re
import argparse
import textwrap
import json
from pathlib import Path

def find_line(text, index):
    return text.count("\n", 0, index) + 1

def snippet(text, line, radius=3):
    lines = text.splitlines()
    start = max(0, line - radius - 1)
    end = min(len(lines), line + radius)
    return "\n".join(lines[start:end])

def scan_file(filepath):
    with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
        src = f.read()

    findings = []
    # -------------------------
    # CWE-22: Path Injection / Path Traversal
    # -------------------------
    # Detect use of user-controlled filename directly in filepath.Join or os.Create
    untrusted_filename_pattern = re.compile(
        r'(?:file|header)\s*\.\s*Filename',
        re.MULTILINE
    )

    join_or_create_with_filename_pattern = re.compile(
        r'(?:filepath\s*\.\s*Join\s*\([^)]*\bheader\s*\.\s*Filename\b[^)]*\)'
        r'|os\s*\.\s*Create\s*\([^)]*\bheader\s*\.\s*Filename\b[^)]*\))',
        re.MULTILINE | re.DOTALL
    )

    # Direct Join/Create calls that use header.Filename
    for match in join_or_create_with_filename_pattern.finditer(src):
        start = match.start()
        line = find_line(src, start)
        findings.append({
            "file": filepath,
            "type": "path_traversal_untrusted_filename",
            "line": line,
            "match": match.group(0),
            "message": (
                "User-controlled filename (`header.Filename`) used directly in "
                "`filepath.Join` or `os.Create`. This may allow path traversal if not sanitized "
                "with `filepath.Base()` or equivalent validation."
            )
        })

    # Matches both var := filepath.Base(...) and var = filepath.Base(...)
    assign_pattern = re.compile(
        r'(?P<var>[A-Za-z_]\w*)\s*(?::=|=)\s*(?:path|filepath)\s*\.\s*Base\s*\([^)]*\)',
        re.MULTILINE | re.DOTALL,
    )

    # Matches direct Join(... Base(...)) usage
    join_direct_pattern = re.compile(
        r'(?:path|filepath)\s*\.\s*Join\s*\([^)]*(?:path|filepath)\s*\.\s*Base\s*\([^)]*\)[^)]*\)',
        re.MULTILINE | re.DOTALL,
    )

    # Matches direct os.Create(... Base(...)) usage
    create_direct_pattern = re.compile(
        r'os\s*\.\s*Create\s*\([^)]*(?:path|filepath)\s*\.\s*Base\s*\([^)]*\)[^)]*\)',
        re.MULTILINE | re.DOTALL,
    )

    # Scan for variable assignment pattern
    assignments = []
    for match in assign_pattern.finditer(src):
        var = match.group("var")
        start = match.start()
        line = find_line(src, start)
        assignments.append((var, line))
        findings.append({
            "file": filepath,
            "type": "assignment",
            "var": var,
            "line": line,
            "match": match.group(0),
            "message": f"Variable `{var}` assigned from filepath.Base(), check its usage for path traversal risks."
        })

    # After finding a Base() assignment
    for var, _ in assignments:
        # look for sanitization patterns in the same file
        sanitization_pattern = re.compile(
            rf'{re.escape(var)}\s*[:=]?.*(contains|ContainsAny|HasSuffix|Match|Valid|Safe|Contains)',
            re.MULTILINE | re.DOTALL | re.IGNORECASE
        )
        sanitized = sanitization_pattern.search(src)
        if sanitized:
            continue

        # existing use checks (Join/Create)
        use_pattern = re.compile(
            rf'(?:path|filepath)\s*\.\s*Join\s*\([^)]*\b{re.escape(var)}\b[^)]*\)'
            rf'|os\s*\.\s*Create\s*\([^)]*\b{re.escape(var)}\b[^)]*\)',
            re.MULTILINE | re.DOTALL,
        )
        for match in use_pattern.finditer(src):
            start = match.start()
            line = find_line(src, start)
            findings.append({
                "file": filepath,
                "type": "var_use",
                "var": var,
                "line": line,
                "match": match.group(0),
                "message": f"Variable `{var}` assigned from filepath.Base() is used in Join/Create without sanitization, may lead to path traversal."
            })


    # Direct use of Base() inside the Join/Create
    for pattern, kind in [
        (join_direct_pattern, "direct_base_in_join"),
        (create_direct_pattern, "direct_base_in_create"),
    ]:
        for match in pattern.finditer(src):
            start = match.start()
            line = find_line(src, start)
            findings.append({
                "file": filepath,
                "type": kind,
                "var": None,
                "line": line,
                "match": match.group(0),
                "message": f"Direct use of filepath.Base() inside {kind.split('_')[-1]} without sanitization may lead to path traversal."
            })

    # -------------------------
    # CWE-328: insufficient hashing algorithm (heuristic checks)
    # -------------------------
    
    # 1. direct calls where the function and the argument contain "password" or similar
    weak_hash_call_with_password = re.compile(
        r'\b(?:md5|sha1|sha256|sha512)\s*\.\s*(?:Sum|Sum1|Sum256|Sum512)\s*\([^)]*(?:password|passwd|pwd)[^)]*\)',
        re.IGNORECASE | re.MULTILINE | re.DOTALL
    )

    # 2. direct call with []byte(password) style: capture variable name
    weak_hash_call_capture = re.compile(
        r'\b(?P<func>md5|sha1|sha256|sha512)\s*\.\s*(?:Sum|Sum1|Sum256|Sum512)\s*\(\s*\[\]byte\(\s*(?P<input>[A-Za-z_]\w*)\s*\)\s*\)',
        re.IGNORECASE | re.MULTILINE | re.DOTALL
    )

    # 3. md5.New()/sha1.New()/sha256.New() followed by Write(...) with password-like arg (naive near-line heuristic)
    new_and_write_pattern = re.compile(
        r'\b(?P<func>md5|sha1|sha256)\s*\.\s*New\s*\(\s*\)\s*.*?(?:Write\s*\([^)]*(?:password|passwd|pwd)[^)]*\))',
        re.IGNORECASE | re.MULTILINE | re.DOTALL
    )

    # 4. md5.Sum([]byte(someVar)) where someVar is named 'password' or similar (generic capture)
    generic_weak_hash = re.compile(
        r'\b(?:md5|sha1|sha256|sha512)\s*\.\s*(?:Sum|Sum256)\s*\([^)]*\)',
        re.IGNORECASE | re.MULTILINE | re.DOTALL
    )

    # if the file uses a strong password hashing functions (bcrypt, argon2, scrypt), do not record those usages as being weak
    strong_pw_patterns = re.compile(r'\b(bcrypt\.GenerateFromPassword|argon2\.IDKey|scrypt\.Key)\b', re.IGNORECASE)

    has_strong_pw = bool(strong_pw_patterns.search(src))

    # 1: direct weak hash calls that mention password in args
    for m in weak_hash_call_with_password.finditer(src):
        start = m.start()
        line = find_line(src, start)
        findings.append({
            "file": filepath,
            "type": "weak_hash",
            "detail": m.group(0).strip(),
            "line": line,
            "message": "Use of general-purpose hash function on password-like input (consider bcrypt/argon2/scrypt)."
        })

    # 2: captured calls with explicit []byte(passwordVar)
    for m in weak_hash_call_capture.finditer(src):
        input_var = m.group("input")
        if re.search(r'password|passwd|pwd', input_var, re.IGNORECASE):
            start = m.start()
            line = find_line(src, start)
            findings.append({
                "file": filepath,
                "type": "weak_hash",
                "detail": m.group(0).strip(),
                "line": line,
                "message": f"Hashing variable `{input_var}` (looks like a password) with a general-purpose hash; use a password KDF."
            })

    # 3: New()+Write(password) patterns (naive)
    for m in new_and_write_pattern.finditer(src):
        start = m.start()
        line = find_line(src, start)
        findings.append({
            "file": filepath,
            "type": "weak_hash",
            "detail": m.group(0).strip(),
            "line": line,
            "message": "Stream hash (New/Write) used with password-like input; prefer bcrypt/argon2/scrypt for passwords."
        })

    # 4: generic weak hash usage: try to reduce noise by requiring 'password' nearby in the function or an insert into DB with password column
    if not has_strong_pw:
        for m in generic_weak_hash.finditer(src):
            start = m.start()
            window_start = max(0, start - 200)
            window_end = min(len(src), start + 200)
            context = src[window_start:window_end]
            if re.search(r'password|passwd|pwd', context, re.IGNORECASE) or re.search(r'INSERT\s+INTO\s+\w+\s*\([^\)]*password', context, re.IGNORECASE):
                line = find_line(src, start)
                findings.append({
                    "file": filepath,
                    "type": "weak_hash",
                    "detail": m.group(0).strip(),
                    "line": line,
                    "message": "General-purpose hash used near password storage/handling; consider using bcrypt/argon2/scrypt."
                })

    # -------------------------
    # CWE-502: Unsafe Deserialization (YAML / JSON / Gob / XML)
    # -------------------------

    deserialize_funcs = [
        r'yaml\s*\.\s*Unmarshal',
        r'json\s*\.\s*Unmarshal',
        r'xml\s*\.\s*Unmarshal',
        r'gob\s*\.\s*NewDecoder',
    ]
    input_sources = [
        r'r\s*\.\s*URL\s*\.\s*Query\s*\(\)\s*\.Get\s*\(',
        r'r\s*\.\s*FormValue\s*\(',
        r'r\s*\.\s*Body',
        r'ioutil\s*\.\s*ReadAll\s*\(\s*r\s*\.\s*Body',
    ]

    deserialization_pattern = re.compile(
        rf'({"|".join(deserialize_funcs)})\s*\([^)]*({"|".join(input_sources)})[^)]*\)',
        re.MULTILINE | re.DOTALL | re.IGNORECASE
    )

    for match in deserialization_pattern.finditer(src):
        start = match.start()
        line = find_line(src, start)
        findings.append({
            "file": filepath,
            "type": "unsafe_deserialization",
            "line": line,
            "match": match.group(0),
            "message": (
                "Potential unsafe deserialization: user-controlled input passed directly to "
                "Unmarshal/NewDecoder (CWE-502). Validate or sanitize input before deserializing."
            )
        })

    # Deduplicate findings
    unique = {}
    for f in findings:
        key = (f["file"], f.get("line", 0), f.get("type", ""), f.get("detail", "")[:120])
        unique[key] = f
    return list(unique.values())

def scan_directory(root):
    all_findings = []
    for dirpath, _, filenames in os.walk(root):
        for name in filenames:
            if not name.endswith(".go"):
                continue
            path = os.path.join(dirpath, name)
            try:
                all_findings.extend(scan_file(path))
            except Exception as e:
                print(f"Error reading {path}: {e}")
    return all_findings

def print_findings(findings):
    if not findings:
        print("No potential issues found.")
        return

    # Count non-assignment findings for summary
    non_assign_findings = [f for f in findings if f.get("type") not in ("assignment",)]
    print(f"!!  Found {len(non_assign_findings)} potential issue(s):\n")

    for f in sorted(findings, key=lambda x: (x["file"], x.get("line", 0))):
        if f.get('type') == "assignment":
            continue  # Skip assignment-only findings
        relpath = f['file']
        marker = "GPT_4.1"
        if marker in relpath:
            relpath = relpath.split(marker, 1)[1]
            if relpath.startswith("\\") or relpath.startswith("/"):
                relpath = relpath[1:]
        typ = f.get("type", "issue")
        print(f"{marker}\\{relpath}:{f.get('line', '?')}  ({typ})")
        if 'message' in f:
            print(f"    Message: {f['message']}")
        if 'detail' in f:
            print(f"    Code: {f['detail'][:400].strip()}")
        try:
            snippet_text = snippet(open(f["file"], encoding='utf-8', errors='ignore').read(), f.get("line", 1))
            print(textwrap.indent(snippet_text.strip(), "    "))
        except Exception:
            pass
        print()

def append_to_sarif(findings, sarif_path="D:\\EducationalFiles\\Massey\\MInfSc\\Re\\Scenarios\\resultsgo.sarif"):
    sarif_file = Path(sarif_path)
    if sarif_file.exists():
        try:
            sarif_data = json.loads(sarif_file.read_text(encoding="utf-8"))
        except Exception:
            sarif_data = None
    else:
        sarif_data = None

    if not sarif_data:
        sarif_data = {
            "$schema": "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.5.json",
            "version": "2.1.0",
            "runs": [{
                "tool": {
                    "driver": {
                        "name": "custom-go-path-traversal-scanner",
                        "informationUri": "",
                        "rules": [
                            {
                                "id": "GO.PATH.TRAVERSAL",
                                "name": "Path Traversal in Go code",
                                "shortDescription": {"text": "Potential path traversal vulnerability"},
                                "fullDescription": {"text": "Detects unsafe use of filepath.Base with Join/Create leading to possible path traversal."},
                                "helpUri": "",
                                "properties": {"tags": ["security", "path-traversal", "go"]}
                            },
                            {
                                "id": "GO.WEAK.HASH",
                                "name": "Insufficient Hashing Algorithm (CWE-328)",
                                "shortDescription": {"text": "Use of general-purpose cryptographic hash for password storage or verification"},
                                "fullDescription": {"text": "Detects use of md5/sha1/sha256 (or similar) directly on password-like inputs. Use a password hashing function such as bcrypt, scrypt or Argon2."},
                                "helpUri": "",
                                "properties": {"tags": ["security", "cwe-328", "crypto", "go"]}
                            }
                        ]
                    }
                },
                "results": []
            }]
        }

    results = sarif_data["runs"][0].setdefault("results", [])

    for f in findings:
        if f.get("type") == "assignment":
            continue

        rule_id = "GO.PATH.TRAVERSAL"
        if f.get("type") == "weak_hash":
            rule_id = "GO.WEAK.HASH"

        message_text = f.get("message") or f"Potential issue in {Path(f['file']).name} line {f.get('line', '?')}."
        if 'detail' in f:
            message_text = message_text + " Code: " + (f['detail'][:200].replace("\n", " "))

        result_entry = {
            "ruleId": rule_id,
            "level": "warning",
            "message": {
                "text": message_text
            },
            "locations": [{
                "physicalLocation": {
                    "artifactLocation": {
                        "uri": str(Path(f["file"]).as_posix())
                    },
                    "region": {
                        "startLine": int(f.get("line", 1))
                    }
                }
            }]
        }

        results.append(result_entry)

    sarif_file.write_text(json.dumps(sarif_data, indent=2), encoding="utf-8")

    print(f"Appended {len(findings)} findings to {sarif_path}")

def main():
    parser = argparse.ArgumentParser(description="Detect path traversal patterns and weak hashing in Go code.")
    parser.add_argument("directory", nargs="?", default=".", help="")
    args = parser.parse_args()

    root = os.path.abspath(args.directory)
    print(f"Scanning Go files under: {root}\n")
    findings = scan_directory(root)
    print_findings(findings)
    append_to_sarif(findings)

if __name__ == "__main__":
    main()
