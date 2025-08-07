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
    
    # Generate a random salt
    salt = os.urandom(16)
    salt_hex = salt.hex()
    
    password_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="test")
        cursor = db.cursor()
        
        # Prepare SQL statement
        sql = "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"
        cursor.execute(sql, (username, password_hash, salt_hex))
        
        # Commit the transaction
        db.commit()
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return redirect('/login?error=Registration failed')