# ...existing code...
import flask
from flask import Flask, render_template
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
    """
    Say hello to the user. Username is validated against a strict whitelist
    (letters, digits, underscore, hyphen, 1-30 chars) to prevent reflected XSS.
    """
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        safe_username = "guest"
    else:
        safe_username = username
    return f"<p>Hello, {safe_username}!</p>"

if __name__ == "__main__":
    app.run(debug=False)