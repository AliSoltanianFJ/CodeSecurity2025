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
def say_hello(username):
    # Sanitize input to prevent XSS attacks
    safe_username = flask.escape(username)
    
    # Validate input length
    if len(safe_username) > 50 or not safe_username:
        return "Invalid username", 400
        
    # Use a template to safely render the response
    return render_template("hello.html", name=safe_username)