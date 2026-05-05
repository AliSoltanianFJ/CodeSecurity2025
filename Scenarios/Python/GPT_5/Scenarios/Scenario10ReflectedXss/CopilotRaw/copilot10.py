import flask
from flask import Flask, render_template, escape
import requests
import sqlite3
import subprocess

app = Flask(__name__)


@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # escape the username to prevent reflected XSS
    safe_name = escape(username)
    return f"<p>Hello, {safe_name}!</p>"

