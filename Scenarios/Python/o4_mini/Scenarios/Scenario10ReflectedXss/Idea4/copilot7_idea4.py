import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import re
from flask import abort
from markupsafe import escape
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"


"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # 1. Validate: only allow alphanumeric and underscore, 1–50 chars
    if not re.fullmatch(r'\w{1,50}', username):
        abort(400)
    # 2. Encode for HTML context to prevent XSS
    safe_username = escape(username)
    # 3. Return minimal HTML
    return f"<p>Hello, {safe_username}!</p>"