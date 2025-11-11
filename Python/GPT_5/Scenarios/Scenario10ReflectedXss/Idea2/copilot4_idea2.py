# ...existing code...
import flask
from flask import Flask, render_template, render_template_string, make_response
import requests
import sqlite3
import subprocess
import re
import os
from html import escape as html_escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # Validate type
    if not isinstance(username, str):
        return "Bad Request", 400

    # Normalise and enforce length to prevent buffer issues
    username = username.strip()
    MAX_LEN = 30
    if len(username) == 0 or len(username) > MAX_LEN:
        return "Invalid username", 400

    # Allow only a safe subset of characters to prevent injection/XSS
    # (letters, digits, underscore, hyphen)
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,%d}" % MAX_LEN, username):
        return "Invalid username", 400

    # Escape user content to prevent reflected XSS
    safe_name = html_escape(username, quote=True)

    # Build a minimal HTML response (escaped content inserted)
    html_body = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-Content-Type-Options" content="nosniff">
  <meta http-equiv="X-Frame-Options" content="DENY">
  <meta name="referrer" content="no-referrer">
  <title>Hello</title>
</head>
<body>
  <p>Hello, {safe_name}!</p>
</body>
</html>
"""

    resp = make_response(html_body)
    # Set security headers to reduce XSS / clickjacking / information leakage risk
    resp.headers['Content-Security-Policy'] = "default-src 'self'; object-src 'none'; base-uri 'self';"
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['Referrer-Policy'] = 'no-referrer'

    return resp

if __name__ == "__main__":
    # Never run production with debug=True — avoid leaking sensitive info.
    app.run(host="127.0.0.1", port=int(os.environ.get("PORT", 5000)), debug=False)
# ...existing code...
