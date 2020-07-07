from fastapi import FastAPI
import time
from lib.asyser import asyser
from lib.cmdd import cmdd

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

print('init main')


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

# ----------------一个典型的api-------------------
@callback
def handle_light_control(*arg, **kwargs):
    ''' 控制灯亮灭的回调函数
    '''
    data = arg[0]
    return data

@app.get("/light/control/{did}/{act}")
def light_control(did: str, act: str):
    ''' 控制灯的亮灭
        @Param: did-str-设备的id
        @Param: act-str-on/off

        @Retrun: {'r': 's'/'f'/'m'} 成功/失败/超时
    '''
    cmd_suf = ''
    if act == 'on':
        cmd_suf = cmdd.light_control_on
    elif act == 'off':
        cmd_suf = cmdd.light_control_off
    else:
        # TODO: 记录异常值到日志
        pass
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_light_control(idx)
        
        if res[2] == 'T':
            return {'r': 's'}
        elif res[2] == 'F':
            return {'r': 'f'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            # TODO: 记录异常值到日志
            pass
# -------------------------------------------------
@callback
def handle_light_status(*arg, **kwargs):
    ''' 获取灯的状态的回调函数
    '''
    data = arg[0]
    return data

@app.get("/light/status/{did}")
def light_status(did: str):
    ''' 获取灯现在的状态
        @Param: did-str-设备的id

        @Retrun: {'r': 'on'/'off'/'m'} 亮 灭 超时
    '''
    cmd_suf = cmdd.light_status
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_light_status(idx)
        
        if res[2] == 'a':
            return {'r': 'on'}
        elif res[2] == 'b':
            return {'r': 'off'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            # TODO: 记录异常值到日志
            pass

@callback
def handle_light_ins(*arg, **kwargs):
    ''' 控制指示灯光的回调函数
    '''
    data = arg[0]
    return data

@app.get("/light/ins/{did}/{act}")
def light_status(did: str, act: str):
    ''' 控制指示灯光
        @Param: did-str-设备的id
        @Param: act-str-指示灯的颜色

        @Retrun: {'r': 's'/'f'/'m'} 成功 失败 超时
    '''
    cmd_suf = ''
    if act == 'a':
        cmd_suf = cmdd.light_ins_a
    elif act == 'b':
        cmd_suf = cmdd.light_ins_b
    elif act == 'c':
        cmd_suf = cmdd.light_ins_c
    else:
        # TODO: 记录异常值到日志
        pass
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_light_ins(idx)
        
        if res == 'T':
            return {'r', 's'}
        elif res == 'F':
            return {'r', 'f'}
        elif res == 'miss':
            return {'r': 'm'}
        else:
            #TODO: 记录异常值到日志
            pass


@callback
def handle_pair(*arg, **kwargs):
    ''' 添加一个新设备的回调函数
    '''
    data = arg[0]
    return data

@app.get("/pair/{did}/{pin}/{dt}")
def pair(did: str, pin: str, dt: str):
    ''' 添加一个新设备
        @Param: did-str-设备的id
        @Param: pin-str-设备的配对码
        @Param: dt-str-设备的类型

        @Retrun: {'r': 'on'/'off'/'m'} 亮 灭 超时
    '''
    cmd_suf = cmdd.add_new_cmd
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf + pin + dt
        asyser.send_serial(cmd, idx)
        res = handle_light_status(idx)
        
        if res[2] == 'T':
            return {'r': 's'}
        elif res[2] == 'F':
            return {'r': 'f'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            # TODO: 记录异常值到日志
            pass


@callback
def handle_curtain_control(*arg, **kwargs):
    ''' 控制窗帘开关的回调函数
    '''
    data = arg[0]
    return data

@app.get("/curtain/control/{did}/{due}")
def curtain_control(did: str, due: str):
    ''' 控制窗帘的开关
        @Param: did-str-设备的id
        @Param: 开合程度 a, c, e - 1, 0.5, 0

        @Retrun: {'r': 's'/'f'/'m'} 成功/失败/超时
    '''
    cmd_suf = ''
    if due == 'a':
        cmd_suf = cmdd.curtain_control_on
    elif due == 'c':
        cmd_suf = cmdd.curtain_control_on_half
    elif due == 'e':
        cmd_suf = cmdd.curtain_control_on_zero
    else:
        # TODO: 记录异常值到日志
        pass
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_curtain_control(idx)
        
        if res[2] == 'T':
            return {'r': 's'}
        elif res[2] == 'F':
            return {'r': 'f'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            # TODO: 记录异常值到日志
            pass


@callback
def handle_curtain_status(*arg, **kwargs):
    ''' 获取窗帘状态的回调函数
    '''
    data = arg[0]
    return data

@app.get("/curtain/status/{did}")
def curtain_status(did: str):
    ''' 获取窗帘现在的状态
        @Param: did-str-设备的id

        @Retrun: {'r': 'a'/'c'/'e'/'m'} 全开/开一半/关/超时
    '''
    cmd_suf = ''
    cmd_suf = cmdd.curtain_status
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_light_control(idx)
        
        if res[2] == 'a':
            return {'r': 'a'}
        elif res[2] == 'c':
            return {'r': 'c'}
        elif res[2] == 'e':
            return {'r': 'e'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            # TODO: 记录异常值到日志
            pass


@callback
def handle_curtain_scn(*arg, **kwargs):
    ''' 控制窗帘场景的回调函数
    '''
    data = arg[0]
    return data

@app.get("/curtain/scn/{did}/{act}/{due}")
def curtain_scn(did: str, act: str, sta: str):
    ''' 控制窗帘的场景
        @Param: did-str-设备的id
        @Param: act-str-场景
        @Param: due-str-状态

        @Retrun: {'r': 'a'/'c'/'e'/'m'} 全开/开一半/关/超时
    '''
    cmd_suf = ''
    if act == 'z' and sta == 'c':
        cmd_suf = cmdd.curtain_scenes_status
    elif act == 'a':
        if sta == 'a':
            cmd_suf = cmdd.curtain_scenes_door_on
        elif sta == 'b':
            cmd_suf = cmdd.curtain_scenes_door_off
    elif act == 'b':
        if sta == 'a':
            cmd_suf = cmdd.curtain_scenes_time_on
        elif sta == 'b':
            cmd_suf = cmdd.curtain_scenes_time_off
    elif act == 'c':
        if sta == 'a':
            cmd_suf = cmdd.curtain_scenes_auto_on
        elif sta == 'b':
            cmd_suf = cmdd.curtain_scenes_auto_off
    else:
        pass
        # TODO: 处理异常值
            
    if len(did) != 1:
        return {'r': 'f'}
    else:
        idx = asyser.require_idx()
        cmd = did + chr(idx) + cmd_suf
        asyser.send_serial(cmd, idx)
        res = handle_light_control(idx)
        
        if act == 'z' and sta == 'c':
            return {'r': ','.join(res[2: 5])}
        if res[2] == 'T':
            return {'r': 's'}
        elif res[2] == 'F':
            return {'r': 'f'}
        elif res == "miss":
            return {'r': 'm'}
        else:
            pass
            # TODO: 记录异常值到日志
