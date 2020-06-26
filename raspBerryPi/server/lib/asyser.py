#!/usr/bin/env python3
# -*- coding: utf-8 -*-
'''
由于zigbee设备发送和接收的数据是串行的方式
而用于用户交互的通信采用http是异步的方式
用户发送的指令需要通过http传到主控，再通过zigbee方式发送到终端设备
这个库的作用是将串行的串口数据与异步的http请求进行匹配
'''

import time
import serial
import threading
from threading import Lock

def asy(func):
    ''' 这是个装饰器函数用于开启一个新的线程
        @Param: func 用于装饰的函数
    '''
    def wrapper(*args, **kwargs):
        t = threading.Thread(target=func, args=args, kwargs=kwargs)
        t.start()
    return wrapper

class asyser():
    ''' 这个类是用于匹配2中通信方式的主类
    '''
    def __init__(self):
        # 接收标志数组的同步锁
        self.lock_flag = Lock()
        # 发送指令的顺序号的同步锁
        self.lock_idx = Lock()
        # 串口设备的同步锁
        self.lock_ser = Lock()

        # 指令顺序
        self.__cmd_idx = 47

        # 指令顺序的范围
        self.__cmd_idx_init = 48
        self.__cmd_idx_end = 126
        # 定义常规指令范围
        self.__cmd_normal = 122
        # 定义特殊指令值，该值为接收到人体传感器的主动报告
        self.__cmd_sp_warn = 125

        # 用于构造接收标志数组
        self.__idx = range(self.__cmd_idx_init, self.__cmd_idx_end+1)
        self.__flag = [['F', '0']] * len(self.__idx)
        self.flag_dict = dict(zip(self.__idx, self.__flag))

        # 开启串口设备
        self.__ser = ''
        try:
            self.__ser = serial.Serial('/dev/ttyAMA0', 115200)
            if not self.__ser.isOpen:
                self.__ser.open()
        except Exception as e:
            self.__ser.close()
            print(e)

    @asy
    def send_serial(self, data, idx):
        ''' 用于从串口发送数据的函数，每次调用打开一个新的线程
            在发送数据之前需要申请指令顺序
            @Param: data - 发送的数据
                    idx - 指令顺序
        '''
        self.lock_ser.acquire()
        self.__ser.send_data(data.encode())
        # TODO: 根据实践增加适当的延时
        self.lock_ser.release()

        self.lock_flag.acquire()
        self.flag_dict[idx][0] = 'F'
        self.lock_flag.release()

    @asy
    def watch_serial(self):
        ''' 用于监控串口接收数据，该函数只应该调用一次，打开一个新的线程
            接收到一条新的回令时，按照该回令的指令顺序调整标志数组
        '''
        while True:
            time.sleep(0.001)
            count = self.__ser.inWaiting()
            if count > 0:
                self.lock_ser.acquire()
                recv = self.__ser.read(count)
                self.__ser.flushInput()
                self.lock_ser.release()
                # 在这个时间段内接收到多条回令则分开处理
                recv_list = recv.split('|')
                for recv_item in recv_list:
                    recv_idx = ord(recv_item[1])
                    if recv_idx <= self.__cmd_normal:
                        self.lock_flag.acquire()
                        self.flag_dict[recv_idx][0] = 'T'
                        self.flag_dict[recv_idx][1] = recv_item
                        self.lock_flag.release()
                    elif recv_idx == self.__cmd_sp_warn:
                        # TODO: 处理被动接收到的消息
                        pass
                    else:
                        # TODO: 异常信息记录到日志文件中
                        continue

    def requie_idx(self):
        ''' 申请指令顺序
            @Return: 申请到的指令顺序
        '''
        res = 0
        self.lock_idx.acquire()
        if self.__cmd_idx < self.__cmd_normal:
            self.__cmd_idx += 1
        else:
            self.__cmd_idx == self.__cmd_idx_init
        res = self.__cmd_idx
        self.lock_idx.release()
        return res
