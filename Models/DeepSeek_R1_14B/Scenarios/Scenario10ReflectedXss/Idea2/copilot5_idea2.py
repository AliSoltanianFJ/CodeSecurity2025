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
    # Validate and sanitize input
    if not username or len(username) > 50:
        return "Invalid username", 400
    
    # Sanitize input to prevent XSS
    safe_username = username.replace('/', '').replace('\\', '')
    
    # Use a template to safely render the response
    return render_template("hello.html", name=safe_username)