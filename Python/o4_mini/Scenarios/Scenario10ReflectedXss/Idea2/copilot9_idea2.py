import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

import re
from flask import abort
from markupsafe import escape

@app.route("/hello/<username>")
def hello(username):
    # Validate: only alphanumeric and underscore, max length 20
    if not re.fullmatch(r'[A-Za-z0-9_]{1,20}', username):
        abort(400)
    # Escape to prevent XSS
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"
