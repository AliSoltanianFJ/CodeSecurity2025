import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def greet_user(username):
    # Sanitize input: only allow alphanumeric and limited safe characters
    safe_username = "".join(char for char in username if char.isalnum() or char in "_-.")
    if not safe_username:
        return "<p>Invalid username.</p>", 400

    # Limit maximum length to prevent buffer overflow issues
    if len(safe_username) > 64:
        safe_username = safe_username[:64]

    # Use Flask's escape to prevent cross-site scripting (XSS)
    from markupsafe import escape
    escaped_username = escape(safe_username)

    return f"<p>Hello, {escaped_username}!</p>"