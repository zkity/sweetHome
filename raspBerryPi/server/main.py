from fastapi import FastAPI
import time
from lib.asyser import asyser

''' http operations
post get put delete options head patch trace

post: to create data
get: to read data
put: to update data
delete: to delete data
'''

'''
该文件实现对后台所有api的处理
'''

# 创建FastAPI的实例
app = FastAPI()

# 创建串口的实例
asyser = asyser()

# 监控串口接收数据
asyser.watch_serial()


def callback(func):
    ''' 发送指令后的回调函数
        最多等待5s，收到回令则调用该指令的对应回调函数
        若没有收到对应的回令则返回提示信息

        @Param: idx - 等待的指令顺序
        @Return: 返回回调函数处理后的结果或未接收到回令的提示
    '''
    def wrapper(*arg, **kwargs):
        # 等待的指令顺序
        idx = arg[0]
        global asyser
        wait_idx = 0
        res = 'miss'
        while True:
            if wait_idx >= 500:
                break
            # 收到回令
            asyser.lock_flag.acquire()
            if asyser.flag_dict[idx][0] == 'T':
                asyser.flag_dict[idx][0] == 'F'
                data = asyser.flag_dict[idx][1]
                asyser.lock_flag.release()
                res = func(data)
                break
            else:
                asyser.lock_flag.release()
            time.sleep(0.01)
            wait_idx += 1
        # 未收到回令
        return res
    return wrapper

@callback
def handleLight(*arg, **kwargs):
    ''' 与设备灯有关的回调函数
    '''
    data = arg[0]
    # TODO: 处理接收到的数据并返回对应的值
    return {"m": data}

@app.get("/a")
def light():
    ''' 与设备灯有关的函数
    '''
    # TODO: 将路径里的参数做对应的解析
    idx = asyser.require_idx()
    asyser.send_serial("abc", idx)

    res = handleLight(idx)
    return res

# TODO: 其他api的实现
