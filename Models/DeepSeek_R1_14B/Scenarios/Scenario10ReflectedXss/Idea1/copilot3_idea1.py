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
    # Sanitize the username input to prevent XSS attacks
    from bleach import clean
    safe_username = clean(username, tags=[], attributes={}, strip=True)
    
    # Render a template with the sanitized username
    return render_template("greeting.html", username=safe_username)