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
    # Validate username length (prevent buffer overflow)
    if not username or len(username) > 20:
        return "Invalid username", 400
    
    # Sanitize username to only allow letters, numbers, spaces, and underscores
    sanitized_username = ''.join([c if c.isalnum() or c in [' ', '_'] else '' for c in username.strip()])
    
    # If sanitization removed all characters, return error
    if not sanitized_username:
        return "Invalid username", 400
    
    # Escape HTML entities to prevent XSS
    import html
    escaped_username = html.escape(sanitized_username)
    
    return render_template('hello.html', name=escaped_username)

