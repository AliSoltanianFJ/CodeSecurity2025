import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import bleach
from flask import request
app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("/hello/<username>")
def say_hello(username=None):
    # Get username from route parameter or default to 'Anonymous'
    if not username:
        username = request.args.get('username', 'Anonymous')
    
    # Sanitize input to prevent XSS
    sanitized_username = bleach.clean(username, tags=[], attributes={})

    return f"<p>Hello, {sanitized_username}!</p>"