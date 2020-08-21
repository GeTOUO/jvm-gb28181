package com.getouo.gb.scl

import com.getouo.gb.scl.service.DeviceService
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
class DeviceController(devices: DeviceService) {

  @RequestMapping(value = Array("/play"))
  def play(id: String): String = devices.play(id)
}
