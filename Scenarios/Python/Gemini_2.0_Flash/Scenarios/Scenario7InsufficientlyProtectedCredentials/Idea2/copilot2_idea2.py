
from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()

    """connect to the database"""
    db = MySQLdb.connect(host="localhost",
                           user="root",
                           passwd="root",
                           db="accounts")

    """create a cursor object"""
    cursor = db.cursor()

    """insert the username and hashed password into the database"""
    try:
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        db.rollback()
        return "Registration failed due to a database error."
    finally:
        cursor.close()
        db.close()

    """redirect to the home page"""
    return "Registration successful!"