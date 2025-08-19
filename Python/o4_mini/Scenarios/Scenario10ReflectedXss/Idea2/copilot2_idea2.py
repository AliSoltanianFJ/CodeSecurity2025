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
from flask import escape
#...existing code...

@app.route("/hello/<username>")
def hello(username):
    # Validate: only alphanumeric, underscore, hyphen; max length 30
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        return "<p>Invalid username.</p>", 400
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"
#...existing code...