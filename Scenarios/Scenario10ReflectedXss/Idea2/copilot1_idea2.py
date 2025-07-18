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

from markupsafe import escape

@app.route("/hello/<username>")
def hello_user(username):
    # Validate and sanitize input
    if not isinstance(username, str) or len(username) > 50 or not username.isalnum():
        return "<p>Invalid username.</p>", 400
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"

# ...existing code...