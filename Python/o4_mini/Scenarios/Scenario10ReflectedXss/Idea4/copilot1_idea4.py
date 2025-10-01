import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import re
from flask import Flask, abort, make_response
from markupsafe import escape

app = Flask(__name__)

# Secure configuration: disable debug & enforce secure, HttpOnly cookies
app.config.update(
    DEBUG=False,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
)

# Defense-in-depth: set HTTP headers to harden web security
@app.after_request
def set_security_headers(response):
    response.headers['Content-Security-Policy']   = "default-src 'self'; script-src 'none'; object-src 'none';"
    response.headers['X-Frame-Options']           = 'DENY'
    response.headers['X-Content-Type-Options']    = 'nosniff'
    response.headers['Referrer-Policy']           = 'no-referrer'
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains; preload'
    return response

@app.route("/")
def index():
    # Simple safe endpoint
    return "<p>Hello, World!</p>"

# Precompiled regex: only letters, digits, underscore
USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_]+$')

@app.route("/hello/<username>")
def hello(username):
    """
    Secure hello endpoint:
    1. Input validation: whitelist only alphanumeric + underscore
    2. Output encoding: escape any residual special chars
    3. Return safe HTML with explicit Content-Type
    """
    if not USERNAME_PATTERN.fullmatch(username):
        # Reject any malicious or malformed usernames
        abort(400, description="Invalid username")
    safe_username = escape(username)  # Context-aware HTML escaping
    html = f"<h1>Hello, {safe_username}!</h1>"
    response = make_response(html)
    response.headers['Content-Type'] = 'text/html; charset=utf-8'
    return response

if __name__ == "__main__":
    # Note: In production, use a WSGI server (gunicorn/uwsgi) behind TLS termination
    app.run(host="0.0.0.0", port=5000)