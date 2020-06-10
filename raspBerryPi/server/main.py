from fastapi import FastAPI
import RPi.GPIO as GPIO
import serial
import time

# create a fastapi instance
app = FastAPI()

GPIO.setmode(GPIO.BCM)

GPIO.setup(17, GPIO.OUT)
GPIO.setup(27, GPIO.OUT)
GPIO.output(17, GPIO.HIGH)
GPIO.output(27, GPIO.HIGH)

ser = ''
try:
	ser = serial.Serial('/dev/ttyAMA0', 115200)
	if not ser.isOpen:
		ser.open()
except Exception as e:
	ser.close()
	print(e)


def sendx(cmd):
	ser.write(cmd.encode())


''' http operations
post get put delete options head patch trace

post: to create data
get: to read data
put: to update data
delete: to delete data
'''
@app.get("/a")
async def root():
	sendx('aaaaa')
	return {"message": "Hello World"}

@app.get("/b")
async def root():
	sendx('abbbb')
	return {"message": "Hello World"}


@app.get("/c")
async def root():
	sendx('acccc')
	return {"message": "Hello World"}

@app.get("/d")
async def root():
	sendx('adddd')
	return {"message": "Hello World"}
