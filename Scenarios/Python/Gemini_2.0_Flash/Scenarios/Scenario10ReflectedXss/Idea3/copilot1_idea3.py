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
    # Validate and sanitize the input. Only allow alphanumeric and limited safe characters.
    username = escape(username.strip())
    if not username.isalnum():
        return "<p>Invalid username.</p>", 400
    return f"<p>Hello, {username}!</p>"
