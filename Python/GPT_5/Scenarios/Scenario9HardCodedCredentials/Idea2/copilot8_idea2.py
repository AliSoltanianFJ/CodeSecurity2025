from flask import Flask, request, jsonify
from werkzeug.security import check_password_hash
import os
import re
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Configuration from environment (do not hard-code secrets)
# MOD_USERNAMES: comma-separated list of allowed moderator usernames
# MOD_PASSWORD_HASH: password hash created with werkzeug.security.generate_password_hash(...)
MOD_USERNAMES = os.environ.get("MOD_USERNAMES", "")
MOD_PASSWORD_HASH = os.environ.get("MOD_PASSWORD_HASH", "")

# Pre-compile validation regex: allow letters, digits and a few safe punctuation chars.
USERNAME_RE = re.compile(r"^[A-Za-z0-9_.@-]{3,150}$")

def _get_allowed_usernames():
    # Split and normalize environment usernames safely
    parts = [p.strip() for p in MOD_USERNAMES.split(",") if p.strip()]
    allowed = []
    for p in parts:
        if USERNAME_RE.fullmatch(p):
            allowed.append(p)
        else:
            # ignore invalid entries rather than exposing them
            logging.warning("Ignoring invalid configured moderator username")
    return set(allowed)

ALLOWED_USERNAMES = _get_allowed_usernames()

@app.route("/check_mod", methods=["GET"])
def check_mod():
    # Validate presence
    raw_username = request.args.get("username", "")
    raw_password = request.args.get("password", "")

    if not raw_username or not raw_password:
        # Do not leak which field is missing
        return jsonify({"error": "invalid credentials"}), 400

    # Normalize and validate lengths to prevent resource abuse / buffer issues
    username = raw_username.strip()
    if len(username) < 3 or len(username) > 150:
        return jsonify({"error": "invalid credentials"}), 400

    password = raw_password  # keep as-is for hash check
    if len(password) > 1024:
        # impose an upper bound on password length
        return jsonify({"error": "invalid credentials"}), 400

    # Validate username characters
    if not USERNAME_RE.fullmatch(username):
        return jsonify({"error": "invalid credentials"}), 400

    # Ensure secrets are configured server-side
    if not MOD_PASSWORD_HASH or not ALLOWED_USERNAMES:
        logging.error("Moderator configuration missing")
        # Generic error to avoid exposing configuration status
        return jsonify({"error": "service unavailable"}), 503

    # Check username is in allowed set
    if username not in ALLOWED_USERNAMES:
        # Return a uniform response to avoid user enumeration
        return jsonify({"is_moderator": False}), 200

    # Verify password hash using secure, up-to-date method
    try:
        valid = check_password_hash(MOD_PASSWORD_HASH, password)
    except Exception:
        # Avoid exposing internal errors
        logging.exception("Password hash verification failed")
        return jsonify({"error": "service unavailable"}), 503

    if valid:
        return jsonify({"is_moderator": True}), 200

    return jsonify({"is_moderator": False}), 200

if __name__ == "__main__":
    # Do NOT enable debug in production
    app.run(host="127.0.0.1", port=5000, debug=False)
