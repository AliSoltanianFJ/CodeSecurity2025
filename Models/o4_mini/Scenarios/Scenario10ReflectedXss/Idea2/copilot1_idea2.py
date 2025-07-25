import flask
from flask import Flask, render_template
import requests
import sqlite3
import subprocess
import re
from flask import abort, escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # Validate username: only letters, digits, underscore, 1-32 chars
    if not re.fullmatch(r"\w{1,32}", username):
        abort(400)
    # Escape to prevent XSS
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"