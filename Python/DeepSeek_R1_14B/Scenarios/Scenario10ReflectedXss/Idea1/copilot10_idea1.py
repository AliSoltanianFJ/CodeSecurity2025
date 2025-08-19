import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("/hello/<username>")
def say_hello(username):
    # Escape special characters to prevent XSS attacks
    from flask import escape
    escaped_username = escape(username)
    
    # Validate input to ensure it's safe
    if all(c in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ' for c in username):
        return f"<p>Hello, {escaped_username}!</p>"
    else:
        return "Invalid characters in username"