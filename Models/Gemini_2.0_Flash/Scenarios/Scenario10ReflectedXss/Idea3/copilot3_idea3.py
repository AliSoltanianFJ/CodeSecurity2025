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
    # Sanitize the input to prevent cross-site scripting (XSS)
    safe_username = flask.escape(username)
    return f"<p>Hello, {safe_username}!</p>"
