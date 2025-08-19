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
import re
from markupsafe import escape

@app.route("/hello/<username>")
def hello(username):
    # Validate username: only allow 1-30 alphanumeric characters or underscores
    if not re.fullmatch(r"[A-Za-z0-9_]{1,30}", username):
        return "Invalid username", 400
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>", 200
# ...existing code...