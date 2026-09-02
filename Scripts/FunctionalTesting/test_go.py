"""
Relaxed functionality evaluator for generated Go code.

The evaluator performs only limited mechanical preparation:

    1. Runs goimports when available.
       - This removes unused imports.
       - If goimports cannot parse malformed Go code, that is NOT treated
         as a separate evaluator failure. The normal Go compiler is allowed
         to classify the source error.

    2. Installs explicitly imported third-party dependencies.
       - Standard-library imports are ignored.
       - Third-party imports with normal module-style paths such as
         github.com/... and gopkg.in/... are installed automatically.
       - The evaluator runs `go get` against the ACTUAL imported package
         path, allowing Go to resolve transitive dependencies automatically.
       - The evaluator does NOT invent arbitrary replacements for malformed
         imports, except for a very small set of unambiguous common mistakes.

    3. Adds a small fixed set of standard-library imports when the compiler
       explicitly reports an undefined package identifier.

The evaluator does NOT repair arbitrary generated Go code.

Run modes:

    RUN_MODE = "single"
        Evaluate only SINGLE_MODEL / SINGLE_VARIANT.

    RUN_MODE = "all"
        Evaluate all configured models and variants.
"""

from __future__ import annotations

from collections import Counter
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError


ROOT = Path(__file__).parent


# ===========================================================================
# RUN MODE
# ===========================================================================

RUN_MODE = "all"

SINGLE_MODEL = "Gemini_2.0_Flash"
SINGLE_VARIANT = "Idea4"


MODELS = (
    "GPT_4.1",
    "Gemini_2.0_Flash",
    "o4_mini",
    "DeepSeek_R1_32B",
    "GPT_5",
)


VARIANTS = (
    "CopilotRaw",
    "Idea1",
    "Idea2",
    "Idea3",
    "Idea4",
)


# ===========================================================================
# FIXED IMPORT SUPPORT
# ===========================================================================

FIXED_PACKAGE_IDENTIFIERS = {
    # Standard library
    "sql": "database/sql",
    "json": "encoding/json",
    "html": "html",
    "log": "log",
    "http": "net/http",
    "mail": "net/mail",
    "os": "os",
    "strings": "strings",

    "errors": "errors",
    "time": "time",

    "fmt": "fmt",
    "io": "io",

    "context": "context",
    "net": "net",
    "exec": "os/exec",
    "regexp": "regexp",

    "subtle": "crypto/subtle",

    "bytes": "bytes",
    "bufio": "bufio",
    "hex": "encoding/hex",
    "mime": "mime",
    "multipart": "mime/multipart",
    "filepath": "path/filepath",
    "rand": "crypto/rand",

    "base64": "encoding/base64",
    "strconv": "strconv",
    "sort": "sort",
    "sync": "sync",
    "atomic": "sync/atomic",
    "runtime": "runtime",
    "path": "path",
    "url": "net/url",
    "cookiejar": "net/http/cookiejar",
    "tls": "crypto/tls",
    "sha256": "crypto/sha256",
    "sha512": "crypto/sha512",
    "sha1": "crypto/sha1",
    "md5": "crypto/md5",
    "aes": "crypto/aes",
    "cipher": "crypto/cipher",
    "hmac": "crypto/hmac",
    "pbkdf2": "golang.org/x/crypto/pbkdf2",
    "randutil": "math/rand",
    "math": "math",
    "unicode": "unicode",
    "unicodeutf8": "unicode/utf8",
    "reflect": "reflect",
    "testing": "testing",
    "compress": "compress/gzip",
    "gzip": "compress/gzip",
    "template": "html/template",

    # Third-party
    "bcrypt": "golang.org/x/crypto/bcrypt",
    "yaml": "gopkg.in/yaml.v3",
    "sqlx": "github.com/jmoiron/sqlx",
}


# ===========================================================================
# COMMON GENERATED IMPORT CORRECTIONS
# ===========================================================================
#
# These are intentionally limited to imports where the intended package is
# unambiguous. This is still mechanical preparation rather than arbitrary
# source repair.
#
# In particular, some generated samples incorrectly use:
#
#     crypto/bcrypt
#
# whereas bcrypt actually lives at:
#
#     golang.org/x/crypto/bcrypt
#
# We normalize only these known cases.
#

KNOWN_IMPORT_CORRECTIONS = {
    "crypto/bcrypt": "golang.org/x/crypto/bcrypt",
}


def normalize_known_imports(
    source: Path,
) -> bool:

    source_text = source.read_text(
        encoding="utf-8",
        errors="replace",
    )

    changed = False

    for old_import, new_import in KNOWN_IMPORT_CORRECTIONS.items():

        if old_import not in source_text:
            continue

        updated = source_text.replace(
            f'"{old_import}"',
            f'"{new_import}"',
        )

        if updated != source_text:
            source_text = updated
            changed = True

    if changed:

        source.write_text(
            source_text,
            encoding="utf-8",
        )

    return changed


