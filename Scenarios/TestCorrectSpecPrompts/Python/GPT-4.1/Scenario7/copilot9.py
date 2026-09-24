from flask import Flask, request, redirect
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os

app = Flask(__name__)
    
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    password_hash = generate_password_hash(password)   # <-- secure password hashing

    # Assuming you have a MySQLdb connection set up:
    db = MySQLdb.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        user=os.environ.get("DB_USER", "user"),
        passwd=os.environ.get("DB_PASS", "password"),
        db=os.environ.get("DB_NAME", "database")
    )
    cursor = db.cursor()

    # Very simple insert, adjust for your structure and add checks for existing users, etc.
    cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, password_hash))
    db.commit()
    cursor.close()
    db.close()

    return redirect('/login')  # redirect or render as appropriate