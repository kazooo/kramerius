/*
 * Copyright (C) 2010 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.kramerius.security;

/**
 * The exception indicates problem with right criterium
 */
public class RightCriteriumException extends Exception {

    public RightCriteriumException() {
        super();
    }

    public RightCriteriumException(String message, Throwable cause) {
        super(message, cause);
    }

    public RightCriteriumException(String message) {
        super(message);
    }

    public RightCriteriumException(Throwable cause) {
        super(cause);
    }
    
}
