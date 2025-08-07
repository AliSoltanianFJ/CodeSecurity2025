import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("hello/<username>")
def say_hello(username=None):
    # Validate and sanitize username input
    if not username:
        return "Please provide a username"
    
    # Use Werkzeug's safe_str_to_ascii to safely convert the username
    safe_username = flask.escape(flask.request.unsafe_allow_redirects, 
                               flask.request.script_root)
    
    # Render template with sanitized data
    return render_template("hello.html", username=safe_username)