# ...existing code...
from flask import Flask, escape, make_response, abort, request
import re

app = Flask(__name__)

# Simple index kept from existing file
@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# Validate username: allow only a safe subset of characters and limit length
_USERNAME_RE = re.compile(r"^[A-Za-z0-9_-]{1,30}$")
_MAX_USERNAME_LEN = 30

@app.route("/hello/<username>")
def hello(username):
    # Basic type and length checks to avoid buffer-like issues
    if not isinstance(username, str):
        abort(400, "Invalid username")
    if len(username) == 0 or len(username) > _MAX_USERNAME_LEN:
        abort(400, "Username length invalid")

    # Strict validation to prevent injection/XSS and other unexpected input
    if not _USERNAME_RE.fullmatch(username):
        abort(400, "Username contains invalid characters")

    # Escape any remaining special characters (defense-in-depth)
    safe_name = escape(username)

    # Build a minimal HTML response with safe, escaped content
    html = (
        "<!doctype html>"
        "<html lang='en'><head>"
        "<meta charset='utf-8'/>"
        "<title>Hello</title>"
        "</head><body>"
        "<h1>Hello, " + safe_name + "!</h1>"
        "</body></html>"
    )

    resp = make_response(html, 200)

    # Strong response headers to mitigate XSS/clickjacking/mimetype attacks
    resp.headers["Content-Security-Policy"] = "default-src 'none'; img-src 'none'; script-src 'none'; style-src 'self';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Cache-Control"] = "no-store"

    return resp

if __name__ == "__main__":
    # Do not use debug=True in production; this is safe for local testing
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
