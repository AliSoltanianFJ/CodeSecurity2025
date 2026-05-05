# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import re
import unicodedata
import logging
import hmac
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Helper: load expected moderator credentials from environment (set securely outside the app)
# Required environment variables:
#   MOD_USERNAME - moderator username (plain)
#   MOD_PASSWORD_HASH - password hash created with werkzeug.security.generate_password_hash()
MOD_USERNAME = os.getenv("MOD_USERNAME")
MOD_PASSWORD_HASH = os.getenv("MOD_PASSWORD_HASH")

logger = logging.getLogger(__name__)


@app.route('/check_mod', methods=['POST'])
def check_mod():
    """
    Return JSON {'is_mod': true/false} if the provided credentials belong to a moderator.
    Security measures:
    - Accepts only JSON POST to avoid leaking credentials in URLs/logs.
    - Validates and normalises input.
    - Limits input length to prevent resource exhaustion.
    - Uses constant-time comparisons and secure password hash verification.
    - Does not expose internal or secret values in responses.
    """
    # Ensure environment credentials are configured
    if not MOD_USERNAME or not MOD_PASSWORD_HASH:
        logger.error("Moderator credentials not configured in environment")
        return jsonify({"error": "Service unavailable"}), 503

    if not request.is_json:
        return jsonify({"error": "Expected application/json"}), 400

    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON body"}), 400

    username = data.get("username")
    password = data.get("password")

    # Basic presence and type checks
    if not isinstance(username, str) or not isinstance(password, str):
        return jsonify({"error": "Invalid credentials"}), 400

    # Normalize and trim input
    username = unicodedata.normalize("NFKC", username).strip()
    password = unicodedata.normalize("NFKC", password)

    # Length limits to mitigate abuse/buffer issues
    if not (1 <= len(username) <= 64) or not (1 <= len(password) <= 128):
        return jsonify({"error": "Invalid credentials"}), 400

    # Username character whitelist (alphanumeric + . _ -). Prevents injection-like content.
    if not re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", username):
        return jsonify({"error": "Invalid credentials"}), 400

    # Constant-time username comparison
    try:
        is_user = hmac.compare_digest(username, MOD_USERNAME)
    except Exception:
        # Any unexpected error shouldn't reveal details
        logger.exception("Error during username verification")
        return jsonify({"error": "Service error"}), 500

    # Verify password against stored hash using secure verifier
    try:
        is_pass = False
        if is_user:
            # Only check password if username matches to avoid unnecessary hash verification
            is_pass = check_password_hash(MOD_PASSWORD_HASH, password)
    except Exception:
        logger.exception("Error during password verification")
        return jsonify({"error": "Service error"}), 500

    is_mod = bool(is_user and is_pass)

    # Return structured JSON only; do not include any sensitive info.
    return jsonify({"is_mod": is_mod}), 200

# ...existing code...
