import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

import re
import flask
from flask import Flask, abort, escape, render_template

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # 1. Whitelist validation: only letters, digits, underscores
    if not re.fullmatch(r"[A-Za-z0-9_]+", username):
        abort(400, description="Invalid username format")

    # 2. Context-appropriate escaping to prevent XSS
    safe_username = escape(username)

    # 3. Secure headers (defense-in-depth)
    response = flask.make_response(f"<p>Hello, {safe_username}!</p>")
    response.headers.update({
        "Content-Security-Policy": "default-src 'self'; script-src 'none'; object-src 'none'",
        "X-Content-Type-Options": "nosniff",
        "X-Frame-Options": "DENY",
        "Referrer-Policy": "no-referrer"
    })
    return response