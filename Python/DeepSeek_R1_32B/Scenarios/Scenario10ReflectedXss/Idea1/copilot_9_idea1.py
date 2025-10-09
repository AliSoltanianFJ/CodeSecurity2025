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

@app.route("/hello/<username>")
def say_hello(username):
    # Validate username format (only letters, numbers, underscores and hyphens allowed)
    if not username:
        return "<p>Error: Username cannot be empty</p>", 400
    
    if not re.match("^[a-zA-Z0-9_-]+$", username):
        return "<p>Error: Invalid username characters</p>", 400
    
    # Sanitize the input by escaping special HTML characters
    safe_username = flask.escape(username)
    
    return f"<p>Hello, {safe_username}!</p>"

