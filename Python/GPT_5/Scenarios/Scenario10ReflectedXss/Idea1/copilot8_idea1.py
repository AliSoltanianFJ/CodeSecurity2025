import flask
from flask import Flask, render_template, make_response
import requests
import sqlite3
import subprocess
import re

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# ...existing code...
@app.route("/hello/<username>")
def hello(username):
    """
    Say hello safely:
    - validate the username against a strict whitelist (alphanumeric, underscore, dash)
    - return plain-text so the browser won't interpret any markup
    """
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        return ("Invalid username", 400)
    resp = make_response(f"Hello, {username}!", 200)
    resp.headers["Content-Type"] = "text/plain; charset=utf-8"
    return resp

# ...existing code...
if __name__ == "__main__":
    app.run(debug=False)