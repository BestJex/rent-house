package com.harry.renthouse.web.dto;

import lombok.Data;

/**
 * 地铁站dto
 * @author Harry Xu
 * @date 2020/5/9 13:55
 */
@Data
public class SubwayStationDTO {

    private Long id;

    private Long subwayId;

    private String name;

}
