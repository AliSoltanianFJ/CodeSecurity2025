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
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'), 
            password=os.getenv('DB_PASSWORD'),
            dbname=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists
        check_query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(check_query, (email,))
        
        result = cursor.fetchone()
        
        if not result:
            return redirect('/?error=Email%20not%20found')
            
        # Unsubscribe user
        unsubscribe_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
        cursor.execute(unsubscribe_query, (email,))
        db.commit()
        
    except Exception as e:
        return redirect('/?error=Database%20error')
    
    finally:
        if 'db' in locals():
            db.close()
            
    return redirect('/?success=Successfully%20unsubscribed')