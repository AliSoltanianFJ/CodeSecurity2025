from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return redirect('/error?message=Email%20parameter%20is%20missing.')
    
    try:
        # Establish database connection using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'your_database')
        )
        
        cursor = db.cursor()
        
        # Check if the email exists in the database
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Delete the email from the database
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
        
        # Close the database connection
        db.close()
        
        return redirect('/unsubscribed.html')
    
    except MySQLdb.Error as e:
        return f"An error occurred: {e}"

