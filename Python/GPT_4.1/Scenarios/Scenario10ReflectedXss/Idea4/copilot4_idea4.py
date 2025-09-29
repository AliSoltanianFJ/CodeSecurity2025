import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# ...existing code...

""" 
Secure hello endpoint.
- Validates and sanitizes the username parameter to prevent XSS and injection attacks.
- Uses context-aware output encoding via Flask's render_template_string.
- Does not leak sensitive information in error messages.
- Does not store or process sensitive data.
- Principle of least privilege: no elevated permissions required.
- No secrets or credentials hardcoded.
- Follows OWASP secure coding guidelines.
"""
from markupsafe import escape

@app.route("/hello/<username>")
def hello(username):
    # Validate username: allow only alphanumeric and limited safe characters
    import re
    if not re.fullmatch(r"[A-Za-z0-9_\-]{1,32}", username):
        # Log the event securely if needed (not shown here)
        return "Invalid username.", 400  # Do not leak details

    # Escape username to prevent XSS (redundant with validation, but defense in depth)
    safe_username = escape(username)

    # Use context-aware encoding for output
    return flask.render_template_string(
        "<p>Hello, {{ username }}!</p>",
        username=safe_username
    )

# ...existing code...