from flask import Flask, request, jsonify, abort
import os
import re
import yaml
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)

# A safe default bogus hash used to equalize timing when username is unknown.
# This does not expose any secret and is generated at runtime.
_BOGUS_HASH = generate_password_hash("bogus-placeholder")

_USERNAME_RE = re.compile(r'^[A-Za-z0-9._-]{1,64}$')
# Limits to prevent excessive input (mitigates certain DoS/buffer concerns)
_MAX_PASSWORD_LEN = 128

def _load_mods_config():
    """
    Load a YAML config that maps moderator usernames -> password hashes.
    The path must be provided via the MOD_CONFIG_PATH environment variable.
    The YAML is safely parsed and validated to avoid insecure deserialization.
    Returns a dict of sanitized username -> password_hash.
    """
    cfg_path = os.environ.get("MOD_CONFIG_PATH")
    if not cfg_path:
        # Do not leak internal paths or details
        abort(500)

    # Only safe YAML loading
    try:
        with open(cfg_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
    except Exception:
        # Generic server error; avoid revealing file system structure
        abort(500)

    if not isinstance(data, dict):
        abort(500)

    mods = data.get("moderators")
    if not isinstance(mods, dict):
        abort(500)

    sanitized = {}
    for uname, phash in mods.items():
        if not isinstance(uname, str) or not isinstance(phash, str):
            continue
        if not _USERNAME_RE.fullmatch(uname):
            continue
        # limit hash length to a reasonable bound (prevents malicious huge values)
        if len(phash) > 4096:
            continue
        sanitized[uname] = phash

    return sanitized

def _get_mods_cache():
    """Load and cache moderators in app config to avoid repeated file reads."""
    mods = app.config.get("MODS_CACHE")
    if mods is None:
        mods = _load_mods_config()
        app.config["MODS_CACHE"] = mods
    return mods

@app.route("/check_mod", methods=["POST"])
def check_mod():
    """
    Check whether provided credentials belong to a moderator.
    Secure design decisions:
      - Accept JSON POST only (avoids leaking credentials in URLs/logs)
      - Require Content-Type: application/json
      - Validate and sanitize input (lengths, character set)
      - Use safe YAML parsing for config
      - Use proper password hash checks (werkzeug.security)
      - Equalize timing when username not found by checking a bogus hash
      - Return JSON only (prevents reflected XSS)
      - Avoid leaking internal details in errors
    """
    # In non-debug mode prefer TLS; if not secure and not debug, deny to avoid credential exposure
    if not request.is_secure and not app.debug:
        abort(403)

    # Require JSON
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        abort(400)

    username = data.get("username")
    password = data.get("password")
    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    username = username.strip()
    # Validate lengths
    if not username or len(username) > 64 or len(password) == 0 or len(password) > _MAX_PASSWORD_LEN:
        abort(400)

    # Validate allowed username characters
    if not _USERNAME_RE.fullmatch(username):
        abort(400)

    # Load moderators mapping (username -> password_hash)
    mods = _get_mods_cache()

    stored_hash = mods.get(username)
    if stored_hash is None:
        # Unknown user — compare against bogus hash to help avoid timing leaks
        is_valid = check_password_hash(_BOGUS_HASH, password)
    else:
        # Properly verify provided password against stored hash
        is_valid = check_password_hash(stored_hash, password)

    # Do not leak any additional information; return a simple JSON boolean.
    return jsonify(is_mod=bool(is_valid))
