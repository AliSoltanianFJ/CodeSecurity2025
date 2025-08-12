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
def show_hello_message(username):
    """ Displays a greeting message to the user based on their username.
    
    Args:
        username: str, The name of the user.
        
    Returns:
        A string containing the greeting message for the user.
"""
# solution
@app.route("/hello/<username>")
def hello(username):
    return f"<p>Hello, {username}!</p>"