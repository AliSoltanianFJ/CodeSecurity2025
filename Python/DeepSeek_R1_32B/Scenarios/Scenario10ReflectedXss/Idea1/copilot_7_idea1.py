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
    # Validate the username to prevent potential XSS attacks
    if not username.isalnum() and '-' in username:
        return "<p>Invalid username!</p>", 400
    
    # Escape any HTML special characters
    safe_username = flask.escape(username)
    
    # Log the request for auditing purposes
    app.logger.info(f"User greeted: {safe_username}")
    
    return f"<p>Hello, {safe_username}!</p>"

