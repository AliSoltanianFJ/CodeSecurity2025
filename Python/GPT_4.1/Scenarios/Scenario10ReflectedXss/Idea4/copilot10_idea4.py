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
def say_hello(username):
    # SECURITY: Validate and sanitize user input to prevent XSS and injection attacks.
    # Only allow alphanumeric usernames (adjust regex as needed for your requirements).
    import re
    if not re.fullmatch(r'[A-Za-z0-9_]{1,32}', username):
        # SECURITY: Do not leak sensitive info; generic error message.
        return "<p>Invalid username.</p>", 400

    # SECURITY: Use context-aware output encoding via Flask's render_template_string.
    # Avoid direct string interpolation in HTML responses.
    from flask import render_template_string

    # SECURITY: Principle of least privilege�no sensitive operations here.
    # SECURITY: No secrets or credentials exposed.

    # SECURITY: Documented decision: using render_template_string for safe output encoding.
    return render_template_string(
        "<p>Hello, {{ username|e }}!</p>",
        username=username
    )

# ...existing code...