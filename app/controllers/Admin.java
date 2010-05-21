package controllers;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

import play.mvc.*;
import play.modules.gae.*;
import com.google.appengine.api.users.*;

import com.google.gdata.client.*;
import com.google.gdata.data.*;
import com.google.gdata.util.*;
import com.google.gdata.client.http.AuthSubUtil;

import flexjson.*;

import models.*;

@With(Secure.Security.class)
public class Admin extends Controller {
    interface processAction {
        void doAction();
    }    

    @Before
    static void checkConnected() {
        if (GAE.isLoggedIn()) {
            User user = GAE.getUser();
            renderArgs.put("user", user.getEmail());
            renderArgs.put("userName", user.getNickname());
        }
    }

    private static String BASE_URL_PREFIX = "http://www.blogger.com/feeds/";
    private static String BASE_URL_SUFFFIX = "/posts/default";
    private static String BLOG_ID = "3671514850256229698"; //1755575872729350021
    private static GoogleService googleService = new GoogleService("blogger", "pragmaticLogicLabs-bloggerMarkdownEditor-1");
    private static String defaultUrl = BASE_URL_PREFIX + BLOG_ID + BASE_URL_SUFFFIX;

    private static void renderCheck(String url) {
        if (GAE.isLoggedIn()) {            
            GeneralInfo info = new GeneralInfo();
            render(info);
        } else {
            GAE.login(url); //example: url is something like "Admin.Post"
        }
    }

     private static void renderWithInfo() {
        GeneralInfo info = new GeneralInfo();
        render(info);
    }

    private static String getAuth(String next) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("https://www.google.com/accounts/AuthSubRequest");
            sb.append("?scope=");
            sb.append(java.net.URLEncoder.encode("http://www.blogger.com/feeds/", "UTF-8"));
            sb.append("&session=1");
            sb.append("&secure=0");
            sb.append("&next=");
            sb.append("http://" + request.host + "/" + next);
        } catch (Exception e) {
        }
        return sb.toString();
    }

    private static void authenticate(String next, processAction p) {
        if (session.contains("auth-token")) {
            p.doAction();
        } else {
            if (params._contains("token")) {
                try {
                    //String token = AuthSubUtil.getTokenFromReply( params.get("token"));
                    String token = AuthSubUtil.getTokenFromReply(request.querystring);
                    String sessionToken = AuthSubUtil.exchangeForSessionToken(token, null);
                    session.put("auth-token", sessionToken);
                    googleService.setAuthSubToken(sessionToken);
                    p.doAction();
                } catch (IOException ioe) {
                    System.out.println(ioe.toString());
                } catch (GeneralSecurityException gse) {
                    System.out.println(gse.toString());
                } catch (AuthenticationException ae) {
                    System.out.println(ae.toString());
                }
            } else {
                String auth = getAuth(next);
                redirect(auth);
            }
        }
    }

    public static void login() {
        renderCheck("Admin.post");
    }

    public static void logout() {
        GAE.logout("Application.index");
    }

    public static void post() {        
        authenticate("post", new processAction() {
             public void doAction() {
                renderWithInfo();
             }
        });
    }

    public static void publish() {
        if (session.contains("auth-token")) {
            String title = params.get("title");
            String content = params.get("content");            

            JSONSerializer serializer = new JSONSerializer();
            JsonResponse jsonResponse = new JsonResponse();

            try {                                
                URL postUrl = new URL(defaultUrl);

                Entry entry = new Entry();
                entry.setTitle(new PlainTextConstruct(title));
                PlainTextConstruct htmlContent = new PlainTextConstruct();
                htmlContent.setText(content);
                entry.setContent(htmlContent);
                entry.setDraft(false);

                googleService.insert(postUrl, entry);

                jsonResponse.nextUrl = "/posts";
                Admin.renderJSON(serializer.exclude("class", "nextUrl").serialize(jsonResponse));
            } catch (java.net.MalformedURLException me) {
                System.out.println(me.toString());
            } catch (IOException ioe) {
                System.out.println(ioe.toString());
            } catch (AuthenticationException ae) {
                session.remove("auth-token");
                authenticate("/post", new processAction() {
                     public void doAction() {                      
                     }
                });
            } catch (ServiceException se) {
                System.out.println(se.toString());
                post();
            }
        } else {
            
        }
    }

    public static void saveDraft() {
         posts();
    }

    public static void posts() {        
        authenticate("posts", new processAction() {
             public void doAction() {                                                
                try {                    
                    URL feedUrl = new URL(defaultUrl);
                    Feed posts = googleService.getFeed(feedUrl, Feed.class);
                    java.util.List<Entry> entries = posts.getEntries();
                    // posts.
                    //java.util.List<PlainTextConstruct> ptc = posts.getEntries(<PlainTextConstruct>);
                    renderArgs.put("entries", entries);
                    for (Entry entry : entries) {
                        //PlainTextConstruct ptc = (PlainTextConstruct) entry.getPl;
                        //PlainTextConstruct text = entry.getPlainTextContent();
                        System.out.println(entry.getTextContent().getContent().getPlainText());
                        //entry.getPublished().toStringRfc822().substring(beginIndex, endIndex)
                    }
                } catch (java.net.MalformedURLException me) {                                        
                } catch (IOException ioe) {                                  
                } catch (ServiceException se) {                    
                }
                renderWithInfo();
             }
        });
    }

    public static void drafts() {
        authenticate("drafts", new processAction() {
             public void doAction() {
                renderWithInfo();
             }
        });
    }

    public static void settings() {
        authenticate("settings", new processAction() {
             public void doAction() {
                renderWithInfo();
             }
        });
    }

     public static void edit() {
        authenticate("edit", new processAction() {
             public void doAction() {
                renderWithInfo();
             }
        });
    }
}
