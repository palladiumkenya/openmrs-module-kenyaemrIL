package org.openmrs.module.kenyaemrIL.api.comparator;

import org.openmrs.Order;

import java.util.Comparator;
class CustomOrderComparator implements Comparator {
    // Used for sorting in ascending order of order id
    @Override
    public int compare(Object o1,Object o2){
            Order s1=(Order) o1;
            Order s2=(Order) o2;
        return Integer.compare(s1.getOrderId(), s2.getOrderId());
    }
}