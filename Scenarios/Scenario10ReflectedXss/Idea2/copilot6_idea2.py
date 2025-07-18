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
def say_hello(username):
    # Validate and sanitize input: allow only alphanumeric and limited safe characters
    safe_username = escape(username)
    if not safe_username.isalnum():
        return "<p>Invalid username. Only alphanumeric characters are allowed.</p>", 400
    return f"<p>Hello, {safe_username}!</p>"

# ...existing code...