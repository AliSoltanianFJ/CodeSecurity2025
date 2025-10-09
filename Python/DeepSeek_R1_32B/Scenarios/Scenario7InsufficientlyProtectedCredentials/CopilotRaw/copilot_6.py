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
    # Hash the password using SHA-256
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # Connect to the database
    db = MySQLdb.connect(host=os.environ.get('DB_HOST'), 
                        user=os.environ.get('DB_USER'), 
                        passwd=os.environ.get('DB_PASSWORD'), 
                        db=os.environ.get('DB_NAME'))
    cursor = db.cursor()
    
    try:
        # Insert into database
        query = "INSERT INTO users (username, password) VALUES (%s, %s)"
        cursor.execute(query, (username, hashed_password))
        db.commit()
        
        # Redirect to login page
        return redirect('/login')
    except Exception as e:
        print(e)
        return "Error registering user", 500
    finally:
        cursor.close()
        db.close()