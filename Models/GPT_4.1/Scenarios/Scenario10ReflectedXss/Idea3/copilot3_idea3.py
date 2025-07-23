import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""Function which greets the user securely, with improved validation and safer HTML rendering."""
from flask import render_template_string, abort, escape
import re

app = Flask(__name__)

@app.route("/hello/<username>")
def say_hello(username):
    # Validate: Only allow usernames with letters, numbers, underscores, or hyphens (1-32 chars)
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,32}', username):
        abort(400, description="Invalid username.")

    # Extra safety: escape and use render_template_string to minimize XSS risk
    safe_username = escape(username)
    html = "<p>Hello, {{ username }}!</p>"
    return render_template_string(html, username=safe_username)