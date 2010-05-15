package controllers;

import play.*;
import play.mvc.*;
import play.modules.gae.GAE;
import com.google.appengine.api.users.User;

import models.*;

public class Application extends Controller {
    @Before
    static void checkConnected() {
        if (GAE.isLoggedIn()) {
            User user = GAE.getUser();
            renderArgs.put("user", user.getEmail());
            renderArgs.put("userName", user.getNickname());
        }
    }
    
    public static void index() {
        GeneralInfo info = new GeneralInfo();
        render(info);
    }    

    /**
     * This method is used to complete the loop for
     * URL forwarding.  The GAE.login(url) method
     * would not take a plain url or an action with parameters.
     */
    public static void redirect() {
        String url = null;
        if (session.contains("forwardURL")) {
            url = session.get("forwardURL");
            // clear the variable from the session
            session.remove("forwardURL");
        } else {
            url = "/";
        }
        redirect(url);
    }
}