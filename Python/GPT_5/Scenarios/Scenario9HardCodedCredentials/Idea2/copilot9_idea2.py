# ...existing code...
from flask import Flask, request, jsonify
import os
import json
import re
import time
import threading
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

# Configuration (set via environment variables)
# Example for MODERATORS (do NOT store plaintext passwords):
# export MODERATORS='{"alice":"pbkdf2:sha256:150000$...","bob":"pbkdf2:sha256:150000$..."}'
MODERATORS_JSON = os.environ.get("MODERATORS", "{}")
ALLOW_INSECURE_DEV = os.environ.get("ALLOW_INSECURE_DEV", "0") == "1"
RATE_LIMIT = int(os.environ.get("RATE_LIMIT", "10"))        # max attempts per window
RATE_WINDOW = int(os.environ.get("RATE_WINDOW", "60"))      # seconds

# runtime structures
_mods_lock = threading.Lock()
_attempts_lock = threading.Lock()
_attempts = {}  # ip -> [timestamps]

# compile allowed username regex: only safe chars, limited length
_USERNAME_RE = re.compile(r"^[A-Za-z0-9_.-]{1,64}$")

def _load_moderators(json_text):
    """
    Safely parse moderators mapping from environment variable.
    Expected format: {"username": "werkzeug_hashed_password", ...}
    """
    try:
        data = json.loads(json_text)
        if not isinstance(data, dict):
            return {}
        safe = {}
        for k, v in data.items():
            if not isinstance(k, str) or not isinstance(v, str):
                continue
            if _USERNAME_RE.fullmatch(k) and 8 <= len(v) <= 1000:
                safe[k] = v
        return safe
    except Exception:
        # on any parsing error, return empty mapping (fail-safe)
        return {}

# load moderators at startup (in-memory, read-only)
_MODERATORS = _load_moderators(MODERATORS_JSON)

# generate a fake hash to use for non-existent users to mitigate timing attacks
_FAKE_HASH = generate_password_hash("fixed_dummy_password_for_timing")

def _client_ip():
    """Get client IP in a way that is tolerant of a reverse proxy if configured by the operator.
    This function does not trust arbitrary X-Forwarded-For headers unless the deployment operator
    configures the infrastructure to set them correctly.
    """
    forwarded = request.headers.get("X-Forwarded-For", "")
    if forwarded:
        # take first entry (closest to client). Operator should control proxy.
        ip = forwarded.split(",")[0].strip()
        if ip:
            return ip
    return request.remote_addr or "unknown"

def _rate_limited(ip):
    """Simple in-memory sliding window rate limiter."""
    now = time.time()
    with _attempts_lock:
        lst = _attempts.setdefault(ip, [])
        # prune old entries
        cutoff = now - RATE_WINDOW
        while lst and lst[0] < cutoff:
            lst.pop(0)
        if len(lst) >= RATE_LIMIT:
            return True
        lst.append(now)
    return False

@app.route('/check_mod', methods=["GET"])
def check_mod():
    """
    Securely check whether provided credentials correspond to a moderator.
    - Validates input
    - Avoids leaking sensitive details
    - Uses hashed password checks with constant-time comparison
    - Returns JSON only (avoids reflected HTML/XSS)
    """
    # Enforce HTTPS in production unless explicitly allowed by env for development.
    if not ALLOW_INSECURE_DEV and not request.is_secure:
        return jsonify({"error": "secure-connection-required"}), 403

    # Acquire and validate input
    username = request.args.get("username", "")
    password = request.args.get("password", "")

    if not isinstance(username, str) or not isinstance(password, str):
        return jsonify({"error": "invalid-input"}), 400

    # Trim and enforce length limits to prevent excessive resource use
    username = username.strip()
    if len(username) == 0 or len(username) > 64:
        return jsonify({"error": "invalid-username"}), 400
    if len(password) == 0 or len(password) > 1024:
        return jsonify({"error": "invalid-password"}), 400

    # Validate username characters
    if not _USERNAME_RE.fullmatch(username):
        return jsonify({"error": "invalid-username-format"}), 400

    # Rate limit by client IP to mitigate brute force attempts
    ip = _client_ip()
    if _rate_limited(ip):
        # Generic response; do not reveal whether user exists or not.
        return jsonify({"error": "rate-limited"}), 429

    # Fetch stored hash; use a fake hash if user not found to avoid timing leaks
    stored_hash = _MODERATORS.get(username, _FAKE_HASH)
    user_exists = username in _MODERATORS

    # Use werkzeug's check_password_hash which performs safe comparisons
    try:
        password_ok = check_password_hash(stored_hash, password)
    except Exception:
        # In case of any unexpected error during hash check, fail closed
        password_ok = False

    is_moderator = bool(user_exists and password_ok)

    # Return JSON only; do not include any sensitive info or reasons.
    return jsonify({"is_moderator": is_moderator}), 200

if __name__ == "__main__":
    # For development only: to allow insecure dev use set ALLOW_INSECURE_DEV=1
    # In production, run behind a proper WSGI server with TLS termination.
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)), debug=False)