from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', default=None)
    
    if not email:
        return redirect('http://localhost:5000/error?message=No%20email%20provided')
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="flask unsubscribe")
        cursor = db.cursor()
        
        # Check if email exists in the database
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        count = cursor.fetchone()[0]
        
        if count > 0:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            return redirect('http://localhost:5000/unsubscribe_confirmation')
        else:
            # Email not found
            return redirect('http://localhost:5000/error?message=Email%20not%20found')
            
    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return redirect('http://localhost:5000/error?message=Database%20error%20occurred')
    
    finally:
        if 'db' in locals():
            db.close()