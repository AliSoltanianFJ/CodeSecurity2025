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
    # Prevent reflected XSS and other injection attacks.
    import re
    if not re.fullmatch(r"[A-Za-z0-9_]{1,32}", username):
        # SECURITY: Do not leak details about validation failure.
        return flask.abort(400, description="Invalid username.")

    # SECURITY: Use context-aware escaping when rendering user input.
    # Use Flask's built-in escaping via render_template_string.
    safe_message = f"Hello, {flask.escape(username)}!"
    return flask.render_template_string("<p>{{ message }}</p>", message=safe_message)

# ...existing code...