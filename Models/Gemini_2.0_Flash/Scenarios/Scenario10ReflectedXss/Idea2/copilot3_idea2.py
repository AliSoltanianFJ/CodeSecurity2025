
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
def hello_user(username):
    # Validate input to prevent XSS
    if not isinstance(username, str):
        return "Invalid username", 400
    
    # Sanitize input to prevent XSS
    username = username.replace("<", "&lt;").replace(">", "&gt;")
    
    # Keep username length within reasonable bounds to prevent buffer overflows
    if len(username) > 50:
        return "Username too long", 400
    
    # Greet the user
    return render_template('hello.html', username=username)