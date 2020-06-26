# -*- coding: utf-8 -*-
import RPi.GPIO as GPIO
import serial
import time


''' select the GPIO numbers

BCM   : GPIO numbers
BOARD : physical numbers
'''
GPIO.setmode(GPIO.BCM)

# RST, low bit to reset the zigbee model
GPIO.setup(17, GPIO.OUT)
# Communication Mode Select, higt reprsent the ont-to-many mode
GPIO.setup(27, GPIO.OUT)

GPIO.output(17, GPIO.HIGH)
GPIO.output(27, GPIO.HIGH)

# serial device init
ser = ''
try:
	# ser = serial.Serial('/dev/ttyAMA0', 9600)
	ser = serial.Serial('/dev/ttyAMA0', 115200)
	if not ser.isOpen:
		ser.open()
except Exception as e:
	ser.close()
	print(e)


def cmdx(serial, command, timeout=2, retry=2):
	''' send command to zigbee router and wait for response

	serial  : serial device
	command : command line
	timeout : wait time

	return  : the response list from zigbee router
	'''
	ret = []
	ind = 0
	# if time out and can't receive data, the retry
	while ind < retry:
		ind += 1
		serial.write(command)
		n = 0
		# wait for a while and record all the response
		while n < timeout*1:
			time.sleep(0.01)
			n += 1
			count = serial.inWaiting()
			if count > 0:
				recv = ser.read(count)
				ser.flushInput()
				ret.append(recv)
		if len(ret) > 0:
			break
	return ret


def main():
	try:
		while True:
			a = cmdx(ser, 'acx'.encode())
			time.sleep(5)
			print('a')
			print(a)
			b = cmdx(ser, b'bdn')
			time.sleep(5)
			print('b')
			print(b)
	except Exception as e:
		ser.close()
		GPIO.cleanup()
		print(e)


if __name__ == "__main__":
	main()
