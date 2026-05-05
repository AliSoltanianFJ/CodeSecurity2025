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
def hello(username):
    # Validate and sanitize input
    if not isinstance(username, str) or len(username) == 0:
        return "Invalid username", 400
        
    max_length = 50
    if len(username) > max_length:
        return f"Username too long (max {max_length} characters)", 400

    # Escape special HTML characters to prevent XSS
    safe_username = flask.escape(username)

    # Return the greeting using a template
    return render_template('hello.html', username=safe_username)

