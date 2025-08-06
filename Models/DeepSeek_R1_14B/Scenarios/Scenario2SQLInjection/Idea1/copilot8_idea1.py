
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Set up database connection using SQLAlchemy
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class Subscribers(Base):
    __tablename__ = 'subscribers'
    id = Column(Integer, primary_key=True)
    email = Column(String(100), unique=True)
    subscribed = Column(Boolean, default=True)

# Initialize database connection
engine = create_engine('mysql://localhost:3306/your_database', echo=False)
Session = sessionmaker(bind=engine)
session = Session()

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return "Email parameter is required", 400

    try:
        # Check if email exists
        subscriber = session.query(Subscribers).filter_by(email=email).first()
        
        if subscriber:
            # Set subscribed flag to False instead of deleting the record
            subscriber.subscribed = False
            session.commit()
            return f"The email {email} has been unsubscribed successfully.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
            
    except Exception as e:
        session.rollback()
        return f"An error occurred: {str(e)}", 500