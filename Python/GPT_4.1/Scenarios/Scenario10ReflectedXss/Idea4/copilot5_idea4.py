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

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    # SECURITY: Validate and sanitize the username input.
    # Only allow alphanumeric usernames (no spaces, no special chars).
    # This prevents XSS, injection, and other attacks.
    import re
    if not re.fullmatch(r'[A-Za-z0-9]+', username):
        # SECURITY: Do not leak details about validation failure.
        return flask.abort(400, description="Invalid username.")

    # SECURITY: Use context-aware output encoding via Flask's render_template.
    # Avoid direct string interpolation in HTML responses.
    # SECURITY: Do not expose sensitive information in error messages.
    return render_template("hello.html", username=username)

# ...existing code...