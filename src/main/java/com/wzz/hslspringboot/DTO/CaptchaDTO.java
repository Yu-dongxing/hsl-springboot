package com.wzz.hslspringboot.DTO;

import lombok.Data;

@Data
public class CaptchaDTO {
    private String type;
    private String backgroundImage;
    private String templateImage;


}
