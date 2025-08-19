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

def is_valid_username(username):
    # only alphanumeric and underscores allowed, with minimum length 1 character.
    if not username:
        return False
    pattern = r'^[a-zA-Z0-9_]+$'
    import re
    return bool(re.match(pattern, username))

@app.route("/hello/<username>")
def hello_endpoint(username):
    """Function which says hello to the user"""
    if is_valid_username(username):
        return "Hello, {}!".format(flask.escape(username))
    else:
        flask.abort(400)