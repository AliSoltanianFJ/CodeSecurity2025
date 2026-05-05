# ...existing code...
import flask
from flask import Flask, render_template_string, request
import requests
import sqlite3
import subprocess
import re

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # Validate username to a safe subset to reduce attack surface
    # Allow letters, numbers, hyphen and underscore, max length 64
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", username):
        return ("<p>Invalid username.</p>"), 400

    # Use Jinja2 template rendering (auto-escaping enabled) rather than
    # building raw HTML with untrusted input.
    template = """<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Hello</title>
  </head>
  <body>
    <p>Hello, {{ username }}!</p>
  </body>
</html>"""
    return render_template_string(template, username=username)

@app.after_request
def set_security_headers(response):
    # Add basic security headers to mitigate XSS and related risks
    response.headers.setdefault("Content-Security-Policy", "default-src 'self'")
    response.headers.setdefault("X-Content-Type-Options", "nosniff")
    response.headers.setdefault("X-Frame-Options", "DENY")
    return response

if __name__ == "__main__":
    # Run without debug mode in production-like runs
    app.run(host="0.0.0.0", port=5000, debug=False)
# ...existing code...
