from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email from the GET request parameters
    email = request.args.get('email')
    
    if not email:
        return "Error: No email provided."
    
    try:
        # Connect to the MySQL database
        db = MySQLdb.connect(host="localhost", user="username", 
                            passwd="password", db="database_name")
        cursor = db.cursor()
        
        # Check if the email exists in the database
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result[0] > 0:
            # Unsubscribe by deleting the email from the database
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            return redirect("/unsubscribe_success")
        else:
            # Email not found in the database
            return redirect("/unsubscribe_error")
        
    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return "An error occurred while processing your request."
    
    finally:
        # Close the cursor and database connection if they exist
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()