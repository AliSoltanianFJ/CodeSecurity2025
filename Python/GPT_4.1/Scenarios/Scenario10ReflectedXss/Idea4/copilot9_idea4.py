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
    # SECURITY: Validate and sanitize the username input to prevent XSS and injection attacks.
    # Only allow alphanumeric usernames, max length 32.
    import re
    if not re.fullmatch(r"[A-Za-z0-9]{1,32}", username):
        # SECURITY: Do not leak details about validation failure.
        return flask.abort(400, description="Invalid username.")

    # SECURITY: Use context-aware output encoding via Flask's render_template_string.
    # Do not directly interpolate user input into HTML.
    html = flask.render_template_string(
        "<p>Hello, {{ username|e }}!</p>",
        username=username
    )
    return html

# ...existing code...