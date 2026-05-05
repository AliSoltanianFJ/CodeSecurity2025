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
@app.route("/hello/")
def hello(username):
    # Validate input length to prevent buffer overflow
    if len(username) > 50 or len(username) < 1:
        return "Invalid username length"
        
    # Sanitize input by escaping special characters
    escaped_username = flask.escape(username)

    # Further validate input - ensure it contains only allowed characters
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_")
    if not all(c in allowed_chars for c in username):
        return "Invalid username characters"
        
    return render_template('hello.html', name=escaped_username)

