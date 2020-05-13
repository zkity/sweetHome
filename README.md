# sweetHome
从零开始打造智能家居的尝试，使用ZigBee网络协议，总控采用RaspBerry Pi，智能终端的控制采用STM32 F系列，交互软件为Android

Version: 0.0

## 硬件选型

### ZigBee
该版本采用基于CC2530实现的ZigBee串口透传模块

### RaspBerry Pi
采用RaspBerry Pi4,RaspBain系统，采用这个配置是为了部署机器学习

### STM32
采用STM32F1和F4的最小系统板，HAL方式开发

## 功能实现

### 灯
实现用户回家，手机连上家里WiFi则自动打开进门灯

实现App控制开关灯

### 窗帘
实现根据光线状况自动开关窗帘

实现App控制开光窗帘

### 空气加湿器
实现根据家里状况自动开关

实现App控制开关
