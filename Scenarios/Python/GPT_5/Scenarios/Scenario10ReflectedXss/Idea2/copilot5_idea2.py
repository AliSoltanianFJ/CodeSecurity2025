# ...existing code...
from flask import Flask, jsonify, abort
import re
from markupsafe import escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>", methods=["GET"])
def hello(username):
    """
    Secure greeting endpoint.
    - Validates allowed characters and length.
    - Escapes output and returns JSON to avoid HTML injection.
    - Adds strict security headers.
    """
    # Basic type and whitespace normalization
    if not isinstance(username, str):
        abort(400)
    username = username.strip()

    # Prevent overly long input (mitigates buffer-like issues)
    MAX_LEN = 30
    if len(username) == 0 or len(username) > MAX_LEN:
        return jsonify({"error": "Invalid username length"}), 400

    # Validate allowed characters: letters, digits, underscore, dot, hyphen
    if not re.fullmatch(r"[A-Za-z0-9_.-]{1,%d}" % MAX_LEN, username):
        return jsonify({"error": "Invalid characters in username"}), 400

    # Escape any special characters (defense-in-depth)
    safe_username = escape(username)

    # Return JSON (avoids rendering HTML and reflected XSS)
    resp = jsonify({"message": f"Hello, {safe_username}"})

    # Security headers to reduce XSS/CSRF and information leakage
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Content-Security-Policy"] = "default-src 'none';"

    return resp

if __name__ == "__main__":
    # Do not enable debug mode in production (it can leak secrets)
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
