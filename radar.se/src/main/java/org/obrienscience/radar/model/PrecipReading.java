package org.obrienscience.radar.model;

/*******************************************************************************
 * Copyright (c) 2010, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     16/02/2011 2.3  Michael O'Brien 
 *          - 337037: initial API and implementation platform to be used for 
 *             distributed EE application research, development and architecture
 ******************************************************************************/  

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

//import org.eclipse.persistence.annotations.Convert;
//import org.eclipse.persistence.annotations.TypeConverter;

/**
 * This class is part of a distributed application framework used to simulate and research
 * concurrency, analytics, management, performance and exception handling.
 * The focus is on utilizing JPA 2.0 as the persistence layer for scenarios involving
 * multicore, multithreaded and multiuser distributed memory L1 persistence applications.
 * The secondary focus is on exercising Java EE6 API to access the results of this distributed application.
 * 
 * @see http://bugs.eclipse.org/337037
 * @see http://wiki.eclipse.org/EclipseLink/Examples/Distributed
 * @author Michael O'Brien
 * @since EclipseLink 2.3
 */
//@Entity
//@TypeConverter(name="BigIntegerToString",dataType=String.class,objectType=BigInteger.class)
public class PrecipReading extends Reading implements Serializable {
    private static final long serialVersionUID = 3287861579472326552L;
    
    
    // Cached values - do not persist
    @Transient
    private BigInteger interval;

    @Override
    public String toString() {
        StringBuffer aBuffer = new StringBuffer("PrecipReading");//getClass().getSimpleName());
        aBuffer.append("@");
        aBuffer.append(hashCode());
        aBuffer.append("( id: ");
        aBuffer.append(getId());
        aBuffer.append(")");
        return aBuffer.toString();
    }    

}
