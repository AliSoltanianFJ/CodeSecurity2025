from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe') 
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return redirect('/?error=Email%20not%20provided')
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="")
        cursor = db.cursor()
        
        # Check if email exists in database
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Unsubscribe the user
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return redirect('/?success=Successfully%20unsubscribed')
        else:
            return redirect('/?error=User%20not%20found')
        
    except MySQLdb.Error as e:
        return redirect('/?error=Database%20error')
    finally:
        if 'cursor' in locals():
            cursor.close()
        db.close()