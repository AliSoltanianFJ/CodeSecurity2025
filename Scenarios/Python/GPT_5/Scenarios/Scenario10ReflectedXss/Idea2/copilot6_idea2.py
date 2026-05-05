# ...existing code...
import re
import flask
from flask import Flask, request, escape, make_response
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# ...existing code...
@app.route("/hello/<username>")
def hello(username):
    """
    Secure greeting endpoint:
    - Validates and sanitises the username.
    - Limits length to prevent resource exhaustion.
    - Escapes output to prevent reflected XSS.
    - Sets strict response headers (CSP, X-Frame-Options, etc.).
    - Does not perform any deserialization of user input.
    """
    # Validate type and length to mitigate buffer/resource issues
    if not isinstance(username, str):
        return make_response(("Invalid username", 400))

    # Enforce a strict allowlist: letters, digits, hyphen, underscore, max 50 chars
    if len(username) == 0 or len(username) > 50:
        return make_response(("Username must be 1-50 characters", 400))

    if not re.fullmatch(r"[A-Za-z0-9_-]{1,50}", username):
        return make_response(("Username contains invalid characters", 400))

    # Escape to prevent reflected XSS
    safe_name = escape(username)

    html = f"<p>Hello, {safe_name}!</p>"

    resp = make_response(html, 200)
    resp.headers["Content-Type"] = "text/html; charset=utf-8"
    # Strong response headers to reduce XSS/CSRF/Clickjacking risks
    resp.headers["Content-Security-Policy"] = "default-src 'self'; script-src 'none'; object-src 'none';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Cache-Control"] = "no-store"

    return resp

if __name__ == "__main__":
    # Do not enable debug mode in production to avoid leaking sensitive info.
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
