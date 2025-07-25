import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
#...existing code...
import re
from markupsafe import escape
from flask import abort
#...existing code...
@app.route("/hello/<username>")
def hello(username):
    # Validate username (alphanumeric and underscore, 1�50 chars)
    if not re.fullmatch(r"[A-Za-z0-9_]{1,50}", username):
        abort(400)
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"