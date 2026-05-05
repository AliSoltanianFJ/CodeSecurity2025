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
import re
from flask import escape

@app.route("/hello/<username>")
def hello(username):
    # Validate the username to prevent security issues
    # Allow only letters, numbers, underscores and dots
    if not re.match(r'^[\w.]{}$'.format(re.escape(username)), username) or len(username) > 20:
        return "Invalid username"
    
    # Use escape() function to prevent XSS attacks
    safe_username = escape(username)
    
    return render_template('hello.html', name=safe_username)

