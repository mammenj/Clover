package org.floens.chan.net;

import java.io.IOException;
import java.util.ArrayList;

import org.floens.chan.entity.Loadable;
import org.floens.chan.entity.Post;

import android.util.JsonReader;

import com.android.volley.ParseError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.extra.JsonReaderRequest;

public class ChanReaderRequest extends JsonReaderRequest<ArrayList<Post>> {
    private Loadable loadable;
    
    private ChanReaderRequest(String url, Listener<ArrayList<Post>> listener, ErrorListener errorListener) {
        super(url, listener, errorListener);
    }
    
    /**
     * Creates a ChanReaderRequest with supplied params
     * @param mode ThreadManager mode
     * @param board board key
     * @param no page for board, no for threads
     * @param listener
     * @param errorListener
     * @return New instance of ChanReaderRequest
     */
    public static ChanReaderRequest newInstance(Loadable loadable, Listener<ArrayList<Post>> listener, ErrorListener errorListener) {
        String url;
        
        if (loadable.isBoardMode()) {
            url = ChanUrls.getPageUrl(loadable.board, loadable.no);
        } else if (loadable.isThreadMode()) {
            url = ChanUrls.getThreadUrl(loadable.board, loadable.no);
        } else if (loadable.isCatalogMode()) {
        	url = ChanUrls.getCatalogUrl(loadable.board);
        } else {
            throw new IllegalArgumentException("Unknown mode");
        }
        
        ChanReaderRequest request = new ChanReaderRequest(url, listener, errorListener);
        request.loadable = loadable;
        
        return request;
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }

    @Override
    public ArrayList<Post> readJson(JsonReader reader) {
        if (loadable.isBoardMode()) {
            return loadBoard(reader);
        } else if (loadable.isThreadMode()) {
        	return loadThread(reader);
        } else if (loadable.isCatalogMode()) {
        	return loadCatalog(reader);
        } else {
            throw new IllegalArgumentException("Unknown mode");
        }
    }
    
    private ArrayList<Post> loadThread(JsonReader reader) {
        ArrayList<Post> list = new ArrayList<Post>();
        
        try {
            reader.beginObject(); 
            // Page object
            while (reader.hasNext()) {
                if (reader.nextName().equals("posts")) {
                    reader.beginArray(); 
                    // Thread array
                    while (reader.hasNext()) {
                        // Thread object
                        list.add(readThreadObject(reader, loadable.board));
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch(IOException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(NumberFormatException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(IllegalStateException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        }
        
        return list;
    }
    
    private ArrayList<Post> loadBoard(JsonReader reader) {
        ArrayList<Post> list = new ArrayList<Post>();
        
        try {
            reader.beginObject(); // Threads array
            
            if (reader.nextName().equals("threads")) {
                reader.beginArray();
                
                while (reader.hasNext()) {
                    reader.beginObject(); // Thread object
                    
                    if (reader.nextName().equals("posts")) {
                        reader.beginArray();
                        
                        list.add(readThreadObject(reader, loadable.board));

                        // Only consume one post
                        while (reader.hasNext()) reader.skipValue();
                        
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                    
                    reader.endObject();
                }
                
                reader.endArray();
            } else {
                reader.skipValue();
            }
            
            reader.endObject();
        } catch(IOException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(NumberFormatException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(IllegalStateException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        }
        
        return list;
    }
    
    private ArrayList<Post> loadCatalog(JsonReader reader) {
        ArrayList<Post> list = new ArrayList<Post>();
        
        try {
            reader.beginArray(); // Array of pages
            
            while (reader.hasNext()) {
            	reader.beginObject(); // Page object
            	
            	while (reader.hasNext()) {
	                if (reader.nextName().equals("threads")) {
	                    reader.beginArray(); // Threads array
	                    
	                    while (reader.hasNext()) {
	                    	list.add(readThreadObject(reader, loadable.board));
	                    }
	                    
	                    reader.endArray();
	                } else {
	                    reader.skipValue();
	                }
            	}
            	
                reader.endObject();
            }
            
            reader.endArray();
        } catch(IOException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(NumberFormatException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        } catch(IllegalStateException e) {
            e.printStackTrace();
            setError(new ParseError(e));
        }
        
        return list;
    }
    
    private Post readThreadObject(JsonReader reader, String board) throws IllegalStateException, NumberFormatException, IOException {
        Post post = new Post();
        post.board = board;
        
        reader.beginObject();
        while(reader.hasNext()) {
            String key = reader.nextName();
            
            if (key.equals("no")) {
                // Post number
                post.no = reader.nextInt();
            /*} else if (key.equals("time")) {
                // Time
                long time = reader.nextLong();
                post.date = new Date(time * 1000);*/
            } else if (key.equals("now")) {
                post.date = reader.nextString();
            } else if (key.equals("name")) {
                post.name = reader.nextString();
            } else if (key.equals("com")) {
                post.setComment(reader.nextString());
            } else if (key.equals("tim")) {
                post.tim = reader.nextString();
            } else if (key.equals("time")) {
                post.time = reader.nextLong();
            } else if (key.equals("email")) {
                post.email = reader.nextString();
            } else if (key.equals("ext")) {
                post.ext = reader.nextString().replace(".", "");
            } else if (key.equals("resto")) {
                post.resto = reader.nextInt();
            } else if (key.equals("w")) {
                post.imageWidth = reader.nextInt();
            } else if (key.equals("h")) {
                post.imageHeight = reader.nextInt();
            } else if (key.equals("sub")) {
                post.subject = reader.nextString();
            } else if (key.equals("replies")) {
                post.replies = reader.nextInt();
            } else if (key.equals("filename")) {
                post.filename = reader.nextString();
            } else if (key.equals("sticky")) {
                post.sticky = reader.nextInt() == 1;
            } else if (key.equals("closed")) {
                post.closed = reader.nextInt() == 1;
            } else if (key.equals("trip")) {
                post.tripcode = reader.nextString();
            } else if (key.equals("country")) {
                post.country = reader.nextString();
            } else if (key.equals("country_name")) {
                post.countryName = reader.nextString();
            } else if (key.equals("id")) {
                post.id = reader.nextString();
            } else {
                // Unknown/ignored key
//                log("Unknown/ignored key: " + key + ".");
                reader.skipValue();
            }
        }
        reader.endObject();
        
        if (!post.finish()) {
            throw new IOException("Incorrect data about post received.");
        }
        
        return post;
    }
}