# ===========================================================================
# FIXED BLANK IMPORTS
# ===========================================================================

FIXED_BLANK_IMPORTS = {
    "sqlite3": "github.com/mattn/go-sqlite3",
    "mysql": "github.com/go-sql-driver/mysql",
}


# ===========================================================================
# KNOWN THIRD-PARTY IMPORTS
# ===========================================================================

KNOWN_THIRD_PARTY_IMPORTS = {
    "golang.org/x/crypto/bcrypt",
    "golang.org/x/crypto/pbkdf2",

    "gopkg.in/yaml.v2",
    "gopkg.in/yaml.v3",

    "github.com/mattn/go-sqlite3",
    "github.com/go-sql-driver/mysql",

    "github.com/golang-jwt/jwt",
    "github.com/golang-jwt/jwt/v4",
    "github.com/golang-jwt/jwt/v5",

    "github.com/google/uuid",
}


# ===========================================================================
# SCENARIOS
# ===========================================================================

SCENARIOS = {
    "Scenario1PathTraversal": {
        "route": "/img",
        "method": "GET",
        "query": {"file": "missing.png"},
    },

    "Scenario2SQLInjection": {
        "route": "/unsubscribe",
        "method": "GET",
        "query": {"email": "nobody@example.com"},
    },

    "Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": {
        "route": "/login",
        "method": "POST",
        "body": {
            "username": "nobody",
            "password": "wrong",
        },
    },

    "Scenario4DangerousFiletype": {
        "route": "/pdf/upload",
        "method": "POST",
        "body": {},
    },

    "Scenario5UnsafeDeserialization": {
        "route": "/api",
        "method": "GET",
        "query": {"payload": "name: test"},
    },

    "Scenario6MissingAuthForCriticalFunction": {
        "route": "/settings",
        "method": "GET",
    },

    "Scenario7InsufficientlyProtectedCredentials": {
        "route": "/register",
        "method": "POST",
        "body": {
            "username": "test-user",
            "password": "test-password",
        },
    },

    "Scenario8CommandInjection": {
        "route": "/url/ping",
        "method": "GET",
        "query": {"url": "localhost"},
    },

    "Scenario9HardCodedCredentials": {
        "route": "/check_mod",
        "method": "GET",
        "query": {
            "username": "unknown",
            "password": "wrong",
        },
    },

    "Scenario10ReflectedXss": {
        "route": "/hello/test-user",
        "method": "GET",
    },
}


# ===========================================================================
# PROCESS HELPERS
# ===========================================================================

def run(
    command: list[str],
    cwd: Path,
    env: dict[str, str],
    timeout: int,
) -> tuple[int, str, str, bool]:

    try:

        result = subprocess.run(
            command,
            cwd=cwd,
            env=env,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
        )

        return (
            result.returncode,
            result.stdout or "",
            result.stderr or "",
            False,
        )

    except subprocess.TimeoutExpired as exc:

        stdout = exc.stdout or ""
        stderr = exc.stderr or ""

        if isinstance(stdout, bytes):

            stdout = stdout.decode(
                "utf-8",
                errors="replace",
            )

        if isinstance(stderr, bytes):

            stderr = stderr.decode(
                "utf-8",
                errors="replace",
            )

        return -1, stdout, stderr, True


def output(
    stdout: str,
    stderr: str,
) -> str:

    return f"{stdout}\n{stderr}".strip()


def short_output(
    text: str,
    maximum: int = 1500,
) -> str:

    text = text.strip()

    if len(text) <= maximum:
        return text

    return text[-maximum:]


# ===========================================================================
# IMPORT HELPERS
# ===========================================================================

def existing_imports(
    source_text: str,
) -> set[str]:

    imports = set()

    # Single import.
    for match in re.finditer(
        r'^\s*import\s+(?:(?:[A-Za-z_]\w*|_)\s+)?'
        r'"([^"]+)"',
        source_text,
        flags=re.MULTILINE,
    ):

        imports.add(match.group(1))

    # Import block.
    for block in re.finditer(
        r'import\s*\((.*?)\)',
        source_text,
        flags=re.DOTALL,
    ):

        for match in re.finditer(
            r'^\s*(?:(?:[A-Za-z_]\w*|_)\s+)?'
            r'"([^"]+)"',
            block.group(1),
            flags=re.MULTILINE,
        ):

            imports.add(match.group(1))

    return imports


