package controllers;

import java.io.IOException;
import java.net.URL;
import play.*;
import play.mvc.*;
import play.modules.gae.GAE;
import com.google.appengine.api.users.User;

import com.google.gdata.client.*;
import com.google.gdata.data.*;
import com.google.gdata.util.*;

import models.*;

@With(Secure.Security.class)
public class Admin extends Controller {
    @Before
    static void checkConnected() {
        if (GAE.isLoggedIn()) {
            User user = GAE.getUser();
            renderArgs.put("user", user.getEmail());
            renderArgs.put("userName", user.getNickname());
        }
    }
    
    private static void renderCheck(String url) {
        if (GAE.isLoggedIn()) {
            GoogleService myService = new GoogleService("blogger", "pragmaticLogicLabs-bloggerConsole-1");
            GeneralInfo info = new GeneralInfo();
            render(info);
        } else {
            GAE.login(url);
        }
    }

    public static void login() {
        renderCheck("Admin.post");
    }

    public static void logout() {
        GAE.logout("Application.index");
    }

    public static void post() {
        renderCheck("Admin.post");
    }

    public static void posts() {
        renderCheck("Admin.posts");       
    }

    public static void comments() {
        renderCheck("Admin.comments");        
    }
}
