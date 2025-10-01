import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # sanitize the user‐supplied value to prevent reflected XSS
    safe_username = flask.escape(username)
    return f"<p>Hello, {safe_username}!</p>"
