
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query parameters
    email = request.args.get('email', '').strip()
    
    # Validate input
    if not email:
        return "Email parameter is required", 400
        
    # Validate email format (basic check)
    if not email.strip('@').split('@')[1]:
        return "Invalid email format", 400

    try:
        with MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'), 
            password=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        ) as connection:
            cursor = connection.cursor()
            
            # Use parameterized query to prevent SQL injection
            cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
            
            if cursor.rowcount > 0:
                cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
                return redirect("/unsubscribe?success=1", code=302)
                
        return "Email not found or already unsubscribed", 200
        
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return "An error occurred while processing your request", 500