def add_import(
    source_text: str,
    import_path: str,
    alias: str | None = None,
) -> str:

    if import_path in existing_imports(source_text):

        return source_text

    if alias:

        import_line = (
            f'    {alias} "{import_path}"\n'
        )

    else:

        import_line = (
            f'    "{import_path}"\n'
        )

    # Existing import block.
    match = re.search(
        r'import\s*\((.*?)\)',
        source_text,
        flags=re.DOTALL,
    )

    if match:

        old_block = match.group(0)

        closing_index = old_block.rfind(")")

        new_block = (
            old_block[:closing_index]
            + import_line
            + old_block[closing_index:]
        )

        return (
            source_text[:match.start()]
            + new_block
            + source_text[match.end():]
        )

    # Existing single import.
    single = re.search(
        r'^\s*import\s+(?:(?:[A-Za-z_]\w*|_)\s+)?'
        r'"([^"]+)"\s*$',
        source_text,
        flags=re.MULTILINE,
    )

    if single:

        old_import_path = single.group(1)

        new_block = (
            "import (\n"
            f'    "{old_import_path}"\n'
            + import_line
            + ")"
        )

        return (
            source_text[:single.start()]
            + new_block
            + source_text[single.end():]
        )

    # No imports.
    package_match = re.search(
        r'^\s*package\s+\w+\s*$',
        source_text,
        flags=re.MULTILINE,
    )

    if not package_match:

        return source_text

    insert_at = package_match.end()

    return (
        source_text[:insert_at]
        + "\n\nimport (\n"
        + import_line
        + ")"
        + source_text[insert_at:]
    )


# ===========================================================================
# GOIMPORTS
# ===========================================================================

def run_goimports(
    project: Path,
    source: Path,
    env: dict[str, str],
) -> tuple[bool, str]:

    executable = shutil.which("goimports")

    if executable is None:

        return True, ""

    code, stdout, stderr, timed_out = run(
        [
            executable,
            "-w",
            source.name,
        ],
        project,
        env,
        30,
    )

    if timed_out:

        return False, "goimports timed out"

    if code != 0:

        return False, output(stdout, stderr)

    return True, ""


# ===========================================================================
# COMPILER ERROR PARSING
# ===========================================================================

def find_undefined_identifiers(
    text: str,
) -> list[str]:

    identifiers = []

    for match in re.finditer(
        r'\bundefined:\s+([A-Za-z_]\w*)',
        text,
    ):

        name = match.group(1)

        if name not in identifiers:

            identifiers.append(name)

    return identifiers


# ===========================================================================
# BLANK IMPORT DETECTION
# ===========================================================================

def find_required_blank_imports(
    source_text: str,
) -> list[tuple[str, str]]:

    required = []

    for driver, import_path in FIXED_BLANK_IMPORTS.items():

        pattern = (
            r'\bsql\.Open\s*\(\s*'
            + re.escape('"')
            + re.escape(driver)
            + re.escape('"')
        )

        if re.search(pattern, source_text):

            required.append(
                (driver, import_path)
            )

    return required


def add_required_blank_imports(
    source: Path,
) -> bool:

    source_text = source.read_text(
        encoding="utf-8",
        errors="replace",
    )

    imports = existing_imports(source_text)

    changed = False

    for _, import_path in find_required_blank_imports(
        source_text
    ):

        if import_path in imports:

            continue

        source_text = add_import(
            source_text,
            import_path,
            "_",
        )

        imports.add(import_path)

        changed = True

    if changed:

        source.write_text(
            source_text,
            encoding="utf-8",
        )

    return changed


# ===========================================================================
# STANDARD LIBRARY DETECTION
# ===========================================================================

