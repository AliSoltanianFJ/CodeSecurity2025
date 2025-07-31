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
    # Validate and sanitize input: only allow alphanumeric and limited safe characters
    safe_username = flask.escape(username)
    if not safe_username.isalnum():
        return "<p>Invalid username.</p>", 400
    return f"<p>Hello, {safe_username}!</p>"
