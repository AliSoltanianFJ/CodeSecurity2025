# ...existing code...
from flask import Flask, request, jsonify, make_response, abort
import os
import json
import re
import logging
from typing import Dict, Optional
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Minimal logging without sensitive data
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Configuration: path to a JSON file containing { "username": "password_hash", ... }
# The file path should be provided via the MODERATORS_FILE environment variable.
# Alternatively, provide JSON via MODERATORS_JSON environment variable.
MODS_FILE = os.environ.get("MODERATORS_FILE")
MODS_JSON = os.environ.get("MODERATORS_JSON")

_USERNAME_RE = re.compile(r"^[A-Za-z0-9._-]{1,64}$")
_MIN_PW_LEN = 8
_MAX_PW_LEN = 128

def load_moderators() -> Dict[str, str]:
    """
    Load moderator username -> password_hash mapping from a trusted JSON source.
    Uses json.loads/json.load only (no pickle/yaml) to avoid insecure deserialization.
    """
    mods: Dict[str, str] = {}
    try:
        if MODS_FILE:
            if os.path.isabs(MODS_FILE) and os.path.exists(MODS_FILE):
                with open(MODS_FILE, "r", encoding="utf-8") as f:
                    data = json.load(f)
            else:
                logger.warning("MODERATORS_FILE set but file not accessible; ignoring.")
                data = {}
        elif MODS_JSON:
            data = json.loads(MODS_JSON)
        else:
            data = {}
        # Validate structure: simple mapping of str->str
        if isinstance(data, dict):
            for k, v in data.items():
                if isinstance(k, str) and isinstance(v, str):
                    # normalize username length/characters before storing
                    if _USERNAME_RE.match(k):
                        mods[k] = v
        else:
            logger.warning("Moderators data is not a mapping; ignoring.")
    except Exception:
        # Avoid leaking details
        logger.exception("Failed to load moderator configuration.")
    return mods

# Load once at startup; caller can reload if needed.
_MODERATORS = load_moderators()

def _sanitize_username(username: Optional[str]) -> Optional[str]:
    if not username:
        return None
    username = username.strip()
    if not _USERNAME_RE.match(username):
        return None
    return username

def _sanitize_password(password: Optional[str]) -> Optional[str]:
    if not password:
        return None
    if not isinstance(password, str):
        return None
    # Trim and enforce length limits
    pw = password.strip()
    if len(pw) < _MIN_PW_LEN or len(pw) > _MAX_PW_LEN:
        return None
    return pw

@app.after_request
def set_security_headers(response):
    # Minimal secure headers to reduce XSS/ MIME sniffing exposure
    response.headers.setdefault("X-Content-Type-Options", "nosniff")
    response.headers.setdefault("X-Frame-Options", "DENY")
    # CSP is conservative (only allow same-origin)
    response.headers.setdefault("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
    return response

@app.route('/check_mod', methods=["GET", "POST"])
def check_mod():
   """
   Return JSON {"is_moderator": true/false}.
   Accepts credentials via JSON body (preferred) or form/query parameters:
     { "username": "...", "password": "..." }
   Security measures:
   - Input validation and sanitization
   - Uses safe JSON parsing for moderator store
   - Uses constant-time password verification via werkzeug.security.check_password_hash
   - Does not expose whether a username exists
   - No insecure deserialization
   """
   # Prefer JSON body
   try:
       body = request.get_json(silent=True)
   except Exception:
       body = None

   if isinstance(body, dict):
       raw_username = body.get("username")
       raw_password = body.get("password")
   else:
       # Fallback to form or query parameters (request.values merges args and form)
       raw_username = request.values.get("username")
       raw_password = request.values.get("password")

   username = _sanitize_username(raw_username)
   password = _sanitize_password(raw_password)

   # If invalid input, return generic 400 without details
   if username is None or password is None:
       return make_response(jsonify({"is_moderator": False}), 400)

   # Check against loaded moderator hashes
   stored_hash = _MODERATORS.get(username)

   # To avoid revealing whether username exists, perform a constant-time check
   # against a fake hash when username not present.
   # Use a well-formed dummy hash (generated once). This prevents timing user enumeration.
   dummy_hash = "pbkdf2:sha256:150000$zZZz$7b8d3b8d7f0e5a5a3c1f6b3d9a8c2b1e5f9d3c2b1a0e9d8c7b6a5f4e3d2c1b0"
   compare_hash = stored_hash if stored_hash is not None else dummy_hash

   try:
       is_match = check_password_hash(compare_hash, password)
   except Exception:
       # In case stored hash is malformed, treat as non-match without exposing details
       logger.exception("Password hash verification failed.")
       is_match = False

   # If stored_hash is None, still is_match will be False (dummy hash won't match)
   is_moderator = bool(stored_hash is not None and is_match)

   # Always return minimal, non-sensitive JSON
   status_code = 200 if is_moderator else 200
   return make_response(jsonify({"is_moderator": is_moderator}), status_code)

# ...existing code...
if __name__ == "__main__":
    # Development server only. In production use a WSGI server (gunicorn/uwsgi) and TLS.
    app.run(host="127.0.0.1", port=5000, debug=False)
