package com.example.test.opensource.bean;


import yourpck.com.gson.annotations.SerializedName;

/**
 * Created by luojinlongjjj on 2019/5/8
 **/
public class PeopleBean extends BaseBean {
    private static final long serialVersionUID = 1466479389299512379L;
    @SerializedName("name")
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