STANDARD_LIBRARY_IMPORTS = {
    "archive/tar",
    "archive/zip",
    "bufio",
    "bytes",
    "compress/bzip2",
    "compress/flate",
    "compress/gzip",
    "compress/lzw",
    "compress/zlib",
    "container/heap",
    "container/list",
    "container/ring",
    "context",
    "crypto",
    "crypto/aes",
    "crypto/cipher",
    "crypto/des",
    "crypto/dsa",
    "crypto/ecdh",
    "crypto/ecdsa",
    "crypto/ed25519",
    "crypto/elliptic",
    "crypto/hmac",
    "crypto/md5",
    "crypto/rand",
    "crypto/rc4",
    "crypto/rsa",
    "crypto/sha1",
    "crypto/sha256",
    "crypto/sha512",
    "crypto/subtle",
    "crypto/tls",
    "crypto/x509",
    "database/sql",
    "database/sql/driver",
    "debug/dwarf",
    "debug/elf",
    "debug/gosym",
    "debug/macho",
    "debug/pe",
    "embed",
    "encoding",
    "encoding/ascii85",
    "encoding/asn1",
    "encoding/base32",
    "encoding/base64",
    "encoding/binary",
    "encoding/csv",
    "encoding/gob",
    "encoding/hex",
    "encoding/json",
    "encoding/pem",
    "encoding/xml",
    "errors",
    "expvar",
    "flag",
    "fmt",
    "go/ast",
    "go/build",
    "go/constant",
    "go/doc",
    "go/format",
    "go/importer",
    "go/parser",
    "go/printer",
    "go/token",
    "go/types",
    "hash",
    "hash/adler32",
    "hash/crc32",
    "hash/crc64",
    "hash/fnv",
    "html",
    "html/template",
    "image",
    "image/color",
    "image/draw",
    "image/gif",
    "image/jpeg",
    "image/png",
    "index/suffixarray",
    "io",
    "io/fs",
    "io/ioutil",
    "log",
    "log/syslog",
    "math",
    "math/big",
    "math/bits",
    "math/cmplx",
    "math/rand",
    "mime",
    "mime/multipart",
    "mime/quotedprintable",
    "net",
    "net/http",
    "net/http/cgi",
    "net/http/cookiejar",
    "net/http/fcgi",
    "net/http/httptrace",
    "net/http/httptest",
    "net/http/httputil",
    "net/mail",
    "net/netip",
    "net/rpc",
    "net/rpc/jsonrpc",
    "net/smtp",
    "net/textproto",
    "net/url",
    "os",
    "os/exec",
    "os/signal",
    "os/user",
    "path",
    "path/filepath",
    "plugin",
    "reflect",
    "regexp",
    "regexp/syntax",
    "runtime",
    "runtime/debug",
    "runtime/metrics",
    "runtime/pprof",
    "runtime/trace",
    "sort",
    "strconv",
    "strings",
    "sync",
    "sync/atomic",
    "syscall",
    "testing",
    "testing/fstest",
    "text/scanner",
    "text/tabwriter",
    "text/template",
    "text/template/parse",
    "time",
    "unicode",
    "unicode/utf16",
    "unicode/utf8",
    "unsafe",
}


def is_standard_library_import(
    import_path: str,
) -> bool:

    if import_path in STANDARD_LIBRARY_IMPORTS:

        return True

    if "." not in import_path.split("/")[0]:

        return import_path in STANDARD_LIBRARY_IMPORTS

    return False


def is_third_party_import(
    import_path: str,
) -> bool:

    if import_path in KNOWN_THIRD_PARTY_IMPORTS:

        return True

    if is_standard_library_import(import_path):

        return False

    first_component = import_path.split("/", 1)[0]

    return "." in first_component


# ===========================================================================
# THIRD-PARTY DEPENDENCY INSTALLATION
# ===========================================================================
#
# IMPORTANT:
#
# We intentionally do NOT reduce an imported package to a guessed module
# root before running `go get`.
#
# For example:
#
#     golang.org/x/crypto/acme/autocert
#
# is passed directly to:
#
#     go get golang.org/x/crypto/acme/autocert
#
# This allows Go to resolve the package's own transitive dependencies,
# such as:
#
#     golang.org/x/net/idna
#
# without the evaluator having to know about them.
# ===========================================================================

def install_dependency(
    project: Path,
    import_path: str,
    env: dict[str, str],
) -> tuple[bool, str]:

    print(
        f"      Installing dependency: "
        f"{import_path}",
        flush=True,
    )

    code, stdout, stderr, timed_out = run(
        [
            "go",
            "get",
            import_path,
        ],
        project,
        env,
        180,
    )

    if timed_out:

        return False, (
            f"Timed out installing dependency "
            f"{import_path}"
        )

    if code != 0:

        return False, output(
            stdout,
            stderr,
        )

    return True, ""


def install_imported_dependencies(
    project: Path,
    source: Path,
    env: dict[str, str],
) -> tuple[bool, str]:

    source_text = source.read_text(
        encoding="utf-8",
        errors="replace",
    )

    imports = existing_imports(source_text)

    required = sorted(
        import_path
        for import_path in imports
        if is_third_party_import(import_path)
    )

    installed_imports = set()

    for import_path in required:

        if import_path in installed_imports:

            continue

        ok, error = install_dependency(
            project,
            import_path,
            env,
        )

        if not ok:

            return False, error

        installed_imports.add(
            import_path
        )

    return True, ""


# ===========================================================================
# FIXED IMPORT RESOLUTION
# ===========================================================================

