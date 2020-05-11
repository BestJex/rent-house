package com.harry.renthouse.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Harry Xu
 * @date 2020/5/8 10:31
 */
@AllArgsConstructor
@Getter
public enum ApiResponseEnum {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求失败"),
    NOT_FOUND(404, "请求地址不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    NOT_LOGIN(40001, "用户未登录"),
    USERNAME_PASSWORD_ERROR(40002, "用户名密码错误"),
    NO_PRIORITY_ERROR(40003, "无权访问"),
    NOT_VALID_PARAM(40005, "无效的参数"),
    NOT_SUPPORTED_OPERATION(40006, "不支持的操作"),
    NO_AUTHENTICATED_USER_ERROR(40007, "获取认证用户失败"),
    NOT_VALID_CREDENTIAL(40008, "无效的凭据"),
    FILE_UPLOAD_ERROR(50001, "文件上传失败"),
    SUPPORT_ADDRESS_ERROR(50002, "地址选择有误"),
    SUBWAY_LINE_ERROR(50003, "地铁线路有误"),
    SUBWAY_STATION_ERROR(50004, "地铁站有误"),
    SUBWAY_AND_STATION_MATCH_ERROR(50005, "地铁线路与地铁站匹配错误")
    ;

    private Integer code;

    private String message;

}
