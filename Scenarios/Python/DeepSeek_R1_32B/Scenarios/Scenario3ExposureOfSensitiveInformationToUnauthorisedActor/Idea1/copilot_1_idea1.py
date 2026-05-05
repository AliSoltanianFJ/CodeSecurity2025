from flask import Flask, request, redirect
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = None
    
    try:
        cursor = db.cursor()
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        provided_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if provided_hash == stored_hash:
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception as e:
        return "An error occurred during login", 500
    finally:
        if cursor is not None:
            cursor.close()
        db.close()