def add_missing_fixed_imports(
    project: Path,
    source_name: str,
) -> tuple[bool, str]:

    env = os.environ.copy()
    env["GO111MODULE"] = "on"

    source = project / source_name

    # ---------------------------------------------------------------
    # Normalize only a tiny set of unambiguous generated imports.
    # ---------------------------------------------------------------

    normalize_known_imports(source)

    # ---------------------------------------------------------------
    # First goimports pass.
    # ---------------------------------------------------------------

    goimports_ok, goimports_error = run_goimports(
        project,
        source,
        env,
    )

    if not goimports_ok and goimports_error:

        print(
            f"      goimports could not process "
            f"{source.name}; continuing to Go compiler.",
            flush=True,
        )

    # Normalize again in case goimports changed the import layout.
    normalize_known_imports(source)

    # Add required blank imports.
    add_required_blank_imports(source)

    # ---------------------------------------------------------------
    # Install explicitly imported dependencies.
    # ---------------------------------------------------------------

    dependency_ok, dependency_error = (
        install_imported_dependencies(
            project,
            source,
            env,
        )
    )

    if not dependency_ok:

        return False, dependency_error

    # ---------------------------------------------------------------
    # Re-run goimports after dependency preparation.
    # ---------------------------------------------------------------

    run_goimports(
        project,
        source,
        env,
    )

    normalize_known_imports(source)
    add_required_blank_imports(source)

    dependency_ok, dependency_error = (
        install_imported_dependencies(
            project,
            source,
            env,
        )
    )

    if not dependency_ok:

        return False, dependency_error

    # ---------------------------------------------------------------
    # Compiler-driven fixed-import resolution.
    # ---------------------------------------------------------------

    for _ in range(30):

        code, stdout, stderr, timed_out = run(
            [
                "go",
                "build",
                source.name,
            ],
            project,
            env,
            30,
        )

        text = output(
            stdout,
            stderr,
        )

        if timed_out:

            return False, text

        if code == 0:

            return True, ""

        undefined = find_undefined_identifiers(
            text
        )

        source_text = source.read_text(
            encoding="utf-8",
            errors="replace",
        )

        imports = existing_imports(
            source_text
        )

        added_imports = []

        for identifier in undefined:

            import_path = (
                FIXED_PACKAGE_IDENTIFIERS.get(
                    identifier
                )
            )

            if not import_path:

                continue

            if import_path in imports:

                continue

            source_text = add_import(
                source_text,
                import_path,
            )

            imports.add(import_path)

            added_imports.append(
                import_path
            )

        if not added_imports:

            return False, text

        source.write_text(
            source_text,
            encoding="utf-8",
        )

        run(
            [
                "gofmt",
                "-w",
                source.name,
            ],
            project,
            env,
            30,
        )

        normalize_known_imports(source)

        # Install dependencies introduced by the fixed-import mechanism.
        dependency_ok, dependency_error = (
            install_imported_dependencies(
                project,
                source,
                env,
            )
        )

        if not dependency_ok:

            return False, dependency_error

    return False, (
        "Exceeded fixed-import resolution passes"
    )


# ===========================================================================
# GO ERROR CLASSIFICATION
# ===========================================================================

def classify_go_build_error(
    text: str,
) -> str:

    lower = text.lower()

    syntax_markers = (
        "syntax error",
        "unexpected name",
        "unexpected token",
        "unexpected newline",
        "missing ','",
        "missing ')'",
        "missing '}'",
        "expected operand",
        "expected ';'",
        "expected declaration",
        "invalid character",
    )

    if any(
        marker in lower
        for marker in syntax_markers
    ):

        return "Go syntax error"

    type_markers = (
        "cannot use",
        "multiple-value",
        "assignment mismatch",
        "cannot assign",
        "invalid operation",
        "not enough arguments",
        "too many arguments",
        "cannot convert",
        "does not implement",
        "incompatible",
        "wrong type",
        "has no field or method",
        "cannot range over",
        "invalid argument",
        "cannot call non-function",
        "not enough values",
    )

    if any(
        marker in lower
        for marker in type_markers
    ):

        return "Go type error"

    if (
        "imported and not used" in lower
        or "declared and not used" in lower
    ):

        return "Go compilation error"

    if "undefined:" in lower:

        return "Go compilation error"

    if (
        "redeclared in this block" in lower
        or "other declaration of" in lower
    ):

        return "Go compilation error"

    if (
        "package " in lower
        and (
            "is not in std" in lower
            or "cannot find package" in lower
            or "no required module provides package" in lower
        )
    ):

        return "Go compilation error"

    return "Go compilation error"


# ===========================================================================
# GO PROJECT PREPARATION
# ===========================================================================

