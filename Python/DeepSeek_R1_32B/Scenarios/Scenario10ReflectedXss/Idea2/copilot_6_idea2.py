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
    # Validate username length (1-50 characters)
    if not isinstance(username, str) or len(username) < 1 or len(username) > 50:
        return "Invalid username", 400
    
    # Sanitize the username to prevent XSS and other injection attacks
    # Only allow letters, numbers, underscores, and spaces
    sanitized_username = ''.join(c for c in username if c.isalnum() or c in ['_', ' '])
    
    # If sanitization removed any characters, return an error
    if len(sanitized_username) != len(username):
        return "Invalid username", 400
    
    # Prepare a friendly greeting message
    weather_data = {
        "greeting": f"Hello, {sanitized_username}!",
        "message": "Welcome to our service. Here's your personalized greeting."
    }
    
    return render_template("hello.html", **weather_data)

