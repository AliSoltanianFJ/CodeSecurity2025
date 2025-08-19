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
def say_hello(username):
    # Sanitize input: only allow alphanumeric and limited safe characters
    safe_username = ''.join(char for char in username if char.isalnum() or char in {'_', '-'})
    # Limit length for safety
    safe_username = safe_username[:50]

    # Use Flask's escape to prevent cross-site scripting (XSS)
    from markupsafe import escape
    safe_username = escape(safe_username)

    return f"<p>Hello, {safe_username}!</p>"