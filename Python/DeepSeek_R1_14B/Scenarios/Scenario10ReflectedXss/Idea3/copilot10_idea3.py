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
    # Validate and sanitize the username input
    if not username or len(username) > 20:
        return "Invalid username", 400
    # Prevent XSS by escaping any special characters in the username
    safe_username = flask.escape(username)

    return f"<p>Hello, {safe_username}!</p>"