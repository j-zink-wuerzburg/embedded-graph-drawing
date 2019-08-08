package de.uniwue.informatik.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;

public class SerialUtils {
		
	public static String serializeObject(Object o) {
	    Gson gson = new Gson();
	    String serializedObject = gson.toJson(o, o.getClass());
	    return serializedObject;
	}
	
	public static Object unserializeObject(String s, Class<?> classOfObject){
	    Gson gson = new Gson();
	    Object object = gson.fromJson(s, classOfObject);
	    return object;
	}
	
	public static <V, E> EmbeddedUndirectedGraph<V, E> unserializeEmbeddedUndirectedGraph(String s, EmbeddedUndirectedGraph<V, E> graph){
	    Gson gson = new Gson();
	    EmbeddedUndirectedGraph<V, E> object = gson.fromJson(s, new TypeToken<EmbeddedUndirectedGraph<V, E>>() {}.getType());
	    return object;
	}

	public static Object cloneObject(Object o){
	    String s = serializeObject(o);
	    Object object = unserializeObject(s, o.getClass());
	    return object;
	}
	
	public static <V, E> EmbeddedUndirectedGraph<V, E> cloneEmbeddedUndirectedGraph(EmbeddedUndirectedGraph<V, E> o){
	    String s = serializeObject(o);
	    EmbeddedUndirectedGraph<V, E> object = unserializeEmbeddedUndirectedGraph(s,o);
	    return object;
	}
}