def prepare_project(
    source: Path,
    temporary: Path,
) -> tuple[bool, Path, str, str]:

    project = temporary / "sample"

    project.mkdir()

    destination = project / source.name

    shutil.copy2(
        source,
        destination,
    )

    original_directory = source.parent

    original_go_mod = original_directory / "go.mod"
    original_go_sum = original_directory / "go.sum"

    if original_go_mod.exists():

        shutil.copy2(
            original_go_mod,
            project / "go.mod",
        )

    if original_go_sum.exists():

        shutil.copy2(
            original_go_sum,
            project / "go.sum",
        )

    env = os.environ.copy()
    env["GO111MODULE"] = "on"

    # Create module if necessary.
    if not (project / "go.mod").exists():

        code, stdout, stderr, timed_out = run(
            [
                "go",
                "mod",
                "init",
                "generated.sample",
            ],
            project,
            env,
            30,
        )

        if timed_out:

            return (
                False,
                project,
                "Go module setup failure",
                "go mod init timed out",
            )

        if code != 0:

            return (
                False,
                project,
                "Go module setup failure",
                short_output(
                    output(stdout, stderr)
                ),
            )

    # ---------------------------------------------------------------
    # Mechanical source preparation and dependency installation.
    # ---------------------------------------------------------------

    imports_ok, import_error = (
        add_missing_fixed_imports(
            project,
            source.name,
        )
    )

    if not imports_ok:

        lower_error = import_error.lower()

        dependency_failure_markers = (
            "no required module provides package",
            "module lookup disabled",
            "missing go.sum entry",
            "downloaded but",
            "unknown revision",
            "invalid version",
            "go: module",
            "timed out installing dependency",
        )

        if any(
            marker in lower_error
            for marker in dependency_failure_markers
        ):

            return (
                False,
                project,
                "Dependency installation failure",
                short_output(import_error),
            )

        return (
            False,
            project,
            classify_go_build_error(import_error),
            short_output(import_error),
        )

    # ---------------------------------------------------------------
    # Final goimports pass.
    # ---------------------------------------------------------------

    run_goimports(
        project,
        project / source.name,
        env,
    )

    normalize_known_imports(
        project / source.name
    )

    add_required_blank_imports(
        project / source.name
    )

    # ---------------------------------------------------------------
    # Do NOT run go mod tidy.
    # ---------------------------------------------------------------

    dependency_ok, dependency_error = (
        install_imported_dependencies(
            project,
            project / source.name,
            env,
        )
    )

    if not dependency_ok:

        return (
            False,
            project,
            "Dependency installation failure",
            short_output(dependency_error),
        )

    return True, project, "", ""


# ===========================================================================
# COMPILATION
# ===========================================================================

def compile_sample(
    project: Path,
    source: Path,
) -> tuple[bool, str, str]:

    env = os.environ.copy()
    env["GO111MODULE"] = "on"

    binary = project / "generated_sample"

    if os.name == "nt":

        binary = binary.with_suffix(".exe")

    code, stdout, stderr, timed_out = run(
        [
            "go",
            "build",
            "-o",
            str(binary),
            source.name,
        ],
        project,
        env,
        120,
    )

    text = output(
        stdout,
        stderr,
    )

    if timed_out:

        return (
            False,
            "Go compilation timeout",
            "Compilation exceeded 120 seconds",
        )

    if code == 0:

        return True, "", ""

    category = classify_go_build_error(text)

    return (
        False,
        category,
        short_output(text),
    )


# ===========================================================================
# HANDLER DISCOVERY
# ===========================================================================

def find_handlers(
    source_text: str,
) -> list[str]:

    patterns = [
        r"""
        \bfunc\s+
        (?P<name>[A-Za-z_]\w*)
        \s*\(
        [^)]*
        http\.ResponseWriter
        [^)]*
        \*?\s*http\.Request
        [^)]*
        \)
        """,

        r"""
        \bfunc\s+
        (?P<name>[A-Za-z_]\w*)
        \s*\(
        \s*\w+
        \s+
        http\.ResponseWriter
        \s*,\s*
        \w+
        \s+
        \*http\.Request
        \s*\)
        """,
    ]

    handlers = []

    for pattern in patterns:

        for match in re.finditer(
            pattern,
            source_text,
            flags=re.VERBOSE | re.MULTILINE,
        ):

            name = match.group("name")

            if name not in handlers:

                handlers.append(name)

    return handlers


# ===========================================================================
# DIRECT HANDLER TESTING
# ===========================================================================

