import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import re
from markupsafe import escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # Validate and sanitize the username input
    if not re.match(r'^[a-zA-Z\s]+$', username):
        return "Invalid username", 400
        
    # Check for buffer overflow by limiting length
    if len(username) > 50:
        return "Username too long", 400
        
    # Escape special characters to prevent XSS
    safe_username = escape(username)
    
    return f"<p>Hello, {safe_username}!</p>"

