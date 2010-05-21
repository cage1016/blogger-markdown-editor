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

    //Thanks to http://wp.uberdose.com/2007/02/04/how-to-label-posts-via-the-blogger-api/
    private static void addLabel(Entry entry, String label) {
        Category category = new Category();
        category.setScheme("http://www.blogger.com/atom/ns#");
        category.setTerm(label);
        entry.getCategories().add(category);
    }

    public static void publish() {
        if (session.contains("auth-token")) {
            String title = params.get("title");
            String content = params.get("content");
            String labels = params.get("labels");

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
                java.util.StringTokenizer st = new java.util.StringTokenizer(labels, ",");
                while (st.hasMoreTokens()) {
                    addLabel(entry, st.nextToken());
                }
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

                    java.util.List<Entry> published = new java.util.ArrayList<Entry>();
                    for (Entry entry : entries) {                                                
                        if (!entry.isDraft()) {
                            published.add(entry);
                            entry.getTextContent().getContent().getPlainText();                            
                        }
                    }

                    renderArgs.put("entries", published);
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
                try {
                    URL feedUrl = new URL(defaultUrl);
                    Feed posts = googleService.getFeed(feedUrl, Feed.class);
                    java.util.List<Entry> entries = posts.getEntries();
                    //posts.getEntries(<com.google.gdata.data.blogger.BlogEntry>);
                    //Class<com.google.gdata.data.blogger.BlogEntry> returnClass = Class<com.google.gdata.data.blogger.BlogEntry>();
                    //java.util.List<com.google.gdata.data.blogger.BlogEntry> blogEntries = posts.getEntries(returnClass);

                    java.util.List<Entry> drafts = new java.util.ArrayList<Entry>();
                    System.out.println("Drafts:" + entries.size());
                    for (Entry entry : entries) {
                        System.out.println(entry.isDraft());
                        System.out.println(entry.getTextContent().getContent().getPlainText());
                        System.out.println();
                        if (entry.isDraft()) {
                            drafts.add(entry);
                            System.out.println(entry);
                        }
                    }

                    renderArgs.put("entries", drafts);
                } catch (java.net.MalformedURLException me) {
                } catch (IOException ioe) {
                } catch (ServiceException se) {
                }
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

     public static void edit(final long published, final int tzShift) {
        authenticate("edit", new processAction() {
             public void doAction() {
                try {
                    long start = published - 1;
                    long end = published + 1;
                    DateTime startTime = new DateTime(start, tzShift);
                    DateTime endTime = new DateTime(end, tzShift);                    
                    URL feedUrl = new URL(defaultUrl);
                    Query query = new Query(feedUrl);
                    query.setPublishedMin(startTime);
                    query.setPublishedMax(endTime);
                    Feed resultFeed = googleService.query(query, Feed.class);
                    Entry entry = resultFeed.getEntries().get(0);

                    renderArgs.put("title", entry.getTitle().getPlainText());
                    renderArgs.put("entry", entry.getTextContent().getContent().getPlainText());                    
                    renderArgs.put("start", start);
                    renderArgs.put("end", end);
                    renderArgs.put("tzShift", tzShift);
                } catch (java.net.MalformedURLException me) {
                } catch (IOException ioe) {
                } catch (ServiceException se) {
                }
                renderWithInfo();
             }
        });
    }

    public static void update() {
        if (session.contains("auth-token")) {
            String title = params.get("title");
            String content = params.get("content");
            String sStart = params.get("start");
            String sEnd = params.get("end");
            String sTzShift = params.get("tzShift");

            long start = Long.parseLong(sStart);
            long end = Long.parseLong(sEnd);
            int tzShift = Integer.parseInt(sTzShift);

            try {
                DateTime startTime = new DateTime(start, tzShift);
                DateTime endTime = new DateTime(end, tzShift);
                URL feedUrl = new URL(defaultUrl);
                Query query = new Query(feedUrl);
                query.setPublishedMin(startTime);
                query.setPublishedMax(endTime);
                Feed resultFeed = googleService.query(query, Feed.class);
                Entry entry = resultFeed.getEntries().get(0);
                entry.setTitle(new PlainTextConstruct(title));
                PlainTextConstruct htmlContent = new PlainTextConstruct();
                htmlContent.setText(content);
                entry.setContent(htmlContent);
                entry.setDraft(false);

                URL editUrl = new URL(entry.getEditLink().getHref());
                googleService.update(editUrl, entry);


                JSONSerializer serializer = new JSONSerializer();
                JsonResponse jsonResponse = new JsonResponse();



                jsonResponse.nextUrl = "/posts";
                Admin.renderJSON(serializer.exclude("class", "nextUrl").serialize(jsonResponse));
            } catch (java.net.MalformedURLException me) {
                System.out.println(me.toString());
            } catch (IOException ioe) {
                System.out.println(ioe.toString());
            } catch (ServiceException se) {
                System.out.println(se.toString());
            }
        } else {

        }
    }
}
