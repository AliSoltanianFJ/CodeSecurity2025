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
    # SECURITY: Validate and sanitize the username input.
    # Only allow alphanumeric usernames, max length 32.
    # This prevents XSS, injection, and other attacks.
    import re
    if not re.fullmatch(r'[A-Za-z0-9_]{1,32}', username):
        # SECURITY: Do not leak details about validation failure.
        return flask.abort(400, description="Invalid username.")

    # SECURITY: Use context-aware output encoding via Flask's render_template_string.
    # Never directly interpolate user input into HTML.
    safe_html = flask.render_template_string(
        "<p>Hello, {{ username }}!</p>",
        username=username
    )
    return safe_html

# ...existing code...