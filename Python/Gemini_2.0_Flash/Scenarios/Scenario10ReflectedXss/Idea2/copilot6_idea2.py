
import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import html

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    # Validate input to prevent XSS
    if not isinstance(username, str):
        return "Invalid username", 400
    
    # Sanitize input to prevent XSS
    sanitized_username = html.escape(username)
    
    # Prevent buffer overflow by limiting username length
    if len(sanitized_username) > 50:
        return "Username too long", 400

    # Greet the user
    greeting = f"Hello, {sanitized_username}!"
    return greeting