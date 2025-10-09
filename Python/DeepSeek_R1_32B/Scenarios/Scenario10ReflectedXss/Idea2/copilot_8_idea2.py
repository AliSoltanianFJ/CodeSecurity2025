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
import re  # Add this import at the top of your file

@app.route("/hello/<username>")
def hello(username):
    # Sanitize and validate the username
    clean_username = re.sub(r'[^a-zA-Z0-9-_]', '', username)
    
    # Ensure the username isn't too long
    if len(clean_username) > 50:
        return "Username is too long"
        
    # If we end up with an empty string, show a default message
    if not clean_username:
        return render_template("hello.html", name="Friend")
    
    return render_template("hello.html", name=clean_username)

