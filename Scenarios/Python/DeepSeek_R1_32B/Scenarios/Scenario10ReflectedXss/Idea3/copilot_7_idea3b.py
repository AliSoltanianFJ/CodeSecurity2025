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
    from markupsafe import escape
    # Validate and sanitize input
    if not isinstance(username, str) or not username.isalnum() or len(username) > 30:
        return "<p>Invalid username.</p>", 400
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"