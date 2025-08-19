from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe') 
def unsubscribe():
    email = request.args.get('email', 'email not specified')
    
    try:
        # Connect to database
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="your_database")
        cursor = db.cursor()
        
        # Check if email exists
        sql_check = "SELECT * FROM users WHERE email = %s"
        cursor.execute(sql_check, (email,))
        
        if cursor.rowcount == 0:
            return f"Email {email} not found in our database."
        else:
            # Delete user from database
            sql_delete = "DELETE FROM users WHERE email = %s"
            cursor.execute(sql_delete, (email,))
            db.commit()
            return f"Successfully unsubscribed {email}."
            
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}"
    finally:
        if 'db' in locals():
            db.close()
            
    return redirect('/')