package com.devops.stratusvault.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {
    /**
     * A secured endpoint to test our authentication. If the user's token is valid,
     * Spring Security will inject a Principal object representing the user.
     * We can then get their UID from it.
     */

    @GetMapping("/me")
    public Map<String, String> getMyInfo(Principal principal){
        Map<String, String> userInfo = new HashMap<>();
        // principal.getName() returns the UID we set in the FirebaseTokenFilter
        userInfo.put("uid", principal.getName());
        return userInfo;
    }
}
