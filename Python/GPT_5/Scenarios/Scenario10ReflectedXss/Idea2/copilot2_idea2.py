# ...existing code...
import re
from flask import Flask, make_response, escape, request

app = Flask(__name__)

@app.route("/")
def index():
    # Minimal, safe welcome message
    resp = make_response("Hello, World!", 200)
    resp.headers["Content-Type"] = "text/plain; charset=utf-8"
    # Security headers to reduce attack surface
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    return resp

def _sanitize_username(raw: str, max_len: int = 50) -> str | None:
    """
    Validate and sanitize username:
    - Must be a string
    - Trim whitespace
    - Enforce length limit to avoid excessive allocation
    - Allow only a restricted safe character set (letters, digits, underscore, hyphen, space)
    - Return sanitized string or None if invalid
    """
    if not isinstance(raw, str):
        return None
    user = raw.strip()
    if len(user) == 0 or len(user) > max_len:
        return None
    # Only allow a small safe subset of characters
    if not re.fullmatch(r"[A-Za-z0-9_\- ]+", user):
        return None
    return user

@app.route("/hello/<username>")
def hello(username):
    # Do not perform any insecure deserialization or eval-style operations.
    safe = _sanitize_username(username)
    if safe is None:
        resp = make_response("Invalid username", 400)
        resp.headers["Content-Type"] = "text/plain; charset=utf-8"
        resp.headers["X-Content-Type-Options"] = "nosniff"
        resp.headers["X-Frame-Options"] = "DENY"
        return resp

    # Escape any remaining special characters (defense-in-depth) and return plain text
    greeting = f"Hello, {escape(safe)}!"
    resp = make_response(greeting, 200)
    resp.headers["Content-Type"] = "text/plain; charset=utf-8"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    return resp

if __name__ == "__main__":
    # Never run with debug=True in production to avoid leaking secrets
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
