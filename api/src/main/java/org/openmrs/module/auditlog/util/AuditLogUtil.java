/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.auditlog.util;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Contains static utility methods
 */
public class AuditLogUtil {
	
	private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	public static final String NODE_CHANGES = "changes";
	
	public static final String NODE_PROPERTY = "property";
	
	public static final String NODE_PREVIOUS = "previous";
	
	public static final String NODE_NEW = "new";
	
	public static final String ATTRIBUTE_NAME = "name";
	
	/**
	 * Utility method that generates the xml for edited properties including their previous and new
	 * property values of an edited object
	 * 
	 * @param propertyChangesMap mapping of edited properties to their previous and new values
	 * @return the generated xml text
	 */
	public static String generateChangesXml(Map<String, String[]> propertyChangesMap) {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n<" + NODE_CHANGES + ">");
		for (Map.Entry<String, String[]> entry : propertyChangesMap.entrySet()) {
			String newValue = entry.getValue()[0];
			String previousValue = entry.getValue()[1];
			//we shouldn't even be here since this is not a change
			if (previousValue == null && newValue == null)
				continue;
			
			sb.append("\n<" + NODE_PROPERTY + " " + ATTRIBUTE_NAME + "=\"" + entry.getKey() + "\">");
			//when deserializing, missing tags will be interpreted as NULL
			if (newValue != null) {
				sb.append("\n<" + NODE_NEW + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(newValue));
				sb.append("\n</" + NODE_NEW + ">");
			}
			if (previousValue != null) {
				sb.append("\n<" + NODE_PREVIOUS + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(previousValue));
				sb.append("\n</" + NODE_PREVIOUS + ">");
			}
			sb.append("\n</" + NODE_PROPERTY + ">");
		}
		
		sb.append("\n</" + NODE_CHANGES + ">");
		
		return sb.toString();
	}
	
	/**
	 * Gets the text content of a nested previous or new tag inside a property tag with a name
	 * attribute matching the specified property name
	 * 
	 * @param propertyEle {@link Element} object
	 * @param getNew specifies which value to value to return i.e previous vs new
	 * @return the text content of the nested tag
	 * @throws Exception
	 */
	public static String getPreviousOrNewPropertyValue(Element propertyEle, boolean getNew) throws Exception {
		if (propertyEle != null) {
			String tagName = (getNew) ? NODE_NEW : NODE_PREVIOUS;
			Element ele = getElement(propertyEle, tagName);
			if (ele != null) {
				if (ele.getTextContent() != null)
					return ele.getTextContent().trim();
			}
		}
		return null;
	}
	
	/**
	 * Utility method that converts an xml string to a {@link Document} object
	 * 
	 * @param xml the xml to convert
	 * @return {@link Document} object
	 * @throws Exception
	 */
	public static Document createDocument(String xml) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}
	
	/**
	 * @param propertyElement {@link Element} object
	 * @return
	 * @throws Exception
	 */
	private static Element getElement(Element propertyElement, String tagName) throws Exception {
		NodeList nodeList = propertyElement.getElementsByTagName(tagName);
		if (nodeList != null) {
			if (nodeList.getLength() == 1)
				return (Element) nodeList.item(0);
			else if (nodeList.getLength() > 1)
				log.warn("Invalid changes xml: Found multiple " + tagName + " tags");
		}
		return null;
	}
	
	/**
	 * Converts a set of class objects to a list of class name strings
	 * 
	 * @param clazzes
	 * @return
	 */
	public static List<String> getAsListOfClassnames(Set<Class<?>> clazzes) {
		List<String> classnames = new ArrayList<String>(clazzes.size());
		for (Class<?> clazz : clazzes) {
			classnames.add(clazz.getName());
		}
		return classnames;
	}
	
	/**
	 * Gets the class of the collection elements if the property with the specified name is a
	 * collection
	 * 
	 * @param owningType
	 * @param propertyName
	 * @return the class of the elements of the matching property
	 * @should return the class of the property
	 */
	public static Class<?> getCollectionElementType(Class<?> owningType, String propertyName) {
		Field field = getField(owningType, propertyName);
		if (field != null) {
			if (Collection.class.isAssignableFrom(field.getType())) {
				Type type = field.getGenericType();
				if (type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					if (!ArrayUtils.isEmpty(pt.getActualTypeArguments())) {
						return (Class<?>) pt.getActualTypeArguments()[0];
					}
				}
			}
		} else {
			log.warn("Failed to find property " + propertyName + " in class " + owningType.getName());
		}
		
		return null;
	}
	
	/**
	 * Convenience method that find a field with the specified name in the specified class The
	 * method is recursively called to check all superclasses too
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clazz, String fieldName) {
		Field field = null;
		try {
			field = clazz.getDeclaredField(fieldName);
		}
		catch (Exception e) {
			//check the super classes if any
			if (clazz.getSuperclass() != null)
				field = getField(clazz.getSuperclass(), fieldName);
		}
		
		return field;
	}
}
