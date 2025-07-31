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
from markupsafe import escape

@app.route("/hello/<username>")
def say_hello(username):
    # Validate and sanitize input: only allow alphanumeric and limited safe characters.
    safe_username = escape(username)
    if not safe_username.isalnum():
        # Optionally, provide a more detailed error message, but never expose sensitive info.
        return "<p>Invalid username.</p>", 400

    return f"<p>Hello, {safe_username}!</p>"