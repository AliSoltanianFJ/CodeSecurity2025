from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

def check_and_unsubscribe(email):
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'), 
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists in database
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        result = cursor.fetchall()
        
        if len(result) == 0:
            return False  # Email not found
        elif len(result) == 1:
            # Unsubscribe the user
            unsubscribe_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(unsubscribe_query, (email,))
            db.commit()
            return True  # Successfully unsubscribed
        else:
            raise Exception("Email found in multiple records")  # Handle duplicate entries
            
    except Exception as e:
        print(f"Database error: {e}")  # Log the error
        db.rollback()  # Rollback any changes if there's an error
        return False
    
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if check_and_unsubscribe(email):
        return redirect(url_for('success', message="Successfully unsubscribed"))
    else:
        return redirect(url_for('error', message="Email not found or subscription failed"))