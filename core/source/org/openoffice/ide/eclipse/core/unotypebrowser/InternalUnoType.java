/*************************************************************************
 *
 * $RCSfile: InternalUnoType.java,v $
 *
 * $Revision: 1.2 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2006/06/09 06:14:02 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.openoffice.ide.eclipse.core.unotypebrowser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class describing a UNO-Type. Only used with the {@link UnoTypeProvider}.
 * A Uno type is described by its name, a boolean field defining if it's a
 * local type and a path containing the fully qualified name of the type 
 * container.
 * 
 * @author cbosdonnat
 *
 */
public class InternalUnoType {

	private static final String LOCAL_TAG = "L";
	private static final String EXTERNAL_TAG= "E";
	
	private String path;
	private int type;
	private boolean local = false;
	
	public InternalUnoType(String typeString) {
		if (null != typeString) {
			Matcher typeMatcher = Pattern.compile(
					"(" + EXTERNAL_TAG + "|" + LOCAL_TAG +
					") ([^\\s]*) ([0-9]+)").matcher(typeString);
			if (typeMatcher.matches() && 3 == typeMatcher.groupCount()){
				setLocal(typeMatcher.group(1));
				setType(Integer.parseInt(typeMatcher.group(3)));
				path = typeMatcher.group(2);
			}
		}
	}
	
	public InternalUnoType(String completeName, int aType, boolean isLocal) {
		local = isLocal;
		setType(aType);
		path = completeName;
	}
	
	/**
	 * Returns the type name, ie <code>XInterface</code> for 
	 * <code>com.sun.star.uno.XInterface</code>
	 */
	public String getName() {
		String name = "";
		
		String[] splittedPath = path.split("\\.");
		if (splittedPath.length > 0) {
			name = splittedPath[splittedPath.length - 1];
		}
		return name;
	}
	
	/**
	 * Returns the type complete name, ie 
	 * <code>com.sun.star.uno.XInterface</code> for <code>
	 * com.sun.star.uno.XInterface</code>
	 */
	public String getFullName(){
		return path;
	}
	
	/**
	 * Returns the type of the type, ie {@link UnoTypeProvider#INTERFACE} for 
	 * <code>com.sun.star.uno.XInterface</code>
	 * 
	 * @return one of the types defined in {@link UnoTypeProvider}
	 */
	public int getType(){
		return type;
	}
	
	/**
	 * Returns whether the type is defined in an external project or not.
	 */
	public boolean isLocalType(){
		return local;
	}

	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		
		String sLocal = EXTERNAL_TAG;
		if (isLocalType()) {
			sLocal = LOCAL_TAG;
		}
		
		return sLocal + " " + getFullName() + " " + getType();
	}
	
	private void setType(int aType) {
		if (aType >= 0 && aType < 1024) {
			type = aType;
		}
	}
	
	private void setLocal(String tag){
		if (tag.equals(LOCAL_TAG)) {
			local = true;
		} else {
			local = false;
		}
	}
}