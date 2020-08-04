# SweetHome
从零开始打造智能家居系统的尝试，主要的实现有

- 可以扫码添加设备并控制设备的Android App
- 可以控制亮灭的灯，并可以设置在Android App连接到指定Wifi后自动亮起
- 可以控制开关的窗帘，机械结构部分自己设计并3D打印出来
- 树莓派主控，实现Http请求接口和ZigBee模块的通信以及这2种协议的桥接

## 效果演示

[视频已上传到B站](https://www.bilibili.com/video/BV1bp4y1q7fd)

### 硬件及功能

#### 主控
使用RaspBerry Pi4运行Fast API后台，处理Android App发出的Http请求，并将请求转换为对应的指令通过ZigBee模块发送到家居终端，同时使用多线程监听接收家居终端发送回来的ZigBee回令，通过指令顺序标识

#### 家居终端
使用STM32F103C8T6最小系统板，搭配ZigBee串口透传模块，结合多种传感器和电机实现相应功能，开发方式为HAL

#### ZigBee
使用的ZigBee模块是基于CC2530的串口透传模块，使用ZigBee而非WiFi是由于WiFi不完全适用于大规模的智能家居场景中