
package org.mule.module.s3.adapter;

import javax.annotation.Generated;

@Generated(value = "Mule DevKit Version 3.5.0-M4", date = "2014-03-07T02:19:11-06:00", comments = "Build M4.1875.17b58a3")
public interface PoolManager {

    /**
     * Retrieves the pool of objects
     */
    public org.mule.util.pool.LifecyleEnabledObjectPool getLifecyleEnabledObjectPool();
}