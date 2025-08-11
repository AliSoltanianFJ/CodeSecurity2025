from flask import Flask, request, redirect, session
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        query = "SELECT id, password_hash FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if not result:
            return redirect('/?error=Invalid credentials')
        
        stored_password_hash = result[1]
        input_password_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if stored_password_hash == input_password_hash:
            session['username'] = username
            return redirect('/')
        else:
            return redirect('/?error=Invalid password')
    except Exception as e:
        print(f"Database error: {e}")
        return redirect('/?error=Database error')
    finally:
        cursor.close()
        db.close()