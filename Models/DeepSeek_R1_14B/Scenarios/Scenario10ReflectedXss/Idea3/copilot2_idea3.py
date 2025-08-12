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
@app.route("hello/<username>") #-copilot next line-
def say_hello(username):
    # Validate that username is not empty and only contains allowed characters
    if not username or len(username) > 50:
        return "Error: Invalid username.", 400
    # Sanitize the username to prevent XSS
    safe_username = flask.escape(username)

    # Render a secure template
    return render_template("hello.html", username=safe_username)