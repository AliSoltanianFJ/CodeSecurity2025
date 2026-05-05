from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email parameter from GET request
    email = request.args.get('email')
    
    if not email:
        return "Error: No email provided."
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(host="localhost",
                             user="root",
                             passwd="password",
                             db="mydatabase")
        
        cursor = db.cursor()
        
        # Check if the email exists in subscribers table
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if not result:
            return "Email not found in our records."
        
        # Unsubscribe by deleting the record
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        db.commit()
        
        return redirect(f"/unsubscribe_confirmation?status=success&email={email}")
    
    except MySQLdb.Error as e:
        return f"Error unsubscribing: {e}"
    finally:
        # Close database connection
        if 'db' in locals():
            db.close()

