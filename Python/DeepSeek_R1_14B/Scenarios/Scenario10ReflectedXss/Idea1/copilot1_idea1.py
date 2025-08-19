import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
from flask import Flask, render_template, escape

app = Flask(__name__)

@app.route("/hello/<username>")
def say_hello(username):
    return f"<p>Hello, {escape(username)}!</p>"