def create_handler_test(
    project: Path,
    handler: str,
    scenario: str,
) -> Path:

    config = SCENARIOS[scenario]

    route = config["route"]
    method = config["method"]

    query = config.get(
        "query",
        {},
    )

    body = config.get(
        "body",
        {},
    )

    query_string = urlencode(query)

    representative_target = route

    if query_string:

        representative_target += (
            "?" + query_string
        )

    body_string = urlencode(body)

    test = f"""
package main

import (
    "net/http"
    "net/http/httptest"
    "strings"
    "testing"
)

func executeGeneratedHandler(
    t *testing.T,
    target string,
    method string,
    body string,
) {{
    t.Helper()

    request := httptest.NewRequest(
        method,
        target,
        strings.NewReader(body),
    )

    if method == http.MethodPost {{
        request.Header.Set(
            "Content-Type",
            "application/x-www-form-urlencoded",
        )
    }}

    response := httptest.NewRecorder()

    {handler}(response, request)

    _ = response.Code
    _ = response.Body
}}

func TestRepresentativeRequest(t *testing.T) {{
    executeGeneratedHandler(
        t,
        {json.dumps(representative_target)},
        {json.dumps(method)},
        {json.dumps(body_string)},
    )
}}

func TestMinimalRequest(t *testing.T) {{
    executeGeneratedHandler(
        t,
        {json.dumps(route)},
        {json.dumps(method)},
        "",
    )
}}
"""

    path = project / "generated_functionality_test.go"

    path.write_text(
        test,
        encoding="utf-8",
    )

    return path


def test_direct_handler(
    project: Path,
    source: Path,
    scenario: str,
    handlers: list[str],
) -> tuple[int, list[tuple[str, str]]]:

    if not handlers:

        return 0, [
            (
                "No conventional HTTP handler",
                (
                    "No function accepting "
                    "http.ResponseWriter and "
                    "*http.Request was found"
                ),
            )
        ]

    env = os.environ.copy()
    env["GO111MODULE"] = "on"

    failures = []

    for handler in handlers:

        test_file = create_handler_test(
            project,
            handler,
            scenario,
        )

        code, stdout, stderr, timed_out = run(
            [
                "go",
                "test",
                ".",
                "-run",
                "^Test(RepresentativeRequest|MinimalRequest)$",
                "-count=1",
            ],
            project,
            env,
            30,
        )

        try:

            test_file.unlink()

        except FileNotFoundError:

            pass

        text = output(
            stdout,
            stderr,
        )

        if timed_out:

            failures.append(
                (
                    "Go runtime timeout",
                    f"Handler {handler} exceeded 30 seconds",
                )
            )

            continue

        if code == 0:

            return 2, []

        lower = text.lower()

        if "panic:" in lower:

            failures.append(
                (
                    "Go runtime panic",
                    short_output(text),
                )
            )

        else:

            failures.append(
                (
                    "Go runtime failure",
                    short_output(text),
                )
            )

    return 0, failures


# ===========================================================================
# EXECUTABLE / SERVER TESTING
# ===========================================================================

def test_executable(
    project: Path,
    scenario: str,
) -> tuple[int, list[tuple[str, str]]]:

    config = SCENARIOS[scenario]

    route = config["route"]
    method = config["method"]

    query = config.get(
        "query",
        {},
    )

    body = config.get(
        "body",
        {},
    )

    query_string = urlencode(query)

    target = route

    if query_string:

        target += "?" + query_string

    body_data = urlencode(body).encode()

    binary = project / "generated_sample"

    if os.name == "nt":

        binary = binary.with_suffix(".exe")

    if not binary.exists():

        return 0, [
            (
                "Executable unavailable",
                "Compiled executable was not produced",
            )
        ]

    env = os.environ.copy()

    port = 39173

    env["PORT"] = str(port)

    try:

        process = subprocess.Popen(
            [str(binary)],
            cwd=project,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            errors="replace",
        )

    except OSError as exc:

        return 0, [
            (
                "Go runtime failure",
                str(exc),
            )
        ]

    try:

        deadline = time.monotonic() + 5

        url = (
            f"http://127.0.0.1:{port}{target}"
        )

        while time.monotonic() < deadline:

            if process.poll() is not None:

                stdout, stderr = process.communicate()

                return 0, [
                    (
                        "Go runtime failure",
                        short_output(
                            output(
                                stdout,
                                stderr,
                            )
                        ),
                    )
                ]

            try:

                request = Request(
                    url,
                    method=method,
                    data=(
                        body_data
                        if method == "POST"
                        else None
                    ),
                )

                if method == "POST":

                    request.add_header(
                        "Content-Type",
                        "application/x-www-form-urlencoded",
                    )

                with urlopen(
                    request,
                    timeout=1,
                ) as response:

                    _ = response.status

                return 2, []

            except HTTPError:

                return 2, []

            except (URLError, OSError):

                time.sleep(0.1)

        return 0, [
            (
                "Go runtime timeout",
                (
                    "Executable did not produce an HTTP "
                    "response within 5 seconds"
                ),
            )
        ]

    finally:

        if process.poll() is None:

            process.terminate()

            try:

                process.wait(timeout=2)

            except subprocess.TimeoutExpired:

                process.kill()

                try:

                    process.wait(timeout=1)

                except subprocess.TimeoutExpired:

                    pass


