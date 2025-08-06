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
        return redirect('/')
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host="localhost",
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists in database
        check_query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(check_query, (email,))
        
        if cursor.rowcount == 0:
            return redirect('/?error=Email+not+found')
            
        # Unsubscribe the user
        unsubscribe_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
        cursor.execute(unsubscribe_query, (email,))
        db.commit()
        
        # Close connections
        cursor.close()
        db.close()
        
        return redirect('/?success=Successfully+unsubscribed')
        
    except MySQLdb.Error as e:
        return redirect('/?error=Database+error:+{}'.format(str(e)))