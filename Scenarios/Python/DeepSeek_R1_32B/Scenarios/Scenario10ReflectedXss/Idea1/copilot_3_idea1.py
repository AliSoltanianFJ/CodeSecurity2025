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
def say_hello(username):
    # Validate the username using regex
    import re
    if not re.match("^[A-Za-z0-9_\-]+$", username):
        return "Invalid username"
    
    # Sanitize the username to prevent any potential XSS attacks
    sanitized_username = re.sub(r"[^\w-]", "", username)
    
    return render_template("hello.html", name=sanitized_username)

