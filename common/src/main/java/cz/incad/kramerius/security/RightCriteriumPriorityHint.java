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
 * Priority hint is some feature for ordering right criteriums.
 *   
 * If one criterium feels as the most important, it can say it through method <code>getPriorityHint</code>
 * {@link RightCriterium#getPriorityHint()}. This act will affect the rights criterium ordering in resolution.
 * 
 * @author pavels
 */
public enum RightCriteriumPriorityHint {
    // dedicated for dnnt
    DNNT_EXCLUSIVE_MAX,

    MAX,
    
    NORMAL,
    
    MIN,

    //dedicated for ddnt
    DNNT_EXCLUSIVE_MIN;
}
