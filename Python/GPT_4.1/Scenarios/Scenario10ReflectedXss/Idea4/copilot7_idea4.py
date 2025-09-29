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

from markupsafe import escape  # Use MarkupSafe for output encoding

"""function which says hello to the user

Security decisions:
- Input is strictly validated: only allows alphanumeric usernames (prevents injection/XSS).
- Output is contextually escaped using MarkupSafe.
- No sensitive information is leaked in errors.
- No secrets or credentials are hardcoded.
- Route uses a leading slash for Flask compatibility.
"""
@app.route("/hello/<username>")
def say_hello(username):
    # Validate: Only allow alphanumeric usernames (adjust regex for stricter requirements if needed)
    if not username.isalnum():
        # Log error securely if needed, but do not leak details to user
        return render_template("error.html", message="Invalid username."), 400

    # Escape username for safe HTML output (prevents reflected XSS)
    safe_username = escape(username)
    return render_template("hello.html", username=safe_username)

# ...existing code...