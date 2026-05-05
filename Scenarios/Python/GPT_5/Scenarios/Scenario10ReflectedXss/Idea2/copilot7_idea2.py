# ...existing code...
import re
from flask import Flask, escape, abort, make_response

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # Validate type and basic shape
    if not isinstance(username, str):
        abort(400)

    # Trim whitespace and enforce length limits to prevent excessive memory use
    username = username.strip()
    if not (1 <= len(username) <= 30):
        abort(400)

    # Allow only safe characters (letters, digits, underscore, hyphen)
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        abort(400)

    # Escape before rendering to prevent reflected XSS
    safe_name = escape(username)

    # Build a safe HTML response with explicit content-type and cache control
    resp = make_response(f"<p>Hello, {safe_name}!</p>")
    resp.headers["Content-Type"] = "text/html; charset=utf-8"
    resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"

    return resp

if __name__ == "__main__":
    # Ensure debug=False in production to avoid information leaks
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
