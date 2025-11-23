package com.moraveco.springboot.auth.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Register {
    private String name;
    private String email;
    private String lastname;
    private String password;

}
