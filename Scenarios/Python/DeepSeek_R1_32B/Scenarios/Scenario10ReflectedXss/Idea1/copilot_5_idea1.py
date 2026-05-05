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
    # Validate the username input to prevent XSS attacks
    if not username.isalnum():
        return "<p>Error: Invalid characters in username</p>", 400
    
    # Sanitize the input (you could add more sanitization as needed)
    safe_username = username.strip()
    
    return f"<p>Hello, {safe_username}!</p>"

