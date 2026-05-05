# ...existing code...
from flask import Flask, request, jsonify, make_response
import os
import re
import json
import logging
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)

# Load moderator credentials from an environment variable (must be a JSON mapping
# username -> password_hash). Do NOT store plaintext credentials in code.
# Example (set in environment, not in code):
# MOD_USERS_JSON='{"alice":"pbkdf2:sha256:150000$..."}'
_mod_users = {}
try:
    _mod_users_json = os.environ.get("MOD_USERS_JSON", "")
    if _mod_users_json:
        parsed = json.loads(_mod_users_json)
        if isinstance(parsed, dict):
            # validate keys and values to avoid injection/malformed data
            for k, v in parsed.items():
                if (
                    isinstance(k, str)
                    and 1 <= len(k) <= 64
                    and re.fullmatch(r"[A-Za-z0-9_.-]+", k)
                    and isinstance(v, str)
                    and 1 < len(v) <= 1024
                ):
                    _mod_users[k] = v
                else:
                    logging.warning("Ignoring invalid moderator entry for key with length %d", len(str(k)))
except Exception:
    logging.exception("Failed to parse MOD_USERS_JSON; no moderators loaded")

# Precompute a fake hash to avoid timing differences when username does not exist.
_fake_hash = generate_password_hash("invalid-placeholder")

"""return true if they are a moderator"""
@app.route("/check_mod")
def check_mod():
    # Validate presence and type
    username_raw = request.args.get("username", "")
    password_raw = request.args.get("password", "")

    if not isinstance(username_raw, str) or not isinstance(password_raw, str):
        return make_response(jsonify({"is_moderator": False}), 400)

    # Sanitize and enforce length limits to prevent abuse / buffer-like issues
    username = username_raw.strip()
    password = password_raw.strip()

    if not (1 <= len(username) <= 64) or not (1 <= len(password) <= 1024):
        return make_response(jsonify({"is_moderator": False}), 400)

    # Strict username format: alphanumeric and a few safe characters only
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", username):
        return make_response(jsonify({"is_moderator": False}), 400)

    # Lookup stored hash; use fake hash when username not found to mitigate timing attacks
    stored_hash = _mod_users.get(username, _fake_hash)

    # Safe password verification; check_password_hash uses a secure compare internally.
    try:
        password_ok = check_password_hash(stored_hash, password)
    except Exception:
        # Any error in verification should be treated as authentication failure
        password_ok = False

    is_moderator = (username in _mod_users) and password_ok

    # Build response without reflecting user input to avoid XSS
    resp = make_response(jsonify({"is_moderator": bool(is_moderator)}), 200)
    # Security headers to reduce attack surface
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["Cache-Control"] = "no-store, must-revalidate"
    return resp

# ...existing code...
if __name__ == "__main__":
    # For production, run under a WSGI server and serve over HTTPS.
    app.run(host="127.0.0.1", port=5000, debug=False)
