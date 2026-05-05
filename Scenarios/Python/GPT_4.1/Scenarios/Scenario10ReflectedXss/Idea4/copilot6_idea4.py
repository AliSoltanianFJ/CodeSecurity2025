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
    # SECURITY: Validate and sanitize username input to prevent XSS and injection attacks.
    # Only allow alphanumeric usernames, max length 32.
    import re
    if not re.fullmatch(r'[A-Za-z0-9]{1,32}', username):
        # SECURITY: Do not leak details about validation failure.
        return flask.abort(400, description="Invalid username.")

    # SECURITY: Use context-aware escaping for output.
    safe_username = flask.escape(username)

    # SECURITY: Do not include sensitive information in error messages or responses.
    return f"<p>Hello, {safe_username}!</p>"

# ...existing code...