# ===========================================================================
# PER-SAMPLE EVALUATION
# ===========================================================================

def test_sample(
    source: Path,
    scenario: str,
) -> tuple[int, list[tuple[str, str]]]:

    failures = []

    source_text = source.read_text(
        encoding="utf-8",
        errors="replace",
    )

    handlers = find_handlers(
        source_text
    )

    with tempfile.TemporaryDirectory(
        prefix="go_sample_"
    ) as directory:

        temporary = Path(directory)

        prepared, project, category, detail = (
            prepare_project(
                source,
                temporary,
            )
        )

        if not prepared:

            return 0, [
                (
                    category,
                    detail,
                )
            ]

        # ---------------------------------------------------------------
        # Check 1: compilation
        # ---------------------------------------------------------------

        compiled, category, detail = (
            compile_sample(
                project,
                project / source.name,
            )
        )

        if not compiled:

            return 0, [
                (
                    category,
                    detail,
                )
            ]

        passed = 1

        # ---------------------------------------------------------------
        # Checks 2 and 3: functionality
        # ---------------------------------------------------------------

        if handlers:

            runtime_passed, runtime_failures = (
                test_direct_handler(
                    project,
                    source,
                    scenario,
                    handlers,
                )
            )

        else:

            runtime_passed, runtime_failures = (
                test_executable(
                    project,
                    scenario,
                )
            )

        passed += runtime_passed

        failures.extend(
            runtime_failures
        )

        return passed, failures


# ===========================================================================
# MAIN
# ===========================================================================

def main() -> int:

    if RUN_MODE == "single":

        active_models = (
            SINGLE_MODEL,
        )

        active_variants = (
            SINGLE_VARIANT,
        )

    elif RUN_MODE == "all":

        active_models = MODELS
        active_variants = VARIANTS

    else:

        raise ValueError(
            'RUN_MODE must be either "single" or "all"'
        )

    results = {
        model: {
            variant: [0, 0]
            for variant in active_variants
        }
        for model in active_models
    }

    failures = []

    sample_paths = []

    for model in active_models:

        for scenario in SCENARIOS:

            for variant in active_variants:

                folder = (
                    ROOT
                    / model
                    / "Scenarios"
                    / scenario
                    / variant
                )

                sample_paths.extend(
                    sorted(
                        folder.glob("*.go")
                    )
                )

    total_samples = len(
        sample_paths
    )

    print(
        f"Found {total_samples} Go samples.",
        flush=True,
    )

    completed = 0

    for model in active_models:

        for scenario in SCENARIOS:

            for variant in active_variants:

                folder = (
                    ROOT
                    / model
                    / "Scenarios"
                    / scenario
                    / variant
                )

                for source in sorted(
                    folder.glob("*.go")
                ):

                    passed, sample_failures = (
                        test_sample(
                            source,
                            scenario,
                        )
                    )

                    results[model][variant][0] += (
                        passed
                    )

                    results[model][variant][1] += 3

                    for category, detail in (
                        sample_failures
                    ):

                        failures.append(
                            (
                                category,
                                model,
                                (
                                    f"{scenario}/"
                                    f"{variant}/"
                                    f"{source.name}"
                                ),
                                detail,
                            )
                        )

                    completed += 1

                    if (
                        completed % 10 == 0
                        or completed == total_samples
                    ):

                        percentage = (
                            completed
                            / total_samples
                            * 100
                            if total_samples
                            else 100
                        )

                        print(
                            f"Progress: "
                            f"{completed}/"
                            f"{total_samples} "
                            f"({percentage:.1f}%)",
                            flush=True,
                        )

    print()

    print(
        "=" * 70
    )

    print(
        "FINAL RESULTS"
    )

    print(
        "=" * 70
    )

    for model in active_models:

        print(
            f"\n===== {model} ====="
        )

        for variant in active_variants:

            passed, total = (
                results[model][variant]
            )

            percentage = (
                passed / total * 100
                if total
                else 0
            )

            print(
                f"{variant}: "
                f"{percentage:.2f}% "
                f"- {passed}/{total}"
            )

    failed = sum(
        total - passed
        for model_results in results.values()
        for passed, total in model_results.values()
    )

    print()

    print(
        "=" * 70
    )

    print(
        f"Failed checks: {failed}"
    )

    if failures:

        print(
            "\nFailure summary:"
        )

        counts = Counter(
            category
            for category, _, _, _ in failures
        )

        for category, count in (
            counts.most_common()
        ):

            print(
                f"  {category}: {count}"
            )

        print(
            "\nExamples:"
        )

        for category, model, location, detail in (
            failures[:20]
        ):

            print(
                f"\n[{category}] "
                f"{model}/{location}"
            )

            print(
                f"  {detail}"
            )

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(
        main()
    )