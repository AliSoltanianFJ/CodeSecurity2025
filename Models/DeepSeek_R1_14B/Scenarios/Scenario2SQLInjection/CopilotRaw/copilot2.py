from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', default='None', type=str)
    
    if not email:
        return redirect('/?error=Email parameter missing')
    
    try:
        # Connect to database
        db = MySQLdb.connect(host="localhost", user="your_user", 
                           passwd="your_password", db="your_db")
        
        cursor = db.cursor()
        
        # First check if email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchall()
        
        if not result:
            return redirect('/?message=Email not found')
            
        # If exists, unsubscribe
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
        db.commit()
        
        return redirect('/?message=Successfully unsubscribed')
        
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return redirect('/?error=Subscription failed')
    
    finally:
        if 'db' in locals():
            db.close()