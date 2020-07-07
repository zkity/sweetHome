#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from enum import Enum

class cmdd(str, Enum):
    add_new_cmd = 'a'

    light_control_cmd = 'b'
    light_control_on = "ba}"
    light_control_off = "bb}"

    light_status_cmd = 'c'
    light_status = "c}}"

    light_ins_cmd = 'd'
    light_ins_a = "da}"
    light_ins_b = "db}"
    light_ins_c = "dc}"

    light_scenes_cmd = 'e'
    light_scenes_door_on = "eaa"
    light_scenes_door_off = "eab"
    light_scenes_voice_on = "eba"
    light_scenes_voice_off = "ebb"
    light_scenes_body_on = "eca"
    light_scenes_body_off = "ecb"
    light_scenes_out_on = "eda"
    light_scenes_out_off = "edb"
    light_scenes_time_on = "eea"
    light_scenes_time_off = "eeb"
    light_scenes_auto_on = "efa"
    light_scenes_auto_off = "efb"
    
    light_scenes_status = "ezc"


    curtain_control_cmd = 'o'
    curtain_control_on = "oa}"
    curtain_control_on_half = "oc}"
    curtain_control_on_zero = "oe}"

    curtain_status_cmd = 'p'
    curtain_status = "p}}"

    curtain_scenes_cmd = 'q'
    curtain_scenes_door_on = "qaa"
    curtain_scenes_door_off = "qab"
    curtain_scenes_time_on = "qba"
    curtain_scenes_time_off = "qbb"
    curtain_scenes_auto_on = "qca"
    curtain_scenes_auto_off = "qcb"

    curtain_scenes_status = "qzc"

