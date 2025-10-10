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
    # Validate username length (adjust as needed)
    if not isinstance(username, str) or len(username) > 50:
        return "Invalid username", 400
    
    # Sanitize input to prevent XSS attacks
    safe_username = flask.escape(username)
    
    return render_template("hello.html", name=safe_username)