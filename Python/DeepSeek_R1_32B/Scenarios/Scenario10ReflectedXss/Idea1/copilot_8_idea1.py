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
from flask import Flask, escape

@app.route("/hello/<username>")
def say_hello(username):
    escaped_username = escape(username)
    return f"<p>Hello, {escaped_username}!</p>"

