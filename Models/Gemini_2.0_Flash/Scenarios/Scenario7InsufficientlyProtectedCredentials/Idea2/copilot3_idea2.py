
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

    """Prevent SQL injection"""
    username = db.escape_string(username)

    """execute the SQL query to insert the username and hashed password into the database"""
    try:
        cursor.execute("INSERT into users (username, password) VALUES ('" + username + "', '" + hashed_password + "')")
        db.commit()
    except:
        db.rollback()

    """close the connection"""
    db.close()

    """redirect to the home page"""
    return redirect('/')