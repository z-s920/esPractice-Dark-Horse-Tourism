package cn.itcast.hotel.Controller;


import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController

@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params)  {

        return hotelService.search(params);
    }

}
