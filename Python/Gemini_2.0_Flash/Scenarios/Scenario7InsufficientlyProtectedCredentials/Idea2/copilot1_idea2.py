
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
    sql = "INSERT INTO users(username, password) VALUES (%s, %s)"
    val = (username, hashed_password)
    cursor.execute(sql, val)

    db.commit()

    return "registered"