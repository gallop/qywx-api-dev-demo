package com.gallop.wechat.base;

import java.io.Serializable;

/**
 * author gallop
 * date 2021-06-18 14:31
 * Description: 自定义响应数据结构
 */
public class BaseResult<T> implements Serializable {
    private static final long serialVersionUID = 6169330890952202539L;

    /**
     * 请求是否成功
     */
    private Boolean success;

    // 响应业务状态
    private Integer code;
    // 响应消息
    private String message;
    // 响应中的数据
    private T data;


    public BaseResult() {
    }

    public BaseResult(Boolean success,Integer code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResult(Integer code, String message, T data) {
        //this.success = true;
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public BaseResult(String msg, T data) {
        //this.success = true;
        this.code = ResponseCodeEnum.SUCCESS.getCode();
        this.message = msg;
        this.data = data;
    }

    public BaseResult(T data) {
        //this.success = true;
        this.code = ResponseCodeEnum.SUCCESS.getCode();
        this.message = "OK";
        this.data = data;
    }

    //此注释表示此属性不参数系列化，是临时的
    //@Transient
    public boolean getSuccess() {
        return ResponseCodeEnum.SUCCESS.getCode() == code;
    }

    public void setSuccess(boolean success) { this.success = success; }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMsg(String msg) {
        this.message = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
