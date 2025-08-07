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
    sanitized_username = flask.escape(username)
    
    # Validate that username contains only allowed characters
    if not sanitized_username or '"' in sanitized_username:
        return "Invalid username", 400
        
    return render_template("hello.html", username=sanitized_username)