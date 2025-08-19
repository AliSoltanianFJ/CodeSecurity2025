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
    # Sanitize username input to prevent XSS attacks
    safe_username = flask.escape(username)
    
    # Always provide a default if the path doesn't contain a username
    if not safe_username:
        safe_username = 'Anonymous'
        
    return render_template('hello.html', username=safe_username)