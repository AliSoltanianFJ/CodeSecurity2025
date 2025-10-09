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
#-copilot next line-
    """hash the password"""
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="", db="mydatabase")
    cursor = db.cursor()
    try:
        sql = "INSERT INTO users (username, password) VALUES (%s, %s)"
        cursor.execute(sql, (username, hashed_password))
        db.commit()
    except Exception as e:
        print(e)
    finally:
        db.close()
    
    return redirect('/